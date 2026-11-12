/*
 * Copyright (C) 2020 Shade Launcher
 * Copyright (C) 2024-2026 Lunaris AOSP
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

package com.android.launcher3.icons.pack;

import static com.android.launcher3.icons.BaseIconFactory.CONFIG_HINT_NO_WRAP;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ComponentInfo;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import com.android.launcher3.icons.clock.CustomClock;
import com.android.launcher3.util.ComponentKey;

/** Loads app icons from the icon pack selected for them. */
public final class IconPackIcons {

    private IconPackIcons() { }

    /**
     * Returns the icon pack icon of {@code info}, or {@code fallback} when no icon pack provides
     * one. The system clock and calendar keep their live icons unless the icon pack replaces them.
     */
    @SuppressLint("WrongConstant")
    public static Drawable getIcon(Context context, ComponentInfo info, int iconDpi,
            IconResolver.DefaultDrawableProvider fallback) {
        ComponentKey key = new ComponentKey(info.getComponentName(),
                UserHandle.getUserHandleForUid(info.applicationInfo.uid));
        IconResolver resolver = IconPackManager.get(context).resolve(key);
        Drawable icon = resolver == null ? null : resolver.getIcon(iconDpi, fallback);
        if (icon != null && resolver.isClock()) {
            Drawable clock = CustomClock.getClock(icon, resolver.clockData());
            if (clock != null) icon = clock;
        }
        if (icon == null) {
            return fallback.get();
        }
        if (!(icon instanceof AdaptiveIconDrawable)) {
            // Icon pack icons bring their own shape, do not wrap them in the theme shape.
            icon.setChangingConfigurations(icon.getChangingConfigurations() | CONFIG_HINT_NO_WRAP);
        }
        return icon;
    }
}
