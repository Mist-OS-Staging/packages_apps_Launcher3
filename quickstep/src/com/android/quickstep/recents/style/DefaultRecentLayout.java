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

public class DefaultRecentLayout implements RecentLayoutHandler {

    @NonNull
    @Override
    public RecentStyle getStyle() {
        return RecentStyle.DEFAULT;
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
    }
}
