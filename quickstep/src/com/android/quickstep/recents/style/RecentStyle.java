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

public enum RecentStyle {
    DEFAULT("default"),
    ONE_UI_GRID("oneui_grid"),
    ONE_UI_STACK("oneui_stack"),
    IOS("ios"),
    MIUI_HORIZONTAL("miui_horizontal");

    private final String mKey;

    RecentStyle(String key) {
        mKey = key;
    }

    public String getKey() {
        return mKey;
    }

    @NonNull
    public static RecentStyle fromKey(String key) {
        if (key != null) {
            if ("oneui_list".equalsIgnoreCase(key) || "one_ui_list".equalsIgnoreCase(key)) {
                return DEFAULT;
            }
            for (RecentStyle style : values()) {
                if (style.mKey.equalsIgnoreCase(key) || style.name().equalsIgnoreCase(key)) {
                    return style;
                }
            }
        }
        return DEFAULT;
    }
}
