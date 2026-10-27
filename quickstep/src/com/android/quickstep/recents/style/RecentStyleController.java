/*
 * Copyright (C) 2026 MistOS
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

package com.android.quickstep.recents.style;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.display.DisplayController;
import com.android.launcher3.display.LauncherDisplayInfo;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;

public class RecentStyleController implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "RecentStyleController";

    private final Context mContext;
    private final boolean mIsLowRam;
    private final StyleTransform mCachedTransform = new StyleTransform();

    private RecentsView<?, ?> mRecentsView;
    private RecentStyle mCurrentStyle = RecentStyle.DEFAULT;
    private RecentStyle mPendingStyle = null;
    private RecentLayoutHandler mActiveHandler = new DefaultRecentLayout();
    private boolean mIsLaunching = false;
    private boolean mWasLargeScreen = false;

    public RecentStyleController(@NonNull Context context) {
        mContext = context;
        ActivityManager am = context.getSystemService(ActivityManager.class);
        mIsLowRam = am != null && am.isLowRamDevice();
        loadStylePreference();
    }

    public boolean isLargeScreen() {
        if (mRecentsView != null && mRecentsView.getContainer() != null) {
            DeviceProfile dp = mRecentsView.getContainer().getDeviceProfile();
            if (dp != null) {
                return dp.getDeviceProperties().isLargeScreen();
            }
        }
        try {
            LauncherDisplayInfo info = DisplayController.INSTANCE.get(mContext).getInfo();
            return info.isLargeScreen(info.realBounds);
        } catch (Exception e) {
            return false;
        }
    }

    public void attach(@NonNull RecentsView<?, ?> recentsView) {
        mRecentsView = recentsView;
        mWasLargeScreen = isLargeScreen();
        try {
            LauncherPrefs.getPrefs(mContext).registerOnSharedPreferenceChangeListener(this);
        } catch (Exception e) {
            Log.w(TAG, "Failed to register preference listener: " + e.getMessage());
        }
        loadStylePreference();
        mActiveHandler.onAttached(recentsView);
        recentsView.updateRecentStyleScrollMode();
    }

    public void detach() {
        try {
            LauncherPrefs.getPrefs(mContext).unregisterOnSharedPreferenceChangeListener(this);
        } catch (Exception e) {
            Log.w(TAG, "Failed to unregister preference listener: " + e.getMessage());
        }
        if (mRecentsView != null) {
            mActiveHandler.onDetached(mRecentsView);
            mRecentsView = null;
        }
        mPendingStyle = null;
        mIsLaunching = false;
    }

    public void setIsLaunching(boolean isLaunching) {
        mIsLaunching = isLaunching;
    }

    public boolean isLaunching() {
        return mIsLaunching;
    }

    @NonNull
    public RecentStyle getCurrentStyle() {
        if (isLargeScreen()) {
            return RecentStyle.DEFAULT;
        }
        return mCurrentStyle;
    }

    public boolean isCustomStyleActive() {
        boolean largeScreen = isLargeScreen();
        if (largeScreen != mWasLargeScreen) {
            onLargeScreenStateChanged(largeScreen);
        }
        if (largeScreen) {
            return false;
        }
        return mCurrentStyle != RecentStyle.DEFAULT;
    }

    private void onLargeScreenStateChanged(boolean isLargeScreen) {
        mWasLargeScreen = isLargeScreen;
        if (mRecentsView != null) {
            mRecentsView.post(() -> {
                setStyle(mCurrentStyle);
            });
        } else {
            mActiveHandler = isLargeScreen ? new DefaultRecentLayout() : createHandlerForStyle(mCurrentStyle);
        }
    }

    public boolean isLowRam() {
        return mIsLowRam;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (LauncherPrefs.RECENTS_STYLE.getSharedPrefKey().equals(key)) {
            loadStylePreference();
        }
    }

    private void loadStylePreference() {
        String styleKey;
        try {
            styleKey = LauncherPrefs.get(mContext).get(LauncherPrefs.RECENTS_STYLE);
        } catch (Exception e) {
            styleKey = "default";
        }

        RecentStyle newStyle = RecentStyle.fromKey(styleKey);
        setStyle(newStyle);
    }

    private boolean isSafeToSwitchStyle() {
        if (mRecentsView == null) {
            return true;
        }
        if (mRecentsView.isHandlingTouch()) {
            return false;
        }
        if (mRecentsView.getScroller() != null && !mRecentsView.getScroller().isFinished()) {
            return false;
        }
        if (mRecentsView.isSplitSelectionActive()) {
            return false;
        }
        for (TaskView tv : mRecentsView.getTaskViews()) {
            if (tv.isBeingDismissed()) {
                return false;
            }
        }
        return true;
    }

    public void setStyle(@NonNull RecentStyle newStyle) {
        if (mCurrentStyle == newStyle && mPendingStyle == null && !mWasLargeScreen) {
            return;
        }

        if (!isSafeToSwitchStyle()) {
            mPendingStyle = newStyle;
            if (mRecentsView != null) {
                mRecentsView.postDelayed(() -> {
                    if (mPendingStyle != null) {
                        RecentStyle target = mPendingStyle;
                        mPendingStyle = null;
                        setStyle(target);
                    }
                }, 150);
            }
            return;
        }

        mPendingStyle = null;
        mIsLaunching = false;
        RecentLayoutHandler oldHandler = mActiveHandler;
        if (mRecentsView != null) {
            if (oldHandler != null) {
                oldHandler.onDetached(mRecentsView);
            }
            int childCount = mRecentsView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = mRecentsView.getChildAt(i);
                if (child instanceof TaskView) {
                    ((TaskView) child).resetCustomStyleTransforms();
                }
            }
            com.android.quickstep.views.ClearAllButton clearAll = mRecentsView.getClearAllButton();
            if (clearAll != null) {
                clearAll.setTranslationX(0f);
                clearAll.setTranslationY(0f);
                clearAll.setAlpha(1f);
                clearAll.setVisibility(View.VISIBLE);
            }
        }

        mCurrentStyle = newStyle;
        mActiveHandler = isLargeScreen() ? new DefaultRecentLayout() : createHandlerForStyle(newStyle);

        if (mRecentsView != null) {
            mActiveHandler.onAttached(mRecentsView);
            mRecentsView.updateRecentStyleScrollMode();
            mRecentsView.post(() -> {
                if (mRecentsView != null) {
                    mRecentsView.requestLayout();
                    mRecentsView.updateCurveProperties();
                    mRecentsView.invalidate();
                }
            });
        }
    }

    @NonNull
    private RecentLayoutHandler createHandlerForStyle(@NonNull RecentStyle style) {
        if (isLargeScreen()) {
            return new DefaultRecentLayout();
        }
        switch (style) {
            case IOS:
                return new IOSRecentLayout();
            case ONE_UI_GRID:
                return new OneUIGridLayout();
            case ONE_UI_STACK:
                return new MistifyStackLayout();
            case MIUI_HORIZONTAL:
                return new HorizontalLayout();
            case DEFAULT:
            default:
                return new DefaultRecentLayout();
        }
    }

    public void updateCurveProperties(@NonNull RecentsView<?, ?> recentsView, int scroll) {
        if (!isCustomStyleActive() || mIsLaunching) {
            return;
        }

        // Count only non-dismissed tasks so that index-based transforms (elevation, stacking,
        // position) are stable while a task is animating out.  Including the dismissed task in
        // taskCount causes surviving tasks to receive wrong index values until the dismissed view
        // is actually removed, producing a one-frame elevation/position jump.
        int taskCount = 0;
        int childCount = recentsView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = recentsView.getChildAt(i);
            if (child instanceof TaskView && !((TaskView) child).isBeingDismissed()) {
                taskCount++;
            }
        }
        if (taskCount == 0) {
            return;
        }

        int childWidth = recentsView.getLastComputedTaskSize().width();
        if (childWidth <= 0) {
            childWidth = recentsView.getWidth();
            if (childWidth <= 0) {
                return;
            }
        }
        int pageSpacing = recentsView.getPageSpacing();
        int pageSize = childWidth + pageSpacing;

        boolean isRtl = recentsView.isRtl();

        int taskIndex = 0;
        for (int i = 0; i < childCount; i++) {
            View child = recentsView.getChildAt(i);
            if (!(child instanceof TaskView)) {
                continue;
            }
            TaskView taskView = (TaskView) child;

            int pageScroll = recentsView.getScrollForPage(i);

            // For the task being dismissed (user swipe), its primaryDismissTranslation is the
            // swipe gesture offset along the primary axis, so we subtract it to keep the custom
            // style pivot stable as the card flies out.
            //
            // For surviving tasks, primaryDismissTranslation is the reflow spring animation value.
            // applyTranslationX() already zeroes dismissTranslationX under custom styles, so the
            // view does NOT visually move by that amount.  Subtracting it here would corrupt
            // scrollProgress on every spring frame and cause the surviving task to jitter.
            float reflowTranslation = taskView.isBeingDismissed()
                    ? taskView.getPrimaryDismissTranslation()
                    : 0f;
            float scrollDelta = (scroll - pageScroll) - reflowTranslation;

            float scrollProgress = scrollDelta / (float) pageSize;
            if (isRtl) {
                scrollProgress = -scrollProgress;
            }

            mCachedTransform.reset();

            if (taskView.isBeingDismissed()) {
                // Calculate the style transform so the dismissed card retains its styled
                // position/scale/rotation/elevation as it animates out.
                mActiveHandler.calculateTransform(
                        recentsView, taskView, taskIndex, taskCount, scrollProgress, mCachedTransform);

                // Multiply the style-calculated alpha by the dismissal fade-out factor.
                float dismissY = Math.abs(taskView.getSecondaryDismissTranslation());
                float taskHeight = taskView.getHeight();
                if (taskHeight <= 0) {
                    taskHeight = recentsView.getHeight() * 0.4f;
                }
                float dismissProgress = Math.min(1f, dismissY / Math.max(1f, taskHeight * 0.5f));
                mCachedTransform.alpha *= mActiveHandler.getDismissedTaskAlpha(dismissProgress);

                mCachedTransform.applyTo(taskView);
                // taskIndex is NOT incremented: dismissed task is excluded from the index sequence.
                continue;
            }

            mActiveHandler.calculateTransform(
                    recentsView, taskView, taskIndex, taskCount, scrollProgress, mCachedTransform);

            mCachedTransform.applyTo(taskView);

            taskIndex++;
        }

        mActiveHandler.onPostUpdateCurveProperties(recentsView);
    }

    public void onTaskDismissed(@NonNull RecentsView<?, ?> recentsView, @NonNull TaskView taskView) {
        mActiveHandler.onTaskDismissed(recentsView, taskView);
    }

    /** Whether the dismiss translation (swipe + neighbour reflow) should be rendered as-is. */
    public boolean isDismissProgressFollowed() {
        return isCustomStyleActive() && mActiveHandler.followsDismissProgress();
    }

    /**
     * Called whenever a card's dismiss translation changes, i.e. on every frame of a swipe-up or
     * of the reflow springs that follow it. Re-evaluates the style synchronously so the card's
     * scale, z-order and position stay in lock-step with its translation (a one-frame lag would
     * show up as jitter).
     */
    public void onDismissTranslationChanged() {
        if (mRecentsView == null || mIsLaunching || !isDismissProgressFollowed()) {
            return;
        }
        mRecentsView.updateCurveProperties();
    }

    public boolean isVerticalScrollStyle() {
        return isCustomStyleActive() && mActiveHandler.isVerticalScrollStyle();
    }

    @Nullable
    public Integer computeMinScroll(@NonNull RecentsView<?, ?> recentsView) {
        if (!isCustomStyleActive())
            return null;
        return mActiveHandler.computeMinScroll(recentsView);
    }

    @Nullable
    public Integer computeMaxScroll(@NonNull RecentsView<?, ?> recentsView) {
        if (!isCustomStyleActive())
            return null;
        return mActiveHandler.computeMaxScroll(recentsView);
    }

    public boolean getPageScrolls(@NonNull RecentsView<?, ?> recentsView, int[] outPageScrolls) {
        if (!isCustomStyleActive())
            return false;
        return mActiveHandler.getPageScrolls(recentsView, outPageScrolls);
    }

    @Nullable
    public Boolean isTaskViewVisible(@NonNull RecentsView<?, ?> recentsView, @NonNull TaskView taskView) {
        if (!isCustomStyleActive()) {
            return null;
        }
        return mActiveHandler.isTaskViewVisible(recentsView, taskView);
    }

    @Nullable
    public Integer getDestinationPage(@NonNull RecentsView<?, ?> recentsView, int scaledScroll) {
        if (!isCustomStyleActive()) {
            return null;
        }
        return mActiveHandler.getDestinationPage(recentsView, scaledScroll);
    }

    @Nullable
    public Integer snapToPageWithVelocity(@NonNull RecentsView<?, ?> recentsView, int whichPage, int velocity) {
        if (!isCustomStyleActive()) {
            return null;
        }
        return mActiveHandler.snapToPageWithVelocity(recentsView, whichPage, velocity);
    }
}
