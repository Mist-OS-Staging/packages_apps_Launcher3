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
 * MIUI-style horizontal carousel: the focused card is full size and the cards on both sides get
 * smaller and are pulled in behind it, overlapping, with the closest card in front.
 *
 * The transform is derived from the card's live position (see {@link StyleGeometry}), not from the
 * scroll position alone. That lets the neighbours visibly slide into the gap after a card is
 * swiped away, and keeps the carousel consistent while a card is being dragged.
 */
public class HorizontalLayout implements RecentLayoutHandler {

    static final float CENTER_SCALE = 1.00f;
    static final float NEAR_SCALE = 0.90f;
    static final float SIDE_SCALE = 0.80f;
    static final float FAR_SCALE = 0.70f;
    static final float MIN_SCALE = 0.60f;

    static final float DIST_NEAR = 0.70f;
    static final float DIST_SIDE = 1.32f;
    static final float DIST_FAR = 1.90f;

    private static final float VISIBLE_DISTANCE = 4.0f;

    @NonNull
    @Override
    public RecentStyle getStyle() {
        return RecentStyle.MIUI_HORIZONTAL;
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
    public Boolean isTaskViewVisible(@NonNull RecentsView<?, ?> recentsView, @NonNull TaskView taskView) {
        if (recentsView.indexOfChild(taskView) < 0) return false;
        if (!StyleGeometry.isNaturalOrientation(recentsView)) return null;
        if (taskView.getWidth() <= 0 || recentsView.getWidth() <= 0
                || recentsView.getLastComputedTaskSize().width() <= 0) return true;
        float distance = Math.abs(StyleGeometry.getNativeCenterOffset(recentsView, taskView))
                / StyleGeometry.getNominalPageSize(recentsView);
        return distance <= VISIBLE_DISTANCE;
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

        // The carousel is horizontal; a rotated Recents keeps the stock layout.
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
        // translation, so a neighbour sliding into a gap moves continuously through the carousel.
        float nativeOffset = StyleGeometry.getNativeCenterOffset(recentsView, taskView);
        float d = Math.abs(nativeOffset) / StyleGeometry.getNominalPageSize(recentsView);

        // Pull the card towards the focused one, whichever side of it the card is on.
        float pullIn = d * childSize - getVisualDistance(d, childSize);

        taskView.setColorTint(0f, 0);

        outTransform.scale = getScaleForDistance(d);
        outTransform.translationX = -Math.copySign(pullIn, nativeOffset);
        outTransform.translationY = 0f;
        outTransform.elevation = getElevationForDistance(d, density);
        outTransform.alpha = getAlphaForDistance(d);
    }

    static float getScaleForDistance(float d) {
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

    static float getVisualDistance(float d, float childSize) {
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

    /**
     * Strictly decreasing with distance up to the point where the card has faded out, which is
     * what orders the cards front-to-back: the closer to the focused card, the further in front.
     */
    static float getElevationForDistance(float d, float density) {
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

    static float getAlphaForDistance(float d) {
        if (d <= 2.8f) {
            return 1.0f;
        } else if (d <= 3.8f) {
            return Math.max(0f, 1.0f - (d - 2.8f) / 1.0f);
        } else {
            return 0f;
        }
    }
}
