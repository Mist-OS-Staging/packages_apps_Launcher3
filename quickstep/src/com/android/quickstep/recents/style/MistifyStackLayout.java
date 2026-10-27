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
import androidx.annotation.Nullable;

import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;

/**
 * Centered stack: the focused card is full size, and the cards on both sides are pulled in behind
 * it, smaller, slightly raised and overlapping, getting more squeezed the further they are.
 *
 * The transform is derived from the card's live position (see {@link StyleGeometry}), not from
 * the scroll position alone. That is what lets the neighbours visibly slide into the gap after a
 * card is swiped away, and keeps the stack consistent while a card is being dragged.
 *
 * Z-order is strictly "closer to the focused card is in front", on both sides.
 */
public class MistifyStackLayout implements RecentLayoutHandler {

    static final float STAPLE_RATIO = 0.58f;
    private static final float SQUISH_FACTOR = 18f;
    static final float MIN_SCALE = 0.74f;

    /** Cards further than this many pages from the focused one are faded out and not loaded. */
    private static final float VISIBLE_PAGES = 4.5f;

    private static final float PEEK_DP = 14f;
    private static final float MAX_ELEVATION_DP = 20f;
    /** Elevation lost across the flat part of the stack, between the center and the staple. */
    private static final float PLATEAU_DROP_DP = 2f;
    private static final float FAR_ELEVATION_DP = 2f;

    @NonNull
    @Override
    public RecentStyle getStyle() {
        return RecentStyle.ONE_UI_STACK;
    }

    @Override
    public boolean followsDismissProgress() {
        return true;
    }

    /** The card is flung off the top of the screen without fading. */
    @Override
    public float getDismissedTaskAlpha(float dismissProgress) {
        return 1f;
    }

    @Nullable
    @Override
    public Boolean isTaskViewVisible(@NonNull RecentsView<?, ?> recentsView,
            @NonNull TaskView taskView) {
        if (recentsView.indexOfChild(taskView) < 0) return false;
        if (!StyleGeometry.isNaturalOrientation(recentsView)) return null;
        if (taskView.getWidth() <= 0 || recentsView.getWidth() <= 0
                || recentsView.getLastComputedTaskSize().width() <= 0) return true;
        float pages = Math.abs(StyleGeometry.getNativeCenterOffset(recentsView, taskView))
                / StyleGeometry.getNominalPageSize(recentsView);
        return pages <= VISIBLE_PAGES;
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

        // The stack is horizontal; a rotated Recents keeps the stock layout.
        if (!StyleGeometry.isNaturalOrientation(recentsView)) {
            return;
        }
        int fullWidth = recentsView.getLastComputedTaskSize().width();
        int fullHeight = recentsView.getLastComputedTaskSize().height();
        if (fullWidth <= 0 || fullHeight <= 0 || taskView.getWidth() <= 0
                || recentsView.getWidth() <= 0) {
            return;
        }

        float density = recentsView.getResources().getDisplayMetrics().density;
        float childSize = (float) fullWidth;

        // Signed distance of the card from the focused position. It already includes the dismiss
        // translation, so a neighbour sliding into a gap moves continuously through the stack.
        float nativeOffset = StyleGeometry.getNativeCenterOffset(recentsView, taskView);
        float absDist = Math.abs(nativeOffset) / StyleGeometry.getNominalPageSize(recentsView)
                * childSize;

        // Pull the card towards the focused one, whichever side of it the card is on.
        float pullBack = getPullBack(absDist, childSize, density);
        outTransform.translationX = -Math.copySign(pullBack, nativeOffset);
        outTransform.translationY = getPeekOffset(absDist, childSize, density);
        outTransform.scale = getScaleForDistance(absDist, childSize);
        outTransform.elevation = getElevationForDistance(absDist, childSize, density);
        outTransform.alpha = getAlphaForSteps(absDist / childSize);
    }

    /** How far (px) a card is pulled back towards the focused one. 0 until the staple distance. */
    static float getPullBack(float absDist, float childSize, float density) {
        float stapleDistance = childSize * STAPLE_RATIO;
        if (absDist <= stapleDistance) return 0f;
        float excess = absDist - stapleDistance;
        float squish = (float) (Math.log10(1f + excess) * (SQUISH_FACTOR * density / 2.75f));
        return absDist - (stapleDistance + squish);
    }

    /** Vertical offset (px, negative is up) that lets the deeper cards peek out above. */
    static float getPeekOffset(float absDist, float childSize, float density) {
        float excess = absDist - childSize * STAPLE_RATIO;
        if (excess <= 0f) return 0f;
        float peekFactor = Math.min(3.5f, excess / (childSize * 0.45f));
        return -peekFactor * (PEEK_DP * density);
    }

    static float getScaleForDistance(float absDist, float childSize) {
        float excess = absDist - childSize * STAPLE_RATIO;
        if (excess <= 0f) return 1f;
        float scaleFactor = 1.0f - (excess / (childSize * 2.2f)) * 0.22f;
        return Math.max(MIN_SCALE, scaleFactor);
    }

    /**
     * Strictly decreasing with distance, which is what orders the cards front-to-back. The old
     * curve was flat before the staple distance and floored far out, so neighbouring cards on
     * one side could share the same elevation and were drawn in child order instead.
     */
    static float getElevationForDistance(float absDist, float childSize, float density) {
        float stapleDistance = childSize * STAPLE_RATIO;
        if (absDist <= stapleDistance) {
            float t = Math.max(0f, absDist) / stapleDistance;
            return (MAX_ELEVATION_DP - PLATEAU_DROP_DP * t) * density;
        }
        float excess = absDist - stapleDistance;
        float range = MAX_ELEVATION_DP - PLATEAU_DROP_DP - FAR_ELEVATION_DP;
        // Starts at the same value and roughly the same slope as the old linear fall-off.
        return (FAR_ELEVATION_DP + range * (float) Math.exp(-excess / (1.1f * childSize)))
                * density;
    }

    static float getAlphaForSteps(float distSteps) {
        if (distSteps <= 3.2f) return 1.0f;
        if (distSteps < 4.2f) return Math.max(0f, 1.0f - (distSteps - 3.2f));
        return 0f;
    }
}
