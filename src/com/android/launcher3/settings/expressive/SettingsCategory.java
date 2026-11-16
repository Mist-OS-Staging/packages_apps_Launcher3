/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings.expressive;

import android.app.Activity;

import com.android.launcher3.R;
import com.android.launcher3.settings.SettingsAppDrawer;
import com.android.launcher3.settings.SettingsHomescreen;
import com.android.launcher3.settings.SettingsIcons;
import com.android.launcher3.settings.SettingsMisc;
import com.android.launcher3.settings.SettingsRecents;

public enum SettingsCategory {
    ICONS(R.string.icons_category_title, R.string.settings_tile_icons_summary,
            R.color.homepage_blue_bg, R.color.homepage_blue_fg,
            R.drawable.ic_settings_icons_expressive, ExpressiveShape.COOKIE, SettingsIcons.class),
    HOME(R.string.home_category_title, R.string.settings_tile_home_summary,
            R.color.homepage_orange_bg, R.color.homepage_orange_fg,
            R.drawable.ic_settings_homescreen_expressive, ExpressiveShape.CLOVER,
            SettingsHomescreen.class),
    APP_DRAWER(R.string.app_drawer_category_title, R.string.settings_tile_app_drawer_summary,
            R.color.homepage_yellow_bg, R.color.homepage_yellow_fg,
            R.drawable.ic_settings_appdrawer_expressive, ExpressiveShape.BURST,
            SettingsAppDrawer.class),
    RECENTS(R.string.recents_category_title, R.string.settings_tile_recents_summary,
            R.color.homepage_red_bg, R.color.homepage_red_fg,
            R.drawable.ic_settings_recents_expressive, ExpressiveShape.PENTAGON,
            SettingsRecents.class),
    MISC(R.string.misc_category_title, R.string.settings_tile_misc_summary,
            R.color.homepage_cyan_bg, R.color.homepage_cyan_fg,
            R.drawable.ic_settings_misc_expressive, ExpressiveShape.FLOWER, SettingsMisc.class);

    public final int titleRes;
    public final int summaryRes;
    public final int containerColorRes;
    public final int onContainerColorRes;
    public final int glyphRes;
    public final ExpressiveShape shape;
    public final Class<? extends Activity> activity;

    SettingsCategory(int titleRes, int summaryRes, int containerColorRes,
            int onContainerColorRes, int glyphRes, ExpressiveShape shape,
            Class<? extends Activity> activity) {
        this.titleRes = titleRes;
        this.summaryRes = summaryRes;
        this.containerColorRes = containerColorRes;
        this.onContainerColorRes = onContainerColorRes;
        this.glyphRes = glyphRes;
        this.shape = shape;
        this.activity = activity;
    }
}
