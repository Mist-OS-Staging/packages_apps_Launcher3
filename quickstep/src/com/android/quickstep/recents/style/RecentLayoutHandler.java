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

public interface RecentLayoutHandler {

    @NonNull
    RecentStyle getStyle();

    default void onAttached(@NonNull RecentsView<?, ?> recentsView) {}

    default void onDetached(@NonNull RecentsView<?, ?> recentsView) {}

    void calculateTransform(
            @NonNull RecentsView<?, ?> recentsView,
            @NonNull TaskView taskView,
            int taskIndex,
            int taskCount,
            float scrollProgress,
            @NonNull StyleTransform outTransform);

    default void onTaskDismissed(@NonNull RecentsView<?, ?> recentsView, @NonNull TaskView taskView) {}

    default void onPostUpdateCurveProperties(@NonNull RecentsView<?, ?> recentsView) {}

    /**
     * Whether this layout derives its transforms from the cards' live positions, including the
     * translations driven by swipe-to-dismiss and the neighbour reflow that follows it.
     *
     * When true, {@link TaskView}'s dismiss translation is rendered (so neighbours visibly slide
     * into the gap) and the style transforms are re-evaluated on every dismiss frame.
     */
    default boolean followsDismissProgress() {
        return false;
    }

    /**
     * Alpha multiplier of the card being swiped away.
     *
     * @param dismissProgress 0 at rest, 1 once the card has travelled half its height.
     */
    default float getDismissedTaskAlpha(float dismissProgress) {
        return Math.max(0f, 1f - dismissProgress);
    }

    default void resetTaskTransforms(@Nullable TaskView taskView) {
        if (taskView != null) {
            taskView.resetCustomStyleTransforms();
        }
    }

    default boolean isVerticalScrollStyle() {
        return false;
    }

    @Nullable
    default Integer computeMinScroll(@NonNull RecentsView<?, ?> recentsView) {
        return null;
    }

    @Nullable
    default Integer computeMaxScroll(@NonNull RecentsView<?, ?> recentsView) {
        return null;
    }

    default boolean getPageScrolls(@NonNull RecentsView<?, ?> recentsView, int[] outPageScrolls) {
        return false;
    }

    @Nullable
    default Boolean isTaskViewVisible(@NonNull RecentsView<?, ?> recentsView, @NonNull TaskView taskView) {
        return null;
    }

    @Nullable
    default Integer getDestinationPage(@NonNull RecentsView<?, ?> recentsView, int scaledScroll) {
        return null;
    }

    @Nullable
    default Integer snapToPageWithVelocity(@NonNull RecentsView<?, ?> recentsView, int whichPage, int velocity) {
        return null;
    }
}
