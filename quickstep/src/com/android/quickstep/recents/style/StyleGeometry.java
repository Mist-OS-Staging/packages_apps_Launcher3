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

/**
 * Live card geometry shared by the carousel-style layouts.
 *
 * Styles that derive their transforms from where a card really is, instead of from the scroll
 * position alone, keep working while the card is moved by something else: the swipe-to-dismiss
 * drag, the reflow springs that close the gap afterwards, and the app launch / Overview entry
 * animations.
 */
final class StyleGeometry {

    private StyleGeometry() {}

    /**
     * Only the phone carousel in its natural orientation is laid out horizontally. A rotated
     * Recents keeps the stock layout because the geometry below uses the horizontal axis.
     */
    static boolean isNaturalOrientation(@NonNull RecentsView<?, ?> recentsView) {
        return recentsView.getPagedOrientationHandler().isLayoutNaturalToLauncher();
    }

    /** Distance between the centers of two neighbouring cards in the stock carousel. */
    static float getNominalPageSize(@NonNull RecentsView<?, ?> recentsView) {
        int cardWidth = recentsView.getLastComputedTaskSize().width();
        return Math.max(1f, cardWidth + recentsView.getPageSpacing());
    }

    /**
     * Signed distance, in px, from the viewport center to the center of the card, without any
     * transform this style applied itself.
     *
     * Mirrors PagedView's screen-center calculation, including its scale and pivot, so the
     * geometry remains anchored during the app-to-Overview transition. Child left/top positions
     * are untransformed layout coordinates, and ordinary AOSP launch / gesture / dismiss
     * translations are preserved, which makes the result stable while Recents moves under a
     * running animation.
     */
    static float getNativeCenterOffset(@NonNull RecentsView<?, ?> recentsView,
            @NonNull TaskView taskView) {
        final float parentScale = Math.max(0.01f, recentsView.getScaleX());
        final float pivot = recentsView.getPivotX();
        final float viewportCenter =
                (recentsView.getWidth() * 0.5f - pivot) / parentScale + pivot;
        final float baseTranslation =
                taskView.getTranslationX() - taskView.getCustomStyleTranslationX();
        return (taskView.getLeft() + taskView.getWidth() * 0.5f + baseTranslation)
                - (recentsView.getScrollX() + viewportCenter);
    }
}
