/*
 * Copyright (C) 2020 Shade Launcher
 * Copyright (C) 2024-2026 Lunaris AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.customization;

import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;
import static com.android.launcher3.util.Executors.THREAD_POOL_EXECUTOR;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.util.PackageManagerHelper;
import com.android.launcher3.util.SystemUiController;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.AbstractSlideInView;

/**
 * Bottom sheet shown for the "App info" shortcut on the home screen and in the app drawer, with
 * the version, last update and install source of the app and a shortcut to the system app info.
 */
public class InfoBottomSheet extends AbstractSlideInView<Launcher> implements Insettable {

    private static final int DEFAULT_CLOSE_DURATION = 200;

    private final Rect mInsets = new Rect();
    private int mContentBottomPadding;

    @Nullable private ItemInfo mItemInfo;
    @Nullable private Rect mSourceBounds;
    @Nullable private Context mViewContext;

    private TextView mVersionSummary;
    private TextView mLastUpdateSummary;
    private TextView mSourceSummary;
    private View mSourceRow;
    private TextView mIconPackSummary;

    public InfoBottomSheet(Context context) {
        this(context, null);
    }

    public InfoBottomSheet(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public InfoBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = findViewById(R.id.app_info_bottom_sheet);
        mContentBottomPadding = mContent.getPaddingBottom();
        setContentBackgroundWithParent(
                getContext().getDrawable(R.drawable.bg_rounded_corner_bottom_sheet), mContent);

        ViewGroup rows = findViewById(R.id.app_info_rows);
        mVersionSummary = addRow(rows, R.drawable.ic_app_info_version,
                R.string.app_info_version).findViewById(android.R.id.summary);
        mLastUpdateSummary = addRow(rows, R.drawable.ic_app_info_last_update,
                R.string.app_info_last_update).findViewById(android.R.id.summary);
        mSourceRow = addRow(rows, R.drawable.ic_app_info_source, R.string.app_info_source);
        mSourceSummary = mSourceRow.findViewById(android.R.id.summary);
        View iconPackRow = addRow(rows, R.drawable.ic_app_info_icon_pack, R.string.icon_pack_title);
        mIconPackSummary = iconPackRow.findViewById(android.R.id.summary);
        iconPackRow.setOnClickListener(v -> pickIconPack());
        View moreRow = addRow(rows, R.drawable.ic_info_no_shadow, R.string.app_info_more);
        moreRow.findViewById(android.R.id.summary).setVisibility(GONE);
        moreRow.setOnClickListener(v -> openSystemAppInfo());
    }

    private View addRow(ViewGroup parent, @DrawableRes int icon, @StringRes int title) {
        View row = LayoutInflater.from(getContext())
                .inflate(R.layout.app_info_bottom_sheet_row, parent, false);
        ((ImageView) row.findViewById(android.R.id.icon)).setImageResource(icon);
        ((TextView) row.findViewById(android.R.id.title)).setText(title);
        row.setClickable(false);
        parent.addView(row);
        return row;
    }

    /**
     * Shows the sheet for {@code itemInfo}.
     *
     * @param sourceBounds bounds of the view the sheet was opened from, used to animate the system
     *                     app info screen out of it.
     */
    public void populateAndShow(ItemInfo itemInfo, Rect sourceBounds, Context viewContext) {
        mItemInfo = itemInfo;
        mSourceBounds = sourceBounds;
        mViewContext = viewContext;

        ((TextView) findViewById(R.id.title)).setText(TextUtils.isEmpty(itemInfo.title)
                ? getContext().getString(R.string.app_info_title) : itemInfo.title);
        bindMetadata(itemInfo);

        attachToContainer();
        mIsOpen = true;
        setupNavBarColor();
        setUpDefaultOpenAnimation().start();
    }

    private void bindMetadata(ItemInfo itemInfo) {
        ComponentName component = itemInfo.getTargetComponent();
        if (component == null) return;
        Context context = getContext().getApplicationContext();
        THREAD_POOL_EXECUTOR.execute(() -> {
            MetadataExtractor extractor =
                    new MetadataExtractor(context, component.getPackageName());
            CharSequence version = component.getPackageName() + " " + context.getString(
                    R.string.app_info_version_value,
                    extractor.getVersionName(), extractor.getVersionCode());
            CharSequence lastUpdate = extractor.getLastUpdate();
            CharSequence source = extractor.getSource();
            Intent marketIntent = extractor.getMarketIntent();
            CharSequence iconPack = IconPackChoices.currentLabel(context, componentKey(itemInfo));
            MAIN_EXECUTOR.execute(() -> {
                if (mItemInfo != itemInfo) return;
                mIconPackSummary.setText(iconPack);
                mVersionSummary.setText(version);
                mLastUpdateSummary.setText(lastUpdate);
                mSourceSummary.setText(source);
                if (marketIntent != null) {
                    mSourceRow.setOnClickListener(v -> openStore(marketIntent));
                }
            });
        });
    }

    private static ComponentKey componentKey(ItemInfo itemInfo) {
        return new ComponentKey(itemInfo.getTargetComponent(), itemInfo.user);
    }

    private void pickIconPack() {
        ItemInfo itemInfo = mItemInfo;
        if (itemInfo == null || itemInfo.getTargetComponent() == null) return;
        ComponentKey key = componentKey(itemInfo);
        Context context = getContext().getApplicationContext();
        // Finding the packs with an icon for this app parses them, keep it off the main thread.
        THREAD_POOL_EXECUTOR.execute(() -> {
            IconPackChoices choices = IconPackChoices.load(context, key);
            MAIN_EXECUTOR.execute(() -> {
                if (!isOpen() || mItemInfo != itemInfo) return;
                new AlertDialog.Builder(mActivityContext)
                        .setTitle(R.string.icon_pack_title)
                        .setSingleChoiceItems(choices.labels, choices.checkedIndex,
                                (dialog, which) -> {
                                    dialog.dismiss();
                                    mIconPackSummary.setText(choices.labels[which]);
                                    THREAD_POOL_EXECUTOR.execute(
                                            () -> choices.apply(context, key, which));
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            });
        });
    }

    private void openStore(Intent marketIntent) {
        try {
            mActivityContext.startActivity(marketIntent);
            close(true);
        } catch (ActivityNotFoundException | SecurityException ignored) {
            // The installer is gone or has no store page, nothing to open.
        }
    }

    private void openSystemAppInfo() {
        if (mItemInfo == null) return;
        PackageManagerHelper.startDetailsActivityForInfo(
                mViewContext != null ? mViewContext : getContext(), mItemInfo, mSourceBounds,
                ActivityOptions.makeBasic().toBundle());
        close(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildWithMargins(mContent, widthMeasureSpec, mInsets.left + mInsets.right,
                heightMeasureSpec,
                mActivityContext.getDeviceProfile().getBottomSheetProfile()
                        .getBottomSheetTopPadding());
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.getSize(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int height = b - t;
        // Content is laid out as center bottom aligned.
        int contentWidth = mContent.getMeasuredWidth();
        int contentLeft = (width - contentWidth - mInsets.left - mInsets.right) / 2 + mInsets.left;
        mContent.layout(contentLeft, height - mContent.getMeasuredHeight(),
                contentLeft + contentWidth, height);
        setTranslationShift(mTranslationShift);
    }

    @Override
    public void setInsets(Rect insets) {
        mInsets.set(insets);
        mContent.setPadding(mContent.getPaddingLeft(), mContent.getPaddingTop(),
                mContent.getPaddingRight(), mContentBottomPadding + insets.bottom);
        if (mIsOpen) setupNavBarColor();
    }

    @Override
    protected int getScrimColor(Context context) {
        return context.getResources().getColor(R.color.widgets_picker_scrim, context.getTheme());
    }

    @Override
    protected void handleClose(boolean animate) {
        handleClose(animate, DEFAULT_CLOSE_DURATION);
    }

    @Override
    protected void onCloseComplete() {
        super.onCloseComplete();
        clearNavBarColor();
    }

    @Override
    protected boolean isOfType(@FloatingViewType int type) {
        return (type & TYPE_WIDGETS_BOTTOM_SHEET) != 0;
    }

    private void setupNavBarColor() {
        boolean isNavBarDark = Themes.getAttrBoolean(getContext(), R.attr.isMainColorDark);
        boolean isPhoneLandscape = mActivityContext.getDeviceProfile().getDeviceProperties()
                .isPhone() && mInsets.bottom == 0;
        if (!isNavBarDark && isPhoneLandscape) {
            isNavBarDark = true;
        }
        mActivityContext.getSystemUiController().updateUiState(
                SystemUiController.UI_STATE_WIDGET_BOTTOM_SHEET,
                isNavBarDark ? SystemUiController.FLAG_DARK_NAV
                        : SystemUiController.FLAG_LIGHT_NAV);
    }

    private void clearNavBarColor() {
        mActivityContext.getSystemUiController().updateUiState(
                SystemUiController.UI_STATE_WIDGET_BOTTOM_SHEET, 0);
    }
}
