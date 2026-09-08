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

public class IOSRecentLayout implements RecentLayoutHandler {

    private static final float CENTER_SCALE = 1.00f;
    private static final float NEIGHBOR_SCALE_1 = 0.94f;
    private static final float NEIGHBOR_SCALE_2 = 0.88f;
    private static final float MIN_SCALE = 0.80f;

    private static final float OVERLAP_DIST_1 = 0.22f;
    private static final float OVERLAP_DIST_2 = 0.40f;
    private static final float OVERLAP_DIST_3 = 0.58f;

    @NonNull
    @Override
    public RecentStyle getStyle() {
        return RecentStyle.IOS;
    }

    @Override
    public void onDetached(@NonNull RecentsView<?, ?> recentsView) {
        int childCount = recentsView.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = recentsView.getChildAt(i);
            if (child instanceof TaskView) {
                ((TaskView) child).setColorTint(0f, 0);
            }
        }
    }

    @Nullable
    @Override
    public Boolean isTaskViewVisible(@NonNull RecentsView<?, ?> recentsView, @NonNull TaskView taskView) {
        int childIndex = recentsView.indexOfChild(taskView);
        if (childIndex < 0)
            return false;
        int scroll = recentsView.getPagedOrientationHandler().getPrimaryScroll(recentsView);
        int pageScroll = recentsView.getScrollForPage(childIndex);
        int pageSize = recentsView.getLastComputedTaskSize().width();
        if (pageSize <= 0)
            pageSize = recentsView.getWidth();
        if (pageSize <= 0)
            return true;

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
        float elevation = getElevationForDistance(d, density);
        float alpha = getAlphaForDistance(d);

        float visualDist = getVisualDistance(d, childSize);
        float signedVisualOffset = (p >= 0f) ? -visualDist : visualDist;

        float transX = signedVisualOffset + (p * childSize);
        if (recentsView.isRtl()) {
            transX = -transX;
        }

        taskView.setColorTint(0f, 0);

        outTransform.scale = scale;
        outTransform.translationX = transX;
        outTransform.translationY = 0f;
        outTransform.elevation = elevation;
        outTransform.alpha = alpha;
    }

    private float smoothstep(float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        return clamped * clamped * (3f - 2f * clamped);
    }

    private float getScaleForDistance(float d) {
        if (d <= 1.0f) {
            float t = smoothstep(d);
            return CENTER_SCALE - t * (CENTER_SCALE - NEIGHBOR_SCALE_1);
        } else if (d <= 2.0f) {
            return NEIGHBOR_SCALE_1 - (d - 1.0f) * (NEIGHBOR_SCALE_1 - NEIGHBOR_SCALE_2);
        } else if (d <= 3.0f) {
            return NEIGHBOR_SCALE_2 - (d - 2.0f) * (NEIGHBOR_SCALE_2 - MIN_SCALE);
        } else {
            return Math.max(0.70f, MIN_SCALE - (d - 3.0f) * 0.04f);
        }
    }

    private float getElevationForDistance(float d, float density) {
        if (d <= 1.0f) {
            float t = smoothstep(d);
            return (32f - t * 12f) * density;
        } else if (d <= 2.0f) {
            return (20f - (d - 1.0f) * 10f) * density;
        } else if (d <= 3.0f) {
            return (10f - (d - 2.0f) * 6f) * density;
        } else {
            return Math.max(1f * density, (4f - (d - 3.0f) * 2f) * density);
        }
    }

    private float getVisualDistance(float d, float childSize) {
        if (d <= 1.0f) {
            return d * (OVERLAP_DIST_1 * childSize);
        } else if (d <= 2.0f) {
            return (OVERLAP_DIST_1 + (d - 1.0f) * (OVERLAP_DIST_2 - OVERLAP_DIST_1)) * childSize;
        } else if (d <= 3.0f) {
            return (OVERLAP_DIST_2 + (d - 2.0f) * (OVERLAP_DIST_3 - OVERLAP_DIST_2)) * childSize;
        } else {
            return (OVERLAP_DIST_3 + (d - 3.0f) * 0.20f) * childSize;
        }
    }

    private float getAlphaForDistance(float d) {
        if (d <= 3.2f) {
            return 1.0f;
        } else if (d <= 4.2f) {
            return Math.max(0f, 1.0f - (d - 3.2f) / 1.0f);
        } else {
            return 0f;
        }
    }
}
