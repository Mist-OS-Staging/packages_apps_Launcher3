package com.android.launcher3.applibrary

import android.content.Context
import android.content.SharedPreferences
import com.android.launcher3.LauncherPrefs

object AppLibraryPrefs {
    private const val PREF_APP_LIBRARY_ENABLED = "app_library_enabled"
    
    fun isAppLibraryEnabled(context: Context): Boolean {
        return LauncherPrefs.getPrefs(context).getBoolean(PREF_APP_LIBRARY_ENABLED, false)
    }
    
    fun setAppLibraryEnabled(context: Context, enabled: Boolean) {
        LauncherPrefs.getPrefs(context).edit()
            .putBoolean(PREF_APP_LIBRARY_ENABLED, enabled)
            .apply()
    }
}
