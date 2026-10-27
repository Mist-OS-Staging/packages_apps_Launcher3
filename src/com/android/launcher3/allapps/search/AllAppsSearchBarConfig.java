/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.launcher3.allapps.search;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.LauncherPrefs;

/**
 * Type-safe configuration and helper for All Apps Search Bar placement and appearance customization.
 */
public final class AllAppsSearchBarConfig {

    public enum Placement {
        TOP("top"),
        BOTTOM("bottom");

        public final String key;

        Placement(String key) {
            this.key = key;
        }

        @NonNull
        public static Placement fromString(@Nullable String value) {
            if ("bottom".equalsIgnoreCase(value)) {
                return BOTTOM;
            }
            return TOP;
        }
    }

    public enum Appearance {
        DEFAULT("default"),
        BLUR("blur"),
        NARROW("narrow"),
        NARROW_BLUR("narrow_blur");

        public final String key;

        Appearance(String key) {
            this.key = key;
        }

        @NonNull
        public static Appearance fromString(@Nullable String value) {
            if ("blur".equalsIgnoreCase(value)) {
                return BLUR;
            } else if ("narrow".equalsIgnoreCase(value)) {
                return NARROW;
            } else if ("narrow_blur".equalsIgnoreCase(value)) {
                return NARROW_BLUR;
            }
            return DEFAULT;
        }

        public boolean isBlur() {
            return this == BLUR || this == NARROW_BLUR;
        }

        public boolean isNarrow() {
            return this == NARROW || this == NARROW_BLUR;
        }
    }

    private AllAppsSearchBarConfig() {}

    @NonNull
    public static Placement getPlacement(@NonNull Context context) {
        String val = LauncherPrefs.get(context).get(LauncherPrefs.ALL_APPS_SEARCH_BAR_PLACEMENT);
        return Placement.fromString(val);
    }

    @NonNull
    public static Appearance getAppearance(@NonNull Context context) {
        String val = LauncherPrefs.get(context).get(LauncherPrefs.ALL_APPS_SEARCH_BAR_APPEARANCE);
        return Appearance.fromString(val);
    }
}

