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

import static com.android.launcher3.util.Executors.MODEL_EXECUTOR;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.launcher3.customization.IconDatabase;
import com.android.launcher3.util.AppReloader;
import com.android.launcher3.util.ComponentKey;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks the installed icon packs and resolves the icon pack icon of an app. */
public class IconPackManager extends BroadcastReceiver {
    private static final String TAG = "IconPackManager";

    static final String[] ICON_INTENT_ACTIONS = new String[] {
            "com.fede.launcher.THEME_ICONPACK",
            "com.anddoes.launcher.THEME",
            "com.novalauncher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.gau.go.launcherex.theme",
            "org.adw.launcher.THEMES",
            "net.oneplus.launcher.icons.ACTION_PICK_ICON",
            "org.adw.launcher.icons.ACTION_PICK_ICON"
    };

    private static IconPackManager sInstance;

    public static synchronized IconPackManager get(Context context) {
        if (sInstance == null) {
            sInstance = new IconPackManager(context.getApplicationContext());
        }
        return sInstance;
    }

    private final Context mContext;
    private final Map<String, IconPack> mProviders = new ConcurrentHashMap<>();

    /** Reloads icon pack calendars, which pick their icon by the day of the month. */
    private final BroadcastReceiver mDateChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadCalendars();
        }
    };

    private IconPackManager(Context context) {
        mContext = context;
        reloadProviders();

        Handler handler = new Handler(MODEL_EXECUTOR.getLooper());

        IntentFilter filter = new IntentFilter();
        filter.addDataScheme("package");
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED);
        // Called when any app has been installed, enabled, disabled, updated or deleted.
        context.registerReceiver(this, filter, null, handler, Context.RECEIVER_NOT_EXPORTED);

        IntentFilter dateFilter = new IntentFilter(Intent.ACTION_DATE_CHANGED);
        dateFilter.addAction(Intent.ACTION_TIME_CHANGED);
        dateFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        context.registerReceiver(mDateChangeReceiver, dateFilter, null, handler,
                Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getData() == null) return;
        String pkg = intent.getData().getEncodedSchemeSpecificPart();
        if (pkg == null) return;
        Log.d(TAG, "Received intent action " + intent.getAction() + " for " + pkg);
        AppReloader appReloader = AppReloader.get(mContext);

        // Create a list of apps that are using the changed package icon pack,
        // either through the global setting or with an override.
        Set<ComponentKey> updateKeys = appReloader.withIconPack(pkg);

        // Remove the changed package from the providers to reload the application info.
        mProviders.remove(pkg);

        // This can reset the global preference, so do this after creating the list.
        reloadProviders();

        // Ensure all icons are up-to-date after this icon pack change.
        appReloader.reload(updateKeys);
    }

    private void reloadCalendars() {
        if (IconDatabase.getGlobal(mContext).isEmpty() && !hasPerAppOverrides()) return;
        Set<ComponentKey> calendars = new HashSet<>();
        for (ComponentKey key : AppReloader.get(mContext).allKeys()) {
            IconResolver resolver = resolve(key);
            if (resolver != null && resolver.isCalendar()) {
                calendars.add(key);
            }
        }
        AppReloader.get(mContext).reload(calendars);
    }

    private boolean hasPerAppOverrides() {
        for (ComponentKey key : AppReloader.get(mContext).allKeys()) {
            if (!IconDatabase.getByComponent(mContext, key).isEmpty()) return true;
        }
        return false;
    }

    private synchronized void reloadProviders() {
        PackageManager pm = mContext.getPackageManager();
        Set<String> available = new HashSet<>();
        Map<String, ResolveInfo> found = new HashMap<>();
        for (String action : ICON_INTENT_ACTIONS) {
            for (ResolveInfo ri : pm.queryIntentActivities(
                    new Intent(action), PackageManager.GET_META_DATA)) {
                available.add(ri.activityInfo.packageName);
                found.putIfAbsent(ri.activityInfo.packageName, ri);
            }
        }

        // Remove unavailable packs
        mProviders.keySet().retainAll(available);

        // Add new packs
        for (ResolveInfo ri : found.values()) {
            String packageName = ri.activityInfo.packageName;
            if (!mProviders.containsKey(packageName)) {
                ApplicationInfo ai = ri.activityInfo.applicationInfo;
                mProviders.put(packageName, new IconPack(ai, ai.loadLabel(pm)));
            }
        }

        String global = IconDatabase.getGlobal(mContext);
        if (!global.isEmpty() && !mProviders.containsKey(global)) {
            // The global icon pack has been removed, so reset it.
            Log.e(TAG, "Resetting global icon pack because provider " + global + " was removed");
            IconDatabase.resetGlobal(mContext);
        }
    }

    public Map<String, CharSequence> getProviderNames() {
        Map<String, CharSequence> providerTitles = new HashMap<>();
        for (Map.Entry<String, IconPack> pack : mProviders.entrySet()) {
            providerTitles.put(pack.getKey(), pack.getValue().getTitle());
        }
        return providerTitles;
    }

    public boolean packContainsActivity(String packPackage, ComponentName componentName) {
        IconPack pack = mProviders.get(packPackage);
        if (pack == null) return false;
        try {
            IconPack.Data data = pack.getData(mContext.getPackageManager());
            return data.drawables.containsKey(componentName);
        } catch (PackageManager.NameNotFoundException | XmlPullParserException
                | IOException ignored) {
            return false;
        }
    }

    /** Returns every icon of {@code packPackage}, for the custom icon picker. */
    public List<IconPack.IconEntry> getAllIconEntries(String packPackage) {
        IconPack pack = mProviders.get(packPackage);
        if (pack == null) return Collections.emptyList();
        try {
            return pack.getAllIconEntries(mContext.getPackageManager());
        } catch (PackageManager.NameNotFoundException | XmlPullParserException
                | IOException e) {
            Log.e(TAG, "Failed to enumerate icons in pack " + packPackage, e);
            return Collections.emptyList();
        }
    }

    /**
     * Creates a resolver that can load the icon once.
     * This resolver should not be stored, as the resolution strategy could change when
     * the selected icon pack in use is uninstalled or updated.
     * @param key Component for which an icon should be extracted.
     * @return Resolver that loads the icon, or null if there is no resolution strategy.
     */
    @Nullable
    public IconResolver resolve(ComponentKey key) {
        String packPackage = IconDatabase.getByComponent(mContext, key);
        IconPack pack = mProviders.get(packPackage);
        if (pack != null) {
            // The icon provider package is available.
            try {
                PackageManager pm = mContext.getPackageManager();
                IconPack.Data data = pack.getData(pm);
                String explicitDrawable =
                        IconDatabase.getExplicitDrawableForComponent(mContext, key);
                if (explicitDrawable != null) {
                    // An icon picked by the user wins over the appfilter.
                    int drawableId = pack.getResources(pm)
                            .getIdentifier(explicitDrawable, "drawable", packPackage);
                    if (drawableId != 0) {
                        return new IconResolverExternal(pm, pack.getAi(), drawableId,
                                data.calendarPrefix.get(key.componentName),
                                data.clockMetadata.get(drawableId));
                    }
                }
                if (data.drawables.containsKey(key.componentName)) {
                    int drawableId = pack.getDrawableId(pm, key.componentName);
                    if (drawableId != 0) {
                        return new IconResolverExternal(pm, pack.getAi(), drawableId,
                                data.calendarPrefix.get(key.componentName),
                                data.clockMetadata.get(drawableId));
                    }
                }
                if (data.hasMasking()) {
                    return new IconResolverMasked(mContext, data, pack.getAi(), key.hashCode());
                }
            } catch (PackageManager.NameNotFoundException | XmlPullParserException
                    | IOException ignored) {
            }
        } else if (!packPackage.isEmpty()
                && !packPackage.equals(IconDatabase.getGlobal(mContext))) {
            // The per app provider is not available anymore, forget it.
            IconDatabase.resetForComponent(mContext, key);
        }
        return null;
    }
}
