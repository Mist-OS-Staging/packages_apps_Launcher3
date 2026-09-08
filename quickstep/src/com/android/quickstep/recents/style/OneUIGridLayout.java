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

import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.DeviceProfile;
import com.android.quickstep.views.ClearAllButton;
import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;

public class OneUIGridLayout implements RecentLayoutHandler {

    private static final float HORIZONTAL_MARGIN_DP = 14f;
    private static final float COL_SPACING_DP = 12f;
    private static final float ROW_SPACING_DP = 12f;
    private static final float TOP_PADDING_DP = 16f;
    private static final float BOTTOM_CLEARANCE_DP = 80f;

    private int getBaseScroll(RecentsView<?, ?> recentsView) {
        int childCount = recentsView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = recentsView.getChildAt(i);
            if (child instanceof TaskView) {
                return recentsView.getScrollForPage(i);
            }
        }
        return 0;
    }

    @Override
    public RecentStyle getStyle() {
        return RecentStyle.ONE_UI_GRID;
    }

    @Override
    public boolean isVerticalScrollStyle() {
        return false;
    }

    public static int computeRowsPerPage(RecentsView<?, ?> recentsView) {
        if (recentsView.getContainer() != null) {
            DeviceProfile dp = recentsView.getContainer().getDeviceProfile();
            if (dp != null && dp.getDeviceProperties().isLargeScreen()) {
                DisplayMetrics dm = recentsView.getResources().getDisplayMetrics();
                int h = recentsView.getHeight();
                if (h <= 0) {
                    h = dm.heightPixels;
                }
                float availableHeightDp = (h - (100f * dm.density)) / dm.density;
                return (availableHeightDp >= 700f) ? 3 : 2;
            }
        }
        return 2;
    }

    private int getPageWidth(RecentsView<?, ?> recentsView) {
        int w = recentsView.getWidth();
        if (w <= 0) {
            w = recentsView.getResources().getDisplayMetrics().widthPixels;
        }
        return w;
    }

    @Override
    public boolean getPageScrolls(@NonNull RecentsView<?, ?> recentsView, int[] outPageScrolls) {
        int taskCount = recentsView.getTaskViewCount();
        if (taskCount == 0 || outPageScrolls == null || outPageScrolls.length == 0) {
            return false;
        }

        int baseScroll = 0;
        int firstTaskChildIndex = -1;
        int childCount = recentsView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = recentsView.getChildAt(i);
            if (child instanceof TaskView) {
                firstTaskChildIndex = i;
                break;
            }
        }
        if (firstTaskChildIndex >= 0 && firstTaskChildIndex < outPageScrolls.length) {
            baseScroll = outPageScrolls[firstTaskChildIndex];
        }

        int rowsPerPage = computeRowsPerPage(recentsView);
        int tasksPerPage = rowsPerPage * 2;
        int pageCount = Math.max(1, (taskCount + tasksPerPage - 1) / tasksPerPage);
        int pageWidth = getPageWidth(recentsView);
        boolean isRtl = recentsView.isRtl();

        int taskIndex = 0;
        for (int i = 0; i < childCount; i++) {
            View child = recentsView.getChildAt(i);
            if (child instanceof TaskView) {
                int page = taskIndex / tasksPerPage;
                int pageScroll = isRtl
                        ? (baseScroll - page * pageWidth)
                        : (baseScroll + page * pageWidth);
                if (i < outPageScrolls.length) {
                    outPageScrolls[i] = pageScroll;
                }
                taskIndex++;
            }
        }

        ClearAllButton clearAll = recentsView.getClearAllButton();
        if (clearAll != null) {
            int clearAllIndex = recentsView.indexOfChild(clearAll);
            if (clearAllIndex >= 0 && clearAllIndex < outPageScrolls.length) {
                int clearAllScroll = isRtl
                        ? (baseScroll - (pageCount - 1) * pageWidth)
                        : (baseScroll + (pageCount - 1) * pageWidth);
                outPageScrolls[clearAllIndex] = clearAllScroll;
            }
        }

        return true;
    }

    @Nullable
    @Override
    public Integer computeMinScroll(@NonNull RecentsView<?, ?> recentsView) {
        int taskCount = recentsView.getTaskViewCount();
        if (taskCount == 0) return null;

        int rowsPerPage = computeRowsPerPage(recentsView);
        int tasksPerPage = rowsPerPage * 2;
        int pageCount = Math.max(1, (taskCount + tasksPerPage - 1) / tasksPerPage);
        int pageWidth = getPageWidth(recentsView);
        boolean isRtl = recentsView.isRtl();
        int baseScroll = getBaseScroll(recentsView);

        if (isRtl) {
            return baseScroll - ((pageCount - 1) * pageWidth);
        } else {
            return baseScroll;
        }
    }

    @Nullable
    @Override
    public Integer computeMaxScroll(@NonNull RecentsView<?, ?> recentsView) {
        int taskCount = recentsView.getTaskViewCount();
        if (taskCount == 0) return null;

        int rowsPerPage = computeRowsPerPage(recentsView);
        int tasksPerPage = rowsPerPage * 2;
        int pageCount = Math.max(1, (taskCount + tasksPerPage - 1) / tasksPerPage);
        int pageWidth = getPageWidth(recentsView);
        boolean isRtl = recentsView.isRtl();
        int baseScroll = getBaseScroll(recentsView);

        if (isRtl) {
            return baseScroll;
        } else {
            return baseScroll + ((pageCount - 1) * pageWidth);
        }
    }

    private int getTaskIndex(RecentsView<?, ?> recentsView, TaskView taskView) {
        int idx = 0;
        int count = recentsView.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = recentsView.getChildAt(i);
            if (child == taskView) {
                return idx;
            }
            if (child instanceof TaskView) {
                idx++;
            }
        }
        return -1;
    }

    @Nullable
    @Override
    public Boolean isTaskViewVisible(@NonNull RecentsView<?, ?> recentsView, @NonNull TaskView taskView) {
        int taskIndex = getTaskIndex(recentsView, taskView);
        if (taskIndex < 0) return false;
        int taskCount = recentsView.getTaskViewCount();
        if (taskCount == 0) return false;

        int rowsPerPage = computeRowsPerPage(recentsView);
        int tasksPerPage = rowsPerPage * 2;
        int taskPage = taskIndex / tasksPerPage;

        int pageWidth = getPageWidth(recentsView);
        if (pageWidth <= 0) return true;

        int baseScroll = getBaseScroll(recentsView);
        int currentScroll = recentsView.getPagedOrientationHandler().getPrimaryScroll(recentsView);
        boolean isRtl = recentsView.isRtl();
        float currentContinuousPage = isRtl
                ? (baseScroll - currentScroll) / (float) pageWidth
                : (currentScroll - baseScroll) / (float) pageWidth;

        return Math.abs(taskPage - currentContinuousPage) <= 1.5f;
    }

    @Override
    public void calculateTransform(
            @NonNull RecentsView<?, ?> recentsView,
            @NonNull TaskView taskView,
            int taskIndex,
            int taskCount,
            float scrollProgress,
            @NonNull StyleTransform outTransform) {

        outTransform.reset();
        if (taskCount == 0) return;

        DisplayMetrics dm = recentsView.getResources().getDisplayMetrics();
        float density = dm.density;

        int containerWidth = recentsView.getWidth();
        int containerHeight = recentsView.getHeight();
        if (containerWidth <= 0) containerWidth = dm.widthPixels;
        if (containerHeight <= 0) containerHeight = dm.heightPixels;

        int rowsPerPage = computeRowsPerPage(recentsView);
        int tasksPerPage = rowsPerPage * 2;

        int page = taskIndex / tasksPerPage;
        int posInPage = taskIndex % tasksPerPage;
        int row = posInPage / 2;
        int col = posInPage % 2;

        Rect insets = null;
        if (recentsView.getContainer() != null) {
            DeviceProfile dp = recentsView.getContainer().getDeviceProfile();
            if (dp != null) {
                insets = dp.getInsets();
            }
        }
        int insetsTop = (insets != null) ? insets.top : 0;
        int insetsBottom = (insets != null) ? insets.bottom : 0;

        float horizontalMargin = HORIZONTAL_MARGIN_DP * density;
        float colSpacing = COL_SPACING_DP * density;
        float rowSpacing = ROW_SPACING_DP * density;
        float topPadding = insetsTop + (TOP_PADDING_DP * density);
        float bottomPadding = insetsBottom + (BOTTOM_CLEARANCE_DP * density);

        float availableWidth = Math.max(100f, containerWidth - (2f * horizontalMargin) - colSpacing);
        float cardWidth = availableWidth / 2f;

        float availableHeight = Math.max(100f, containerHeight - topPadding - bottomPadding - ((rowsPerPage - 1) * rowSpacing));
        float cardHeight = availableHeight / (float) rowsPerPage;

        int fullWidth = recentsView.getLastComputedTaskSize().width();
        int fullHeight = recentsView.getLastComputedTaskSize().height();
        if (fullWidth <= 0) fullWidth = containerWidth;
        if (fullHeight <= 0) fullHeight = containerHeight;

        float scale = Math.min(cardWidth / (float) fullWidth, cardHeight / (float) fullHeight);

        boolean isRtl = recentsView.isRtl();
        float cellLeft = isRtl
                ? (containerWidth - horizontalMargin - (col + 1) * cardWidth - col * colSpacing)
                : (horizontalMargin + col * (cardWidth + colSpacing));
        float cellTop = topPadding + row * (cardHeight + rowSpacing);

        float targetCenterXInPage = cellLeft + (cardWidth / 2f);
        float targetCenterYInPage = cellTop + (cardHeight / 2f);

        int pageWidth = containerWidth;
        int baseScroll = getBaseScroll(recentsView);
        int targetPageScroll = isRtl
                ? (baseScroll - page * pageWidth)
                : (baseScroll + page * pageWidth);

        int currentScroll = recentsView.getPagedOrientationHandler().getPrimaryScroll(recentsView);
        float pageOffsetOnScreen = targetPageScroll - currentScroll;
        float targetScreenCenterX = targetCenterXInPage + pageOffsetOnScreen;
        float targetScreenCenterY = targetCenterYInPage;

        float nativeScreenCenterX = taskView.getLeft() + (taskView.getWidth() / 2f) - currentScroll;
        float nativeScreenCenterY = taskView.getTop() + (taskView.getHeight() / 2f);

        outTransform.scale = scale;
        outTransform.translationX = targetScreenCenterX - nativeScreenCenterX;
        outTransform.translationY = targetScreenCenterY - nativeScreenCenterY;
        outTransform.elevation = 6f * density;
        outTransform.alpha = 1f;
    }

    @Override
    public void onPostUpdateCurveProperties(@NonNull RecentsView<?, ?> recentsView) {
        ClearAllButton clearAll = recentsView.getClearAllButton();
        if (clearAll == null) return;
        int taskCount = recentsView.getTaskViewCount();
        if (taskCount == 0) {
            clearAll.setAlpha(0f);
            clearAll.setVisibility(View.INVISIBLE);
            return;
        }

        int rowsPerPage = computeRowsPerPage(recentsView);
        int tasksPerPage = rowsPerPage * 2;
        int pageCount = Math.max(1, (taskCount + tasksPerPage - 1) / tasksPerPage);

        int containerWidth = recentsView.getWidth();
        int containerHeight = recentsView.getHeight();
        if (containerWidth <= 0) return;

        boolean isRtl = recentsView.isRtl();
        int pageWidth = containerWidth;
        int baseScroll = getBaseScroll(recentsView);
        int targetPageScroll = isRtl
                ? (baseScroll - (pageCount - 1) * pageWidth)
                : (baseScroll + (pageCount - 1) * pageWidth);

        int currentScroll = recentsView.getPagedOrientationHandler().getPrimaryScroll(recentsView);
        float currentContinuousPage = isRtl
                ? (baseScroll - currentScroll) / (float) pageWidth
                : (currentScroll - baseScroll) / (float) pageWidth;

        float progressToLastPage = Math.max(0f, 1f - Math.abs(currentContinuousPage - (pageCount - 1)));
        if (progressToLastPage <= 0.05f) {
            clearAll.setAlpha(0f);
            clearAll.setVisibility(View.INVISIBLE);
            return;
        }

        DisplayMetrics dm = recentsView.getResources().getDisplayMetrics();
        float density = dm.density;

        Rect insets = null;
        if (recentsView.getContainer() != null) {
            DeviceProfile dp = recentsView.getContainer().getDeviceProfile();
            if (dp != null) {
                insets = dp.getInsets();
            }
        }
        int insetsTop = (insets != null) ? insets.top : 0;
        int insetsBottom = (insets != null) ? insets.bottom : 0;

        float rowSpacing = ROW_SPACING_DP * density;
        float topPadding = insetsTop + (TOP_PADDING_DP * density);
        float bottomPadding = insetsBottom + (BOTTOM_CLEARANCE_DP * density);

        float availableHeight = Math.max(100f, containerHeight - topPadding - bottomPadding - ((rowsPerPage - 1) * rowSpacing));
        float cardHeight = availableHeight / (float) rowsPerPage;

        float pageOffsetOnScreen = targetPageScroll - currentScroll;
        float targetScreenCenterX = (containerWidth / 2f) + pageOffsetOnScreen;

        int remainder = taskCount % tasksPerPage;
        int tasksOnLastPage = (remainder == 0) ? tasksPerPage : remainder;

        float targetScreenCenterY;
        if (tasksOnLastPage <= 2) {
            targetScreenCenterY = topPadding + 1 * (cardHeight + rowSpacing) + (cardHeight / 2f);
        } else {
            targetScreenCenterY = containerHeight - bottomPadding + (20f * density);
        }

        float nativeScreenCenterX = clearAll.getLeft() + (clearAll.getWidth() / 2f) - currentScroll;
        float nativeScreenCenterY = clearAll.getTop() + (clearAll.getHeight() / 2f);

        clearAll.setTranslationX(targetScreenCenterX - nativeScreenCenterX);
        clearAll.setTranslationY(targetScreenCenterY - nativeScreenCenterY);

        clearAll.setAlpha(progressToLastPage);
        clearAll.setScrollAlpha(progressToLastPage);
        clearAll.setVisibility(View.VISIBLE);
    }

    private int getChildIndexForTask(RecentsView<?, ?> recentsView, int targetTaskIndex) {
        int taskIdx = 0;
        int count = recentsView.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = recentsView.getChildAt(i);
            if (child instanceof TaskView) {
                if (taskIdx == targetTaskIndex) {
                    return i;
                }
                taskIdx++;
            }
        }
        return -1;
    }

    @Nullable
    @Override
    public Integer getDestinationPage(@NonNull RecentsView<?, ?> recentsView, int scaledScroll) {
        int taskCount = recentsView.getTaskViewCount();
        if (taskCount == 0) return null;

        int rowsPerPage = computeRowsPerPage(recentsView);
        int tasksPerPage = rowsPerPage * 2;
        int pageCount = Math.max(1, (taskCount + tasksPerPage - 1) / tasksPerPage);
        int pageWidth = getPageWidth(recentsView);
        if (pageWidth <= 0) return null;

        boolean isRtl = recentsView.isRtl();
        int baseScroll = getBaseScroll(recentsView);
        float currentContinuousPage = isRtl
                ? (baseScroll - scaledScroll) / (float) pageWidth
                : (scaledScroll - baseScroll) / (float) pageWidth;

        int targetPage = Math.round(currentContinuousPage);
        targetPage = Math.max(0, Math.min(pageCount - 1, targetPage));

        int taskIndex = targetPage * tasksPerPage;
        int childIdx = getChildIndexForTask(recentsView, taskIndex);
        if (childIdx >= 0) {
            return childIdx;
        }
        return null;
    }

    @Nullable
    @Override
    public Integer snapToPageWithVelocity(@NonNull RecentsView<?, ?> recentsView, int whichPage, int velocity) {
        int taskCount = recentsView.getTaskViewCount();
        if (taskCount == 0) return null;

        int rowsPerPage = computeRowsPerPage(recentsView);
        int tasksPerPage = rowsPerPage * 2;
        int pageCount = Math.max(1, (taskCount + tasksPerPage - 1) / tasksPerPage);
        int pageWidth = getPageWidth(recentsView);
        if (pageWidth <= 0) return null;

        boolean isRtl = recentsView.isRtl();
        int baseScroll = getBaseScroll(recentsView);
        int currentScroll = recentsView.getPagedOrientationHandler().getPrimaryScroll(recentsView);
        float currentContinuousPage = isRtl
                ? (baseScroll - currentScroll) / (float) pageWidth
                : (currentScroll - baseScroll) / (float) pageWidth;

        int targetPage;
        boolean flingForward = isRtl ? (velocity > 0) : (velocity < 0);
        if (Math.abs(velocity) >= 400) {
            targetPage = flingForward
                    ? ((int) Math.floor(currentContinuousPage) + 1)
                    : ((int) Math.ceil(currentContinuousPage) - 1);
        } else {
            targetPage = Math.round(currentContinuousPage);
        }
        targetPage = Math.max(0, Math.min(pageCount - 1, targetPage));

        int taskIndex = targetPage * tasksPerPage;
        int childIdx = getChildIndexForTask(recentsView, taskIndex);
        if (childIdx >= 0) {
            return childIdx;
        }
        return null;
    }

    @Override
    public void onTaskDismissed(@NonNull RecentsView<?, ?> recentsView, @NonNull TaskView taskView) {
    }
}
