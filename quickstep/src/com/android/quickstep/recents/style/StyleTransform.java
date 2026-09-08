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

import com.android.quickstep.views.TaskView;

public class StyleTransform {

    public float translationX;
    public float translationY;
    public float scale;
    public float rotation;
    public float elevation;
    public float alpha;

    public StyleTransform() {
        reset();
    }

    public void reset() {
        translationX = 0f;
        translationY = 0f;
        scale = 1f;
        rotation = 0f;
        elevation = 0f;
        alpha = 1f;
    }

    public void set(@NonNull StyleTransform other) {
        this.translationX = other.translationX;
        this.translationY = other.translationY;
        this.scale = other.scale;
        this.rotation = other.rotation;
        this.elevation = other.elevation;
        this.alpha = other.alpha;
    }

    public void applyTo(@Nullable TaskView taskView) {
        if (taskView == null) {
            return;
        }
        taskView.setCustomStyleTranslationX(translationX);
        taskView.setCustomStyleTranslationY(translationY);
        taskView.setCustomStyleScale(scale);
        taskView.setRotation(rotation);
        taskView.setElevation(elevation);
        taskView.setCustomStyleAlpha(alpha);
    }
}
