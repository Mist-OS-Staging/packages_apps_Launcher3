/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.launcher3.allapps.search;

import static android.view.View.MeasureSpec.EXACTLY;
import static android.view.View.MeasureSpec.getSize;
import static android.view.View.MeasureSpec.makeMeasureSpec;

import static com.android.launcher3.icons.IconNormalizer.ICON_VISIBLE_AREA_FACTOR;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.AttributeSet;
import android.view.CrossWindowBlurListeners;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.app.animation.Interpolators;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.ExtendedEditText;
import com.android.launcher3.Insettable;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.ActivityAllAppsContainerView;
import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.allapps.BaseAllAppsAdapter.AdapterItem;
import com.android.launcher3.allapps.PrivateProfileManager;
import com.android.launcher3.allapps.SearchUiManager;
import com.android.launcher3.anim.KeyboardInsetAnimationCallback;
import com.android.launcher3.search.SearchCallback;
import com.android.launcher3.util.ApiWrapper;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Layout to contain the All-apps search UI.
 */
public class AppsSearchContainerLayout extends ExtendedEditText
        implements SearchUiManager, SearchCallback<AdapterItem>,
        AllAppsStore.OnUpdateListener, Insettable {

    private final ActivityContext mLauncher;
    private final AllAppsSearchBarController mSearchBarController;
    private final SpannableStringBuilder mSearchQueryBuilder;

    private ActivityAllAppsContainerView<?> mAppsView;

    // The amount of pixels to shift down and overlap with the rest of the content.
    private final int mContentOverlap;

    private static final int SEARCH_BAR_BG_ALPHA = 255;
    public static final float NARROW_IDLE_WIDTH_EXPANSION = 0.84f;
    public static final float ACTIVE_WIDTH_EXPANSION = 1.0f;
    private static final float NARROW_SIDE_INSET_DP = 10f;
    private final int mMaxBarWidth;
    private float mWidthExpansion = ACTIVE_WIDTH_EXPANSION;
    private ValueAnimator mWidthAnimator;
    private boolean mImeVisible;

    private AllAppsSearchBarConfig.Placement mPlacement = AllAppsSearchBarConfig.Placement.TOP;
    private AllAppsSearchBarConfig.Appearance mAppearance = AllAppsSearchBarConfig.Appearance.DEFAULT;

    private Drawable mSearchIcon;
    private boolean mBlurEnabled;
    private final RenderNode mBlurNode = new RenderNode("searchBarBackdropBlur");
    private final Path mClipPath = new Path();
    private float mBlurRadiusPx = -1f;
    private final int[] mBarLoc = new int[2];
    private final int[] mListLoc = new int[2];
    private RecyclerView mObservedList;
    private final RecyclerView.OnScrollListener mScrollListener =
            new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                    invalidate();
                }
            };
    private final Consumer<Boolean> mBlurListener = this::onBlurEnabledChanged;

    public AppsSearchContainerLayout(Context context) {
        this(context, null);
    }

    public AppsSearchContainerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AppsSearchContainerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mLauncher = ActivityContext.lookupContext(context);
        mSearchBarController = new AllAppsSearchBarController();

        mSearchQueryBuilder = new SpannableStringBuilder();
        Selection.setSelection(mSearchQueryBuilder, 0);

        mContentOverlap =
                getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_content_overlap);

        setWindowInsetsAnimationCallback(new KeyboardInsetAnimationCallback(this));

        mMaxBarWidth = getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_max_width);
        setElevation(0f);
        setTranslationZ(0f);

        mPlacement = AllAppsSearchBarConfig.getPlacement(context);
        mAppearance = AllAppsSearchBarConfig.getAppearance(context);
        mWidthExpansion = (mAppearance != null && mAppearance.isNarrow())
                ? NARROW_IDLE_WIDTH_EXPANSION : ACTIVE_WIDTH_EXPANSION;
        updateBackgroundAlpha();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getParent() instanceof ViewGroup) {
            ((ViewGroup) getParent()).setClipChildren(false);
        }
        if (mAppsView != null) {
            mAppsView.getAppsStore().addUpdateListener(this);
        }
        CrossWindowBlurListeners blurListeners = CrossWindowBlurListeners.getInstance();
        blurListeners.addListener(getContext().getMainExecutor(), mBlurListener);
        onBlurEnabledChanged(blurListeners.isCrossWindowBlurEnabled());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mAppsView != null) {
            mAppsView.getAppsStore().removeUpdateListener(this);
        }
        CrossWindowBlurListeners.getInstance().removeListener(mBlurListener);
        onBlurEnabledChanged(false);
        if (mWidthAnimator != null) {
            mWidthAnimator.cancel();
            mWidthAnimator = null;
        }
        if (mObservedList != null) {
            mObservedList.removeOnScrollListener(mScrollListener);
            mObservedList = null;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        DeviceProfile dp = mLauncher.getDeviceProfile();
        int myRequestedWidth = getSize(widthMeasureSpec);
        RecyclerView activeRv = mAppsView != null ? mAppsView.getActiveRecyclerView() : null;
        int rvPaddingLeft = activeRv != null ? activeRv.getPaddingLeft() : 0;
        int rvPaddingRight = activeRv != null ? activeRv.getPaddingRight() : 0;
        int rowWidth = myRequestedWidth - rvPaddingLeft - rvPaddingRight;

        int cellWidth = DeviceProfile.calculateCellWidth(rowWidth,
                dp.getWorkspaceProfile().getCellLayoutBorderSpacePx().x,
                dp.getHotseatProfile().getNumShownIcons());
        int iconVisibleSize =
                Math.round(ICON_VISIBLE_AREA_FACTOR * dp.getWorkspaceProfile().getIconSizePx());
        int iconPadding = cellWidth - iconVisibleSize;

        int contentWidth = rowWidth - iconPadding;
        int maxAllowedWidth = myRequestedWidth
                - 2 * (int) (16 * getResources().getDisplayMetrics().density);

        int fullWidth = contentWidth > 0 ? Math.min(contentWidth, maxAllowedWidth) : maxAllowedWidth;
        fullWidth = Math.min(fullWidth, mMaxBarWidth);

        float expansion = (mAppearance != null && mAppearance.isNarrow())
                ? mWidthExpansion : ACTIVE_WIDTH_EXPANSION;
        int myWidth = (int) (fullWidth * expansion);
        if (mAppearance != null && mAppearance.isNarrow()) {
            // Trim NARROW_SIDE_INSET_DP off each side while idle, easing back to the full active
            // width as the bar expands for typing.
            float idle = Math.max(0f, Math.min(1f, (ACTIVE_WIDTH_EXPANSION - expansion)
                    / (ACTIVE_WIDTH_EXPANSION - NARROW_IDLE_WIDTH_EXPANSION)));
            myWidth -= Math.round(2 * NARROW_SIDE_INSET_DP
                    * getResources().getDisplayMetrics().density * idle);
        }

        super.onMeasure(makeMeasureSpec(myWidth, EXACTLY), heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // Shift the widget horizontally so that it's centered in the parent (b/63428078)
        View parent = (View) getParent();
        if (parent != null) {
            int availableWidth = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight();
            int myWidth = right - left;
            int expectedLeft = parent.getPaddingLeft() + (availableWidth - myWidth) / 2;
            int shift = expectedLeft - left;
            setTranslationX(shift);
        }
    }

    private void onBlurEnabledChanged(boolean enabled) {
        if (mBlurEnabled == enabled) return;
        mBlurEnabled = enabled;
        updateBackgroundAlpha();
        invalidate();
    }

    private void updateBackgroundAlpha() {
        Drawable bg = getBackground();
        if (bg != null) {
            boolean isBlurActive = mAppearance != null && mAppearance.isBlur() && mBlurEnabled;
            bg.mutate().setAlpha(isBlurActive ? 0 : SEARCH_BAR_BG_ALPHA);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mClipPath.reset();
        mClipPath.addRoundRect(0f, 0f, w, h, h / 2f, h / 2f, Path.Direction.CW);
    }

    @Override
    public void draw(Canvas canvas) {
        if (mAppearance != null && mAppearance.isBlur() && mBlurEnabled) {
            drawBackdropBlur(canvas);
        }
        super.draw(canvas);
    }

    /** Blurs the app list content that scrolls under the search bar. */
    private void drawBackdropBlur(Canvas canvas) {
        if (!mBlurEnabled || mAppsView == null || !canvas.isHardwareAccelerated()
                || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        View list = mAppsView.getActiveRecyclerView();
        if (list == null || list.getWidth() <= 0) {
            return;
        }
        if (list instanceof RecyclerView && list != mObservedList) {
            if (mObservedList != null) {
                mObservedList.removeOnScrollListener(mScrollListener);
            }
            mObservedList = (RecyclerView) list;
            mObservedList.addOnScrollListener(mScrollListener);
        }
        getLocationInWindow(mBarLoc);
        list.getLocationInWindow(mListLoc);
        final int w = getWidth();
        final int h = getHeight();
        boolean effectNeeded = mBlurRadiusPx < 0f;
        if (effectNeeded) {
            mBlurRadiusPx = getResources()
                    .getDimensionPixelSize(R.dimen.all_apps_search_bar_blur_radius);
        }

        mBlurNode.setPosition(0, 0, w, h);
        RecordingCanvas rc = mBlurNode.beginRecording(w, h);
        rc.translate(mListLoc[0] - mBarLoc[0], mListLoc[1] - mBarLoc[1]);
        list.draw(rc);
        mBlurNode.endRecording();
        if (effectNeeded) {
            mBlurNode.setRenderEffect(RenderEffect.createBlurEffect(
                    mBlurRadiusPx, mBlurRadiusPx, Shader.TileMode.CLAMP));
        }

        int base = Themes.getAttrColor(
                getContext(), R.attr.allappsHeaderProtectionColor);
        int save = canvas.save();
        canvas.clipPath(mClipPath);
        // Opaque panel-colored backing hides the sharp icons that sit under the bar
        canvas.drawColor(base | 0xFF000000);
        canvas.drawRenderNode(mBlurNode);
        canvas.restoreToCount(save);
    }

    private int getSearchIconSize() {
        if (mSearchIcon == null) {
            mSearchIcon = getContext().getDrawable(R.drawable.ic_allapps_search);
        }
        return (mSearchIcon != null && mSearchIcon.getIntrinsicWidth() > 0)
                ? mSearchIcon.getIntrinsicWidth()
                : (int) (24 * getResources().getDisplayMetrics().density);
    }

    /** Search icon drawn either as part of the centered group or at the start margin. */
    private void drawSearchIcon(Canvas canvas, boolean isIdle) {
        if (mSearchIcon == null) {
            mSearchIcon = getContext().getDrawable(R.drawable.ic_allapps_search);
            if (mSearchIcon == null) return;
        }
        int size = getSearchIconSize();
        int top = (getHeight() - size) / 2;
        boolean isRtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
        CharSequence hint = getHint();

        int left;
        if (isIdle && hint != null && hint.length() > 0) {
            int iconSpacing = (int) (8 * getResources().getDisplayMetrics().density);
            float hintWidth = getPaint().measureText(hint, 0, hint.length());
            float groupWidth = size + iconSpacing + hintWidth;
            float groupStart = Math.max(0f, (getWidth() - groupWidth) / 2f);

            if (isRtl) {
                left = (int) (groupStart + groupWidth - size) + getScrollX();
            } else {
                left = (int) groupStart + getScrollX();
            }
        } else {
            int margin = (int) (16 * getResources().getDisplayMetrics().density);
            left = (isRtl ? getWidth() - margin - size : margin) + getScrollX();
        }

        mSearchIcon.setBounds(left, top, left + size, top + size);
        mSearchIcon.draw(canvas);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int save = canvas.save();
        boolean isIdle = length() == 0 && !isFocused();
        CharSequence hint = getHint();
        boolean isRtl = getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;

        if (isIdle && hint != null && hint.length() > 0) {
            int iconSize = getSearchIconSize();
            int iconSpacing = (int) (8 * getResources().getDisplayMetrics().density);
            float hintWidth = getPaint().measureText(hint, 0, hint.length());
            float groupWidth = iconSize + iconSpacing + hintWidth;
            float groupStart = Math.max(0f, (getWidth() - groupWidth) / 2f);

            float dx;
            if (isRtl) {
                float targetHintRight = groupStart + groupWidth - iconSize - iconSpacing;
                float normalRight = getWidth() - getCompoundPaddingRight();
                dx = targetHintRight - normalRight;
            } else {
                float targetHintLeft = groupStart + iconSize + iconSpacing;
                float normalLeft = getCompoundPaddingLeft();
                dx = targetHintLeft - normalLeft;
            }
            canvas.translate(dx, 0);
        }

        super.onDraw(canvas);
        canvas.restoreToCount(save);

        drawSearchIcon(canvas, isIdle);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        updateWidthState();
        invalidate();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        updateWidthState();
        invalidate();
    }

    @Override
    public void initializeSearch(ActivityAllAppsContainerView<?> appsView) {
        mAppsView = appsView;
        mSearchBarController.initialize(
                new DefaultAppSearchAlgorithm(getContext(), mLauncher.getUiExecutor(), true),
                this, mLauncher, this);
        updateWidthState();
    }

    @Override
    public void onAppsUpdated() {
        mSearchBarController.refreshSearchResult();
    }

    @Override
    public void resetSearch() {
        mSearchBarController.reset();
        updateWidthState();
        invalidate();
    }

    public void focusSearchField() {
        mSearchBarController.focusSearchField();
    }

    @Override
    public boolean isSearchQueryEmpty() {
        String query = Utilities.trim(getEditableText().toString());
        return query.isEmpty();
    }

    @Override
    public void preDispatchKeyEvent(KeyEvent event) {
        if (!mSearchBarController.isSearchFieldFocused()
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            final int unicodeChar = event.getUnicodeChar();
            final boolean isKeyNotWhitespace = unicodeChar > 0
                    && !Character.isWhitespace(unicodeChar) && !Character.isSpaceChar(unicodeChar);
            if (isKeyNotWhitespace) {
                boolean gotKey = TextKeyListener.getInstance().onKeyDown(this, mSearchQueryBuilder,
                        event.getKeyCode(), event);
                if (gotKey && mSearchQueryBuilder.length() > 0) {
                    mSearchBarController.focusSearchField();
                }
            }
        }
    }

    @Override
    public void onSearchResult(String query, ArrayList<AdapterItem> items) {
        if (query.equalsIgnoreCase(getContext().getString(R.string.private_space_label))) {
            privateSpaceQuery();
            return;
        }
        if (items != null && mAppsView != null) {
            mAppsView.setSearchResults(items);
        }
        updateWidthState();
    }

    @Override
    public void clearSearchResult() {
        mSearchQueryBuilder.clear();
        mSearchQueryBuilder.clearSpans();
        Selection.setSelection(mSearchQueryBuilder, 0);
        if (mAppsView != null) {
            mAppsView.onClearSearchResult();
        }
        updateWidthState();
        invalidate();
    }

    private void updateWidthState() {
        if (mAppearance != null && mAppearance.isNarrow()) {
            boolean isSearching = mAppsView != null && mAppsView.isSearching();
            boolean isActive = isFocused() || mImeVisible || isSearching;
            float target = isActive ? ACTIVE_WIDTH_EXPANSION : NARROW_IDLE_WIDTH_EXPANSION;
            if (!isLaidOut()) {
                mWidthExpansion = target;
                requestLayout();
                return;
            }
            animateWidth(isActive);
        } else if (mWidthExpansion != ACTIVE_WIDTH_EXPANSION) {
            mWidthExpansion = ACTIVE_WIDTH_EXPANSION;
            requestLayout();
        }
    }

    private void animateWidth(boolean expand) {
        float target = (mAppearance != null && mAppearance.isNarrow())
                ? (expand ? ACTIVE_WIDTH_EXPANSION : NARROW_IDLE_WIDTH_EXPANSION)
                : ACTIVE_WIDTH_EXPANSION;
        if (mWidthExpansion == target && (mWidthAnimator == null || !mWidthAnimator.isRunning())) {
            return;
        }

        if (mWidthAnimator != null) {
            mWidthAnimator.cancel();
        }

        mWidthAnimator = ValueAnimator.ofFloat(mWidthExpansion, target);
        mWidthAnimator.setDuration(220);
        mWidthAnimator.setInterpolator(Interpolators.STANDARD);
        mWidthAnimator.addUpdateListener(animator -> {
            mWidthExpansion = (float) animator.getAnimatedValue();
            requestLayout();
        });
        mWidthAnimator.start();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        boolean imeVisible = insets.isVisible(WindowInsets.Type.ime());
        mImeVisible = imeVisible;
        updateWidthState();

        if (mAppsView != null && mAppsView.isSearchBarAtBottom()) {
            // The bar already rests above the nav bar, and the IME inset includes the nav bar.
            int ime = imeVisible ? insets.getInsets(WindowInsets.Type.ime()).bottom : 0;
            int nav = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
            setTranslationY(-Math.max(0, ime - nav));
        } else {
            setTranslationY(0f);
        }
        return super.onApplyWindowInsets(insets);
    }

    @Override
    public void setInsets(Rect insets) {
        MarginLayoutParams mlp = (MarginLayoutParams) getLayoutParams();
        if (mAppsView != null
                && (mAppsView.getSearchUiDelegate().isSearchBarFloating()
                || mLauncher.getDeviceProfile().getDeviceProperties().isLargeScreen())) {
            mlp.topMargin = insets.top;
        }
        requestLayout();
    }

    public void setPlacement(AllAppsSearchBarConfig.Placement placement) {
        mPlacement = placement != null ? placement : AllAppsSearchBarConfig.Placement.TOP;
    }

    public void setAppearance(AllAppsSearchBarConfig.Appearance appearance) {
        mAppearance = appearance != null ? appearance : AllAppsSearchBarConfig.Appearance.DEFAULT;
        updateWidthState();
        updateBackgroundAlpha();
        requestLayout();
        invalidate();
    }

    public AllAppsSearchBarConfig.Placement getPlacement() {
        return mPlacement;
    }

    public AllAppsSearchBarConfig.Appearance getAppearance() {
        return mAppearance;
    }

    public boolean isNarrow() {
        return mAppearance != null && mAppearance.isNarrow();
    }

    @Override
    public ExtendedEditText getEditText() {
        return this;
    }

    private void privateSpaceQuery() {
        if (mAppsView == null) return;
        PrivateProfileManager privateProfileManager = mAppsView.getPrivateProfileManager();
        if (privateProfileManager.isPrivateSpaceHidden()) {
            privateProfileManager.setQuietMode(false);
        } else if (!mAppsView.hasPrivateProfile()) {
            final Intent privateSpaceSettingsIntent =
                    ApiWrapper.INSTANCE.get(getContext()).getPrivateSpaceSettingsIntent();
            if (privateSpaceSettingsIntent != null) {
                mLauncher.startActivitySafely(mAppsView, privateSpaceSettingsIntent, null);
            }
        }
    }
}
