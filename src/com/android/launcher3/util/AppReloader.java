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

package com.android.launcher3.util;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.customization.IconDatabase;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Reloads the icons of apps after their icon pack changed. */
public class AppReloader {
    private static AppReloader sInstance;

    public static synchronized AppReloader get(Context context) {
        if (sInstance == null) {
            sInstance = new AppReloader(context.getApplicationContext());
        }
        return sInstance;
    }

    private final Context mContext;
    private final UserManager mUsers;
    private final LauncherApps mApps;

    private AppReloader(Context context) {
        mContext = context;
        mUsers = context.getSystemService(UserManager.class);
        mApps = context.getSystemService(LauncherApps.class);
    }

    /** Returns all launchable activities of all profiles. */
    public Set<ComponentKey> allKeys() {
        Set<ComponentKey> keys = new HashSet<>();
        for (UserHandle user : mUsers.getUserProfiles()) {
            for (LauncherActivityInfo info : mApps.getActivityList(null, user)) {
                keys.add(new ComponentKey(info.getComponentName(), info.getUser()));
            }
        }
        return keys;
    }

    /** Returns the activities that currently take their icon from {@code iconPack}. */
    public Set<ComponentKey> withIconPack(String iconPack) {
        Set<ComponentKey> reloadKeys = new HashSet<>();
        for (ComponentKey key : allKeys()) {
            if (IconDatabase.getByComponent(mContext, key).equals(iconPack)) {
                reloadKeys.add(key);
            }
        }
        return reloadKeys;
    }

    public void reload(ComponentKey key) {
        reload(key.user, key.componentName.getPackageName());
    }

    public void reload(Collection<ComponentKey> keys) {
        Set<String> reloaded = new HashSet<>();
        for (ComponentKey key : keys) {
            // Reloading a package refreshes all of its activities, do it once per package.
            if (reloaded.add(key.user + "/" + key.componentName.getPackageName())) {
                reload(key);
            }
        }
    }

    private void reload(UserHandle user, String pkg) {
        LauncherAppState.getInstance(mContext).getModel().newModelCallbacks()
                .onPackageChanged(pkg, user);
    }
}
