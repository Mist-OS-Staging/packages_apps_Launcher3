/*
 * Copyright (C) 2026 Mist OS
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
package com.android.launcher3.quickspace;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.LauncherPrefs;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Font handling for the At A Glance "OOS Clock" style.
 *
 * <p>The clock can use its own bundled font (default), follow the current system font, or use
 * one of the font overlays Mistify's font picker offers. A picked font is applied only to this
 * clock: the overlay is never enabled, only its font family name is read.
 */
public final class QuickspaceClockFonts {

    private static final String TAG = "Launcher3:QuickspaceClockFonts";

    /** Overlay category Mistify's font picker uses for the system font. */
    public static final String FONT_CATEGORY = "android.theme.customization.font";
    /** Picker value for the stock system font (framework value, ignoring any font overlay). */
    public static final String SYSTEM_DEFAULT_PKG = "android";

    private static final String BODY_FONT_FAMILY = "config_bodyFontFamily";

    private QuickspaceClockFonts() { }

    /**
     * Returns the regular-weight typeface the OOS clock should use, or {@code null} to keep the
     * clock's own font. Read on every refresh so it follows preference and system font changes.
     */
    @Nullable
    public static Typeface resolveBaseTypeface(@NonNull Context context) {
        if (LauncherPrefs.QUICKSPACE_CLOCK_SYSTEM_FONT.get(context)) {
            return getSystemFontTypeface(context);
        }
        String pkg = LauncherPrefs.QUICKSPACE_CLOCK_FONT.get(context);
        if (TextUtils.isEmpty(pkg)) {
            return null;
        }
        // A font that is no longer installed falls back to the clock's own font.
        return getTypefaceForPackage(context, pkg);
    }

    /**
     * Current system font. Font overlays only change config_bodyFontFamily (not
     * Typeface.DEFAULT), so read it from this process' resources, which include enabled
     * overlays and are refreshed when the font overlay changes.
     */
    @NonNull
    public static Typeface getSystemFontTypeface(@NonNull Context context) {
        String family = getFontFamily(context.getResources(), SYSTEM_DEFAULT_PKG);
        return TextUtils.isEmpty(family)
                ? Typeface.DEFAULT
                : Typeface.create(family, Typeface.NORMAL);
    }

    /** Typeface for a font overlay package (or {@link #SYSTEM_DEFAULT_PKG}), null if missing. */
    @Nullable
    public static Typeface getTypefaceForPackage(@NonNull Context context, @NonNull String pkg) {
        String family = getFontFamily(context, pkg);
        return TextUtils.isEmpty(family) ? null : Typeface.create(family, Typeface.NORMAL);
    }

    @Nullable
    private static String getFontFamily(@NonNull Context context, @NonNull String pkg) {
        try {
            Resources res = SYSTEM_DEFAULT_PKG.equals(pkg)
                    ? Resources.getSystem()
                    : context.getPackageManager().getResourcesForApplication(pkg);
            return getFontFamily(res, pkg);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Font package not found: " + pkg);
            return null;
        }
    }

    @Nullable
    private static String getFontFamily(@NonNull Resources res, @NonNull String pkg) {
        try {
            int id = res.getIdentifier(BODY_FONT_FAMILY, "string", pkg);
            return id != 0 ? res.getString(id) : null;
        } catch (Resources.NotFoundException e) {
            Log.w(TAG, "No font family in " + pkg, e);
            return null;
        }
    }

    /**
     * Font choices, matching Mistify's font picker: the stock system font first, then every
     * installed font overlay targeting android, sorted by label.
     */
    @NonNull
    public static List<String> getFontPackages(@NonNull Context context) {
        PackageManager pm = context.getPackageManager();
        Map<String, String> labels = new HashMap<>();
        for (PackageInfo pi : pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))) {
            if (SYSTEM_DEFAULT_PKG.equals(pi.overlayTarget)
                    && FONT_CATEGORY.equals(pi.overlayCategory)) {
                labels.put(pi.packageName, getLabel(context, pi.packageName).toString());
            }
        }
        List<String> overlays = new ArrayList<>(labels.keySet());
        Collator collator = Collator.getInstance();
        overlays.sort((a, b) -> collator.compare(labels.get(a), labels.get(b)));
        List<String> result = new ArrayList<>(overlays.size() + 1);
        result.add(SYSTEM_DEFAULT_PKG);
        result.addAll(overlays);
        return result;
    }

    /** Label of a font overlay package; the package name if it cannot be loaded. */
    @NonNull
    public static CharSequence getLabel(@NonNull Context context, @NonNull String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.getApplicationInfo(pkg, 0).loadLabel(pm);
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }
}
