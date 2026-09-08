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

import androidx.annotation.NonNull;

import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;

public class MistifyStackLayout implements RecentLayoutHandler {

    private static final float STAPLE_RATIO = 0.58f;
    private static final float SQUISH_FACTOR = 18f;
    private static final float MIN_SCALE = 0.74f;

    @NonNull
    @Override
    public RecentStyle getStyle() {
        return RecentStyle.ONE_UI_STACK;
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

        float dist = -scrollProgress * childSize;
        float absDist = Math.abs(dist);

        float stapleDistance = childSize * STAPLE_RATIO;

        float transX = 0f;
        float transY = 0f;
        float scale = 1.0f;
        float elevation = 20f * density;

        if (absDist > stapleDistance) {
            float excess = absDist - stapleDistance;
            float squish = (float) (Math.log10(1f + excess) * (SQUISH_FACTOR * density / 2.75f));
            float targetVisualDist = stapleDistance + squish;
            float pullBack = absDist - targetVisualDist;

            transX = (dist > 0f) ? -pullBack : pullBack;
            if (recentsView.isRtl()) {
                transX = -transX;
            }

            float peekFactor = Math.min(3.5f, excess / (childSize * 0.45f));
            transY = -peekFactor * (14f * density);

            float scaleFactor = 1.0f - (excess / (childSize * 2.2f)) * 0.22f;
            scale = Math.max(MIN_SCALE, scaleFactor);

            elevation = Math.max(2f * density, (20f - (excess / childSize) * 14f) * density);
        }

        float distSteps = absDist / childSize;
        float alpha;
        if (distSteps <= 3.2f) {
            alpha = 1.0f;
        } else if (distSteps < 4.2f) {
            alpha = Math.max(0f, 1.0f - (distSteps - 3.2f) / 1.0f);
        } else {
            alpha = 0f;
        }

        outTransform.scale = scale;
        outTransform.translationX = transX;
        outTransform.translationY = transY;
        outTransform.elevation = elevation;
        outTransform.alpha = alpha;
    }
}
