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

import com.android.launcher3.LauncherPrefs;
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

    public RecentStyleController(@NonNull Context context) {
        mContext = context;
        ActivityManager am = context.getSystemService(ActivityManager.class);
        mIsLowRam = am != null && am.isLowRamDevice();
        loadStylePreference();
    }

    public void attach(@NonNull RecentsView<?, ?> recentsView) {
        mRecentsView = recentsView;
        try {
            LauncherPrefs.getPrefs(mContext).registerOnSharedPreferenceChangeListener(this);
        } catch (Exception e) {
            Log.w(TAG, "Failed to register preference listener: " + e.getMessage());
        }
        loadStylePreference();
        mActiveHandler.onAttached(recentsView);
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
        return mCurrentStyle;
    }

    public boolean isCustomStyleActive() {
        return mCurrentStyle != RecentStyle.DEFAULT;
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
        if (mCurrentStyle == newStyle && mPendingStyle == null) {
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
        mActiveHandler = createHandlerForStyle(newStyle);

        if (mRecentsView != null) {
            mActiveHandler.onAttached(mRecentsView);
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

        int taskCount = recentsView.getTaskViewCount();
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
        int childCount = recentsView.getChildCount();

        int taskIndex = 0;
        for (int i = 0; i < childCount; i++) {
            View child = recentsView.getChildAt(i);
            if (!(child instanceof TaskView)) {
                continue;
            }
            TaskView taskView = (TaskView) child;

            int pageScroll = recentsView.getScrollForPage(i);
            float reflowTranslation = taskView.getPrimaryDismissTranslation();
            float scrollDelta = (scroll - pageScroll) - reflowTranslation;

            float scrollProgress = scrollDelta / (float) pageSize;
            if (isRtl) {
                scrollProgress = -scrollProgress;
            }

            mCachedTransform.reset();
            mActiveHandler.calculateTransform(
                    recentsView, taskView, taskIndex, taskCount, scrollProgress, mCachedTransform);

            if (taskView.isBeingDismissed()) {
                float dismissY = Math.abs(taskView.getSecondaryDismissTranslation());
                float taskHeight = taskView.getHeight();
                if (taskHeight <= 0) {
                    taskHeight = recentsView.getHeight() * 0.4f;
                }
                float dismissProgress = Math.min(1f, dismissY / Math.max(1f, taskHeight * 0.5f));
                mCachedTransform.alpha *= Math.max(0f, 1f - dismissProgress);
            }

            mCachedTransform.applyTo(taskView);

            taskIndex++;
        }

        mActiveHandler.onPostUpdateCurveProperties(recentsView);
    }

    public void onTaskDismissed(@NonNull RecentsView<?, ?> recentsView, @NonNull TaskView taskView) {
        mActiveHandler.onTaskDismissed(recentsView, taskView);
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
        if (mCurrentStyle == RecentStyle.ONE_UI_STACK) {
            int childIndex = recentsView.indexOfChild(taskView);
            if (childIndex < 0) return false;
            int scroll = recentsView.getPagedOrientationHandler().getPrimaryScroll(recentsView);
            int pageScroll = recentsView.getScrollForPage(childIndex);
            int pageSize = recentsView.getLastComputedTaskSize().width();
            if (pageSize <= 0) pageSize = recentsView.getWidth();
            if (pageSize <= 0) return true;

            float scrollDelta = scroll - pageScroll;
            float p = scrollDelta / (float) pageSize;
            return Math.abs(p) <= 4.5f;
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
