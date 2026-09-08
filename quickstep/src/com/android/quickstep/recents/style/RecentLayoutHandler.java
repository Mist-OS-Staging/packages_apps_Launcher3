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
