package com.android.launcher3.smartspace;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.net.Uri;
import android.os.Process;
import android.provider.CalendarContract;
import android.view.View;
import android.view.View.OnClickListener;

import com.android.launcher3.Launcher;
import com.android.launcher3.Utilities;

public class SmartspaceActionHandler {

    private static final OnClickListener CALENDAR_CLICK = SmartspaceActionHandler::openCalendar;
    private static final OnClickListener WEATHER_CLICK = SmartspaceActionHandler::openWeather;

    private static Launcher launcherFrom(View v) {
        return Launcher.getLauncher(v.getContext());
    }

    private static void openCalendar(View view) {
        Uri.Builder b = CalendarContract.CONTENT_URI.buildUpon().appendPath("time");
        ContentUris.appendId(b, System.currentTimeMillis());
        Uri uri = b.build();
        Intent i = new Intent(Intent.ACTION_VIEW)
                .setData(uri)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            launcherFrom(view).startActivitySafely(view, i, null);
        } catch (ActivityNotFoundException ex) {
            if (!Utilities.isGSAEnabled(view.getContext())) return;
            LauncherApps la = view.getContext().getSystemService(LauncherApps.class);
            if (la != null) {
                la.startAppDetailsActivity(
                        new ComponentName("com.google.android.googlequicksearchbox", ""),
                        Process.myUserHandle(), null, null);
            }
        } catch (SecurityException ignored) { }
    }

    private static void openWeather(View view) {
        if (!Utilities.isGSAEnabled(view.getContext())) return;
        Intent i = new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("dynact://velour/weather/ProxyActivity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                .setComponent(new ComponentName(
                        "com.google.android.googlequicksearchbox",
                        "com.google.android.apps.gsa.velour.DynamicActivityTrampoline"));
        try {
            launcherFrom(view).startActivitySafely(view, i, null);
        } catch (ActivityNotFoundException ex) {
            LauncherApps la = view.getContext().getSystemService(LauncherApps.class);
            if (la != null) {
                la.startAppDetailsActivity(
                        new ComponentName("com.google.android.googlequicksearchbox",
                                "com.google.android.apps.gsa.velour.DynamicActivityTrampoline"),
                        Process.myUserHandle(), null, null);
            }
        } catch (SecurityException ignored) { }
    }

    public static OnClickListener getCalendarAction() { return CALENDAR_CLICK; }
    public static OnClickListener getWeatherAction() { return WEATHER_CLICK; }
}

