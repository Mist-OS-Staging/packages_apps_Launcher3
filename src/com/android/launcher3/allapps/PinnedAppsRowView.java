/*
 * Copyright 2025-2026 AxionOS
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
package com.android.launcher3.allapps;

import static android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE;

import static com.android.launcher3.LauncherPrefs.PINNED_APPS;
import static com.android.launcher3.LauncherPrefs.PINNED_APPS_ORDER;
import static com.android.launcher3.LauncherPrefs.PINNED_APPS_ROWS;
import static com.android.launcher3.LauncherSettings.Favorites.CONTAINER_ALL_APPS;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener;
import com.android.launcher3.Flags;
import com.android.launcher3.LauncherPrefChangeListener;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.anim.AlphaUpdateListener;
import com.android.launcher3.keyboard.FocusIndicatorHelper;
import com.android.launcher3.keyboard.FocusIndicatorHelper.SimpleFocusIndicatorHelper;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class PinnedAppsRowView extends LinearLayout implements OnDeviceProfileChangeListener,
        FloatingHeaderRow, AllAppsStore.OnUpdateListener, LauncherPrefChangeListener {

    private static final int CARD_RADIUS_DP = 28;
    private static final int CARD_INSET_TOP_DP = 4;
    private static final int CARD_INSET_BOTTOM_DP = 8;
    private static final int CARD_PADDING_HORIZONTAL_DP = 8;
    private static final int CARD_PADDING_TOP_DP = 14;
    private static final int CARD_PADDING_BOTTOM_DP = 4;
    private static final int TITLE_TEXT_SIZE_SP = 14;
    private static final int TITLE_ICON_SIZE_DP = 16;
    private static final int TITLE_GAP_DP = 6;

    private final ActivityContext mActivityContext;
    private final AllAppsStore mAllAppsStore;
    private final FocusIndicatorHelper mFocusHelper;
    private final List<WorkspaceItemInfo> mPinnedApps = new ArrayList<>();
    private final List<LinearLayout> mIconRows = new ArrayList<>();
    private final TextView mTitleView;
    private final int mTitleHeight;
    private FloatingHeaderView mParent;
    @Nullable
    private HorizontalScrollView mScrollView;
    private int mNumPinnedAppsPerRow;
    private int mShownCount;
    private boolean mPinnedAppsVisible;
    private boolean mScrollMode;
    private int mMaxRows;

    public PinnedAppsRowView(@NonNull Context context) {
        this(context, null);
    }

    public PinnedAppsRowView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);

        mActivityContext = ActivityContext.lookupContext(context);
        mAllAppsStore = mActivityContext.getActivityComponent().getAppsStore();
        mFocusHelper = new SimpleFocusIndicatorHelper(this);
        mNumPinnedAppsPerRow =
                mActivityContext.getDeviceProfile().getAllAppsProfile().getNumShownAllAppsColumns();

        int insetTop = dp(CARD_INSET_TOP_DP);
        int insetBottom = dp(CARD_INSET_BOTTOM_DP);
        GradientDrawable card = new GradientDrawable();
        card.setColor(Themes.getAttrColor(context, R.attr.allappsHeaderProtectionColor));
        card.setCornerRadius(dp(CARD_RADIUS_DP));
        setBackground(new InsetDrawable(card, 0, insetTop, 0, insetBottom));
        setClipToOutline(true);
        int paddingHorizontal = dp(CARD_PADDING_HORIZONTAL_DP);
        setPadding(paddingHorizontal, insetTop + dp(CARD_PADDING_TOP_DP), paddingHorizontal,
                insetBottom + dp(CARD_PADDING_BOTTOM_DP));

        int textColor = Themes.getAttrColor(context, android.R.attr.textColorPrimary);
        float titleTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                TITLE_TEXT_SIZE_SP, getResources().getDisplayMetrics());
        int titleIconSize = dp(TITLE_ICON_SIZE_DP);
        int titleGap = dp(TITLE_GAP_DP);
        mTitleHeight = Math.max(Utilities.calculateTextHeight(titleTextSize), titleIconSize)
                + titleGap;
        mTitleView = new TextView(context);
        mTitleView.setText(R.string.title_pinned_apps);
        mTitleView.setTextColor(textColor);
        mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TITLE_TEXT_SIZE_SP);
        mTitleView.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        mTitleView.setSingleLine(true);
        mTitleView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        mTitleView.setPaddingRelative(titleGap * 2, 0, titleGap * 2, titleGap);
        mTitleView.setAccessibilityHeading(true);
        Drawable pinIcon = context.getDrawable(R.drawable.ic_pin);
        if (pinIcon != null) {
            pinIcon = pinIcon.mutate();
            pinIcon.setBounds(0, 0, titleIconSize, titleIconSize);
            mTitleView.setCompoundDrawablesRelative(pinIcon, null, null, null);
            mTitleView.setCompoundDrawablePadding(titleGap);
            mTitleView.setCompoundDrawableTintList(ColorStateList.valueOf(textColor));
        }
        addView(mTitleView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mTitleHeight));

        readLayoutPrefs();
        updateVisibility();
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        if (Build.VERSION.SDK_INT >= UPSIDE_DOWN_CAKE) {
            info.setContainerTitle(getContext().getString(R.string.title_pinned_apps));
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mActivityContext.addOnDeviceProfileChangeListener(this);
        mAllAppsStore.addUpdateListener(this);
        LauncherPrefs.get(getContext()).addListener(this, PINNED_APPS, PINNED_APPS_ORDER,
                PINNED_APPS_ROWS);
        updatePinnedApps();
    }

    @Override
    protected void onDetachedFromWindow() {
        LauncherPrefs.get(getContext()).removeListener(this, PINNED_APPS, PINNED_APPS_ORDER,
                PINNED_APPS_ROWS);
        mAllAppsStore.removeUpdateListener(this);
        mActivityContext.removeOnDeviceProfileChangeListener(this);
        for (LinearLayout iconRow : mIconRows) {
            mAllAppsStore.unregisterIconContainer(iconRow);
        }
        super.onDetachedFromWindow();
    }

    @Override
    public void setup(FloatingHeaderView parent, FloatingHeaderRow[] rows, boolean tabsHidden) {
        mParent = parent;
    }

    @Override
    public int getExpectedHeight() {
        DeviceProfile deviceProfile = mActivityContext.getDeviceProfile();
        int perRowHeight = deviceProfile.getAllAppsProfile().getCellHeightPx();
        int totalHeight = Math.max(1, mIconRows.size()) * perRowHeight + mTitleHeight;
        return getVisibility() == GONE ? 0 : totalHeight + getPaddingTop() + getPaddingBottom();
    }

    @Override
    public boolean shouldDraw() {
        return getVisibility() != GONE;
    }

    @Override
    public boolean hasVisibleContent() {
        return mPinnedAppsVisible;
    }

    @Override
    public void setVerticalScroll(int scroll, boolean isScrolledOut) {
        if (!isScrolledOut) {
            setTranslationY(scroll);
        }
        setAlpha(isScrolledOut ? 0 : 1);
        if (getVisibility() != GONE) {
            AlphaUpdateListener.updateVisibility(this);
        }
    }

    @Override
    public Class<PinnedAppsRowView> getTypeClass() {
        return PinnedAppsRowView.class;
    }

    @Override
    public View getFocusedChild() {
        return mIconRows.isEmpty() ? null : mIconRows.get(0).getChildAt(0);
    }

    @Override
    public boolean isVisible() {
        return getVisibility() == VISIBLE;
    }

    @Override
    public void onDeviceProfileChanged(DeviceProfile dp) {
        mNumPinnedAppsPerRow = dp.getAllAppsProfile().getNumShownAllAppsColumns();
        clearIconRows();
        applyPinnedApps();
    }

    @Override
    public void onAppsUpdated() {
        updatePinnedApps();
    }

    @Override
    public void onPrefChanged(String key) {
        if (PINNED_APPS_ROWS.getSharedPrefKey().equals(key)) {
            readLayoutPrefs();
            clearIconRows();
            updatePinnedApps();
        } else if (PINNED_APPS.getSharedPrefKey().equals(key)
                || PINNED_APPS_ORDER.getSharedPrefKey().equals(key)) {
            updatePinnedApps();
        }
    }

    public int getPinnedCount() {
        return mPinnedApps.size();
    }

    public int indexOfPinned(ItemInfo itemInfo) {
        String key = PinnedAppsStore.encode(getContext(), itemInfo);
        if (key == null) {
            return -1;
        }
        for (int i = 0; i < mPinnedApps.size(); i++) {
            if (key.equals(PinnedAppsStore.encode(getContext(), mPinnedApps.get(i)))) {
                return i;
            }
        }
        return -1;
    }

    public void movePinnedApp(ItemInfo itemInfo, int delta) {
        int from = indexOfPinned(itemInfo);
        int to = from + delta;
        if (from < 0 || to < 0 || to >= mPinnedApps.size()) {
            return;
        }
        List<String> keys = new ArrayList<>(mPinnedApps.size());
        for (WorkspaceItemInfo info : mPinnedApps) {
            keys.add(PinnedAppsStore.encode(getContext(), info));
        }
        keys.add(to, keys.remove(from));
        PinnedAppsStore.setOrder(getContext(), keys);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mScrollMode && !mIconRows.isEmpty() && mNumPinnedAppsPerRow > 0) {
            int available = MeasureSpec.getSize(widthMeasureSpec)
                    - getPaddingLeft() - getPaddingRight();
            int iconWidth = Math.max(0, available / mNumPinnedAppsPerRow);
            LinearLayout iconRow = mIconRows.get(0);
            for (int i = 0; i < iconRow.getChildCount(); i++) {
                iconRow.getChildAt(i).getLayoutParams().width = iconWidth;
            }
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(getExpectedHeight(),
                MeasureSpec.EXACTLY));
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        mFocusHelper.draw(canvas);
        super.dispatchDraw(canvas);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void dump(String prefix, PrintWriter writer) {
        writer.println(prefix + "PinnedAppsRowView");
        writer.println(prefix + "\tmPinnedAppsVisible: " + mPinnedAppsVisible);
        writer.println(prefix + "\tmNumPinnedAppsPerRow: " + mNumPinnedAppsPerRow);
        writer.println(prefix + "\tmScrollMode: " + mScrollMode);
        writer.println(prefix + "\tmMaxRows: " + mMaxRows);
        writer.println(prefix + "\tmShownCount: " + mShownCount);
        writer.println(prefix + "\tmPinnedApps: " + mPinnedApps.size());
        for (WorkspaceItemInfo info : mPinnedApps) {
            writer.println(prefix + "\t\t" + info);
        }
    }

    private void readLayoutPrefs() {
        mScrollMode = PinnedAppsStore.isScrollMode(getContext());
        mMaxRows = PinnedAppsStore.getMaxRows(getContext());
    }

    private void updatePinnedApps() {
        mPinnedApps.clear();
        mPinnedApps.addAll(PinnedAppsStore.getPinnedWorkspaceItems(getContext(), mAllAppsStore));
        applyPinnedApps();
    }

    private void applyPinnedApps() {
        mShownCount = PinnedAppsStore.getShownCount(mPinnedApps.size(), mNumPinnedAppsPerRow,
                mScrollMode, mMaxRows);
        updatePinnedIconSlots();
        int iconIndex = 0;

        for (int row = 0; row < mIconRows.size(); row++) {
            LinearLayout iconRow = mIconRows.get(row);
            for (int col = 0; col < iconRow.getChildCount(); col++) {
                BubbleTextView icon = (BubbleTextView) iconRow.getChildAt(col);
                icon.reset();
                if (mShownCount > iconIndex) {
                    icon.setVisibility(View.VISIBLE);
                    WorkspaceItemInfo pinnedItem = mPinnedApps.get(iconIndex);
                    pinnedItem.container = CONTAINER_ALL_APPS;
                    pinnedItem.rank = iconIndex;
                    pinnedItem.cellX = col;
                    pinnedItem.cellY = row;
                    icon.applyFromWorkspaceItem(pinnedItem);
                    icon.verifyHighRes();
                } else {
                    icon.setVisibility(mShownCount == 0 ? GONE : INVISIBLE);
                }
                iconIndex++;
            }
        }

        mPinnedAppsVisible = mShownCount > 0;
        updateVisibility();
        if (mParent != null) {
            mParent.onHeightUpdated();
        }
    }

    private void updatePinnedIconSlots() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        if (mScrollMode) {
            if (mScrollView == null) {
                clearIconRows();
                mScrollView = createScrollRow();
                addView(mScrollView);
                mIconRows.add((LinearLayout) mScrollView.getChildAt(0));
            }
            LinearLayout iconRow = mIconRows.get(0);
            int neededIcons = Math.max(mShownCount, mNumPinnedAppsPerRow);
            while (iconRow.getChildCount() > neededIcons) {
                iconRow.removeViewAt(iconRow.getChildCount() - 1);
            }
            while (iconRow.getChildCount() < neededIcons) {
                iconRow.addView(createIcon(inflater, iconRow, false));
            }
            return;
        }

        int neededRows = Math.max(1,
                (int) Math.ceil(mShownCount / (double) mNumPinnedAppsPerRow));
        while (mIconRows.size() > neededRows) {
            LinearLayout iconRow = mIconRows.remove(mIconRows.size() - 1);
            mAllAppsStore.unregisterIconContainer(iconRow);
            removeView(iconRow);
        }
        while (mIconRows.size() < neededRows) {
            LinearLayout iconRow = createIconRow(inflater);
            mIconRows.add(iconRow);
            addView(iconRow);
        }
    }

    private void clearIconRows() {
        for (LinearLayout iconRow : mIconRows) {
            mAllAppsStore.unregisterIconContainer(iconRow);
        }
        if (mScrollView != null) {
            removeView(mScrollView);
        } else {
            for (LinearLayout iconRow : mIconRows) {
                removeView(iconRow);
            }
        }
        mIconRows.clear();
        mScrollView = null;
    }

    private HorizontalScrollView createScrollRow() {
        int cellHeight = mActivityContext.getDeviceProfile().getAllAppsProfile().getCellHeightPx();
        HorizontalScrollView scrollView = new HorizontalScrollView(getContext());
        scrollView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                cellHeight));
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setHorizontalFadingEdgeEnabled(true);
        scrollView.setFadingEdgeLength(getResources().getDimensionPixelSize(
                R.dimen.all_apps_predicted_icon_vertical_padding) * 3);
        scrollView.setClipToPadding(false);

        LinearLayout iconRow = new LinearLayout(getContext());
        iconRow.setOrientation(LinearLayout.HORIZONTAL);
        iconRow.setLayoutParams(new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.addView(iconRow);
        return scrollView;
    }

    private LinearLayout createIconRow(LayoutInflater inflater) {
        LinearLayout iconRow = new LinearLayout(getContext());
        iconRow.setOrientation(LinearLayout.HORIZONTAL);
        iconRow.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                mActivityContext.getDeviceProfile().getAllAppsProfile().getCellHeightPx()));
        for (int i = 0; i < mNumPinnedAppsPerRow; i++) {
            iconRow.addView(createIcon(inflater, iconRow, true));
        }
        return iconRow;
    }

    private BubbleTextView createIcon(LayoutInflater inflater, LinearLayout iconRow,
            boolean fillRow) {
        BubbleTextView icon = (BubbleTextView) inflater.inflate(
                R.layout.all_apps_prediction_row_icon, iconRow, false);
        icon.setOnClickListener(mActivityContext.getItemOnClickListener());
        icon.setOnLongClickListener(mActivityContext.getAllAppsItemLongClickListener());
        icon.setLongPressTimeoutFactor(1f);
        icon.setOnFocusChangeListener(mFocusHelper);

        LayoutParams lp = (LayoutParams) icon.getLayoutParams();
        if (Flags.enableFocusOutline()) {
            lp.height = ViewGroup.LayoutParams.MATCH_PARENT;
        } else {
            lp.height = mActivityContext.getDeviceProfile().getAllAppsProfile()
                    .getCellHeightPx();
        }
        if (fillRow) {
            lp.width = 0;
            lp.weight = 1;
        } else {
            lp.width = getWidth() > 0 && mNumPinnedAppsPerRow > 0
                    ? (getWidth() - getPaddingLeft() - getPaddingRight()) / mNumPinnedAppsPerRow
                    : ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.weight = 0;
        }
        return icon;
    }

    private void updateVisibility() {
        setVisibility(mPinnedAppsVisible ? VISIBLE : GONE);
        for (LinearLayout iconRow : mIconRows) {
            if (mPinnedAppsVisible) {
                mAllAppsStore.registerIconContainer(iconRow);
            } else {
                mAllAppsStore.unregisterIconContainer(iconRow);
            }
        }
    }
}
