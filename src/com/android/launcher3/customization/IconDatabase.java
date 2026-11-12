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

package com.android.launcher3.customization;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.util.ComponentKey;

/** Stores the global icon pack and the per app icon pack overrides. */
public class IconDatabase {

    private static final String PREF_FILE_SUFFIX = ".ICON_DATABASE";
    public static final String KEY_ICON_PACK = "pref_icon_pack";
    public static final String VALUE_DEFAULT = "";
    /** Separates the icon pack from the drawable of an explicitly picked icon. */
    private static final String DRAWABLE_SEPARATOR = "::";

    public static String getGlobal(Context context) {
        return LauncherPrefs.get(context).get(LauncherPrefs.ICON_PACK);
    }

    public static String getGlobalLabel(Context context) {
        final String defaultLabel = context.getString(R.string.icon_pack_default_label);
        final String pkgName = getGlobal(context);
        if (VALUE_DEFAULT.equals(pkgName)) {
            return defaultLabel;
        }

        final PackageManager pm = context.getPackageManager();
        try {
            final ApplicationInfo ai = pm.getApplicationInfo(pkgName, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return defaultLabel;
        }
    }

    public static void setGlobal(Context context, String value) {
        LauncherPrefs.get(context).put(LauncherPrefs.ICON_PACK, value);
    }

    public static void resetGlobal(Context context) {
        LauncherPrefs.get(context).remove(LauncherPrefs.ICON_PACK);
    }

    public static String getByComponent(Context context, ComponentKey key) {
        String raw = getIconPackPrefs(context).getString(key.toString(), getGlobal(context));
        int idx = raw.indexOf(DRAWABLE_SEPARATOR);
        return idx == -1 ? raw : raw.substring(0, idx);
    }

    public static void setForComponent(Context context, ComponentKey key, String value) {
        getIconPackPrefs(context).edit().putString(key.toString(), value).apply();
    }

    /** Uses the drawable {@code drawableName} of {@code packPackage} as the icon of {@code key}. */
    public static void setExplicitIconForComponent(
            Context context, ComponentKey key, String packPackage, String drawableName) {
        getIconPackPrefs(context).edit()
                .putString(key.toString(), packPackage + DRAWABLE_SEPARATOR + drawableName)
                .apply();
    }

    /** Returns the explicitly picked drawable for {@code key}, if any. */
    public static String getExplicitDrawableForComponent(Context context, ComponentKey key) {
        String raw = getIconPackPrefs(context).getString(key.toString(), null);
        if (raw == null) return null;
        int idx = raw.indexOf(DRAWABLE_SEPARATOR);
        return idx == -1 ? null : raw.substring(idx + DRAWABLE_SEPARATOR.length());
    }

    public static void resetForComponent(Context context, ComponentKey key) {
        getIconPackPrefs(context).edit().remove(key.toString()).apply();
    }

    private static SharedPreferences getIconPackPrefs(Context context) {
        return context.getSharedPreferences(
                context.getPackageName() + PREF_FILE_SUFFIX, Context.MODE_PRIVATE);
    }
}
