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

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;

public class HorizontalLayout implements RecentLayoutHandler {

    private static final float CENTER_SCALE = 1.00f;
    private static final float NEAR_SCALE = 0.90f;
    private static final float SIDE_SCALE = 0.80f;
    private static final float FAR_SCALE = 0.70f;
    private static final float MIN_SCALE = 0.60f;

    private static final float DIST_NEAR = 0.70f;
    private static final float DIST_SIDE = 1.32f;
    private static final float DIST_FAR = 1.90f;

    @NonNull
    @Override
    public RecentStyle getStyle() {
        return RecentStyle.MIUI_HORIZONTAL;
    }

    @Nullable
    @Override
    public Boolean isTaskViewVisible(@NonNull RecentsView<?, ?> recentsView, @NonNull TaskView taskView) {
        int childIndex = recentsView.indexOfChild(taskView);
        if (childIndex < 0) return false;
        int scroll = recentsView.getPagedOrientationHandler().getPrimaryScroll(recentsView);
        int pageScroll = recentsView.getScrollForPage(childIndex);
        int pageSize = recentsView.getLastComputedTaskSize().width();
        if (pageSize <= 0) pageSize = recentsView.getWidth();
        if (pageSize <= 0) return true;

        float scrollDelta = scroll - pageScroll;
        float p = scrollDelta / (float) pageSize;
        return Math.abs(p) <= 4.0f;
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

        int fullWidth = recentsView.getLastComputedTaskSize().width();
        int fullHeight = recentsView.getLastComputedTaskSize().height();
        if (fullWidth <= 0 || fullHeight <= 0) {
            return;
        }

        float density = recentsView.getResources().getDisplayMetrics().density;
        float childSize = (float) fullWidth;

        float p = scrollProgress;
        float d = Math.abs(p);

        float scale = getScaleForDistance(d);

        float visualDist = getVisualDistance(d, childSize);
        float signedVisualOffset = (p >= 0f) ? -visualDist : visualDist;

        float transX = signedVisualOffset + (p * childSize);
        if (recentsView.isRtl()) {
            transX = -transX;
        }

        float elevation = getElevationForDistance(d, density);
        float alpha = getAlphaForDistance(d);

        taskView.setColorTint(0f, 0);

        outTransform.scale = scale;
        outTransform.translationX = transX;
        outTransform.translationY = 0f;
        outTransform.elevation = elevation;
        outTransform.alpha = alpha;
    }

    private float getScaleForDistance(float d) {
        if (d <= 1.0f) {
            return CENTER_SCALE - d * (CENTER_SCALE - NEAR_SCALE);
        } else if (d <= 2.0f) {
            return NEAR_SCALE - (d - 1.0f) * (NEAR_SCALE - SIDE_SCALE);
        } else if (d <= 3.0f) {
            return SIDE_SCALE - (d - 2.0f) * (SIDE_SCALE - FAR_SCALE);
        } else {
            return Math.max(MIN_SCALE, FAR_SCALE - (d - 3.0f) * 0.08f);
        }
    }

    private float getVisualDistance(float d, float childSize) {
        if (d <= 1.0f) {
            return d * (DIST_NEAR * childSize);
        } else if (d <= 2.0f) {
            return (DIST_NEAR + (d - 1.0f) * (DIST_SIDE - DIST_NEAR)) * childSize;
        } else if (d <= 3.0f) {
            return (DIST_SIDE + (d - 2.0f) * (DIST_FAR - DIST_SIDE)) * childSize;
        } else {
            return (DIST_FAR + (d - 3.0f) * 0.52f) * childSize;
        }
    }

    private float getElevationForDistance(float d, float density) {
        if (d <= 1.0f) {
            return (24f - d * 8f) * density;
        } else if (d <= 2.0f) {
            return (16f - (d - 1.0f) * 6f) * density;
        } else if (d <= 3.0f) {
            return (10f - (d - 2.0f) * 5f) * density;
        } else {
            return Math.max(1f * density, (5f - (d - 3.0f) * 3f) * density);
        }
    }

    private float getAlphaForDistance(float d) {
        if (d <= 2.8f) {
            return 1.0f;
        } else if (d <= 3.8f) {
            return Math.max(0f, 1.0f - (d - 2.8f) / 1.0f);
        } else {
            return 0f;
        }
    }
}
