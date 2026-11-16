/*
 * Copyright 2025-2026 AxionOS
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
package com.android.launcher3.allapps;

import static com.android.launcher3.LauncherPrefs.PINNED_APPS;
import static com.android.launcher3.LauncherPrefs.PINNED_APPS_ORDER;

import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.model.data.ItemInfo;
import com.android.launcher3.model.data.WorkspaceItemInfo;
import com.android.launcher3.pm.UserCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PinnedAppsStore {

    private static final String ORDER_SEPARATOR = ",";
    private static final char USER_SEPARATOR = '#';

    private PinnedAppsStore() { }

    public static boolean isPinned(Context context, ItemInfo itemInfo) {
        String key = encode(context, itemInfo);
        return key != null && getPinnedKeys(context).contains(key);
    }

    public static void setPinned(Context context, ItemInfo itemInfo, boolean pinned) {
        String key = encode(context, itemInfo);
        if (key != null) {
            setPinned(context, key, pinned);
        }
    }

    public static void setPinned(Context context, String key, boolean pinned) {
        Set<String> pinnedApps = new LinkedHashSet<>(getPinnedKeys(context));
        if (!(pinned ? pinnedApps.add(key) : pinnedApps.remove(key))) {
            return;
        }
        List<String> order = readOrder(context);
        order.remove(key);
        if (pinned) {
            order.add(key);
        }
        LauncherPrefs.get(context).put(
                PINNED_APPS.to(pinnedApps),
                PINNED_APPS_ORDER.to(TextUtils.join(ORDER_SEPARATOR, order)));
    }

    public static Set<String> getPinnedKeys(Context context) {
        return LauncherPrefs.get(context).get(PINNED_APPS);
    }

    public static List<String> getOrderedKeys(Context context) {
        Set<String> pinnedApps = getPinnedKeys(context);
        List<String> ordered = new ArrayList<>();
        for (String key : readOrder(context)) {
            if (pinnedApps.contains(key) && !ordered.contains(key)) {
                ordered.add(key);
            }
        }
        return ordered;
    }

    public static void setOrder(Context context, List<String> keys) {
        Set<String> pinnedApps = getPinnedKeys(context);
        List<String> order = new ArrayList<>();
        for (String key : keys) {
            if (pinnedApps.contains(key) && !order.contains(key)) {
                order.add(key);
            }
        }
        for (String key : readOrder(context)) {
            if (pinnedApps.contains(key) && !order.contains(key)) {
                order.add(key);
            }
        }
        LauncherPrefs.get(context).put(PINNED_APPS_ORDER,
                TextUtils.join(ORDER_SEPARATOR, order));
    }

    public static List<WorkspaceItemInfo> getPinnedWorkspaceItems(
            Context context, AllAppsStore allAppsStore) {
        List<AppInfo> appInfos = new ArrayList<>();
        Collections.addAll(appInfos, allAppsStore.getApps());
        appInfos = getPinnedApps(context, appInfos);

        List<WorkspaceItemInfo> items = new ArrayList<>(appInfos.size());
        for (AppInfo appInfo : appInfos) {
            items.add(appInfo.makeWorkspaceItem(context));
        }
        return items;
    }

    public static List<AppInfo> getPinnedApps(Context context, List<AppInfo> apps) {
        Set<String> pinnedApps = getPinnedKeys(context);
        if (pinnedApps.isEmpty()) {
            return Collections.emptyList();
        }

        List<AppInfo> appInfos = new ArrayList<>();
        Map<AppInfo, String> keys = new IdentityHashMap<>();
        for (AppInfo appInfo : apps) {
            String key = encode(context, appInfo);
            if (key != null && pinnedApps.contains(key)) {
                appInfos.add(appInfo);
                keys.put(appInfo, key);
            }
        }
        List<String> order = getOrderedKeys(context);
        Comparator<AppInfo> byLabel = new AppInfoComparator(context);
        appInfos.sort((left, right) -> {
            int leftIndex = order.indexOf(keys.get(left));
            int rightIndex = order.indexOf(keys.get(right));
            if (leftIndex < 0 && rightIndex < 0) {
                return byLabel.compare(left, right);
            }
            if (leftIndex < 0) {
                return 1;
            }
            if (rightIndex < 0) {
                return -1;
            }
            return Integer.compare(leftIndex, rightIndex);
        });
        return appInfos;
    }

    @Nullable
    public static String encode(Context context, ItemInfo itemInfo) {
        ComponentName componentName = itemInfo.getTargetComponent();
        if (componentName == null || itemInfo.user == null) {
            return null;
        }
        return componentName.flattenToShortString() + USER_SEPARATOR
                + UserCache.INSTANCE.get(context).getSerialNumberForUser(itemInfo.user);
    }

    @Nullable
    public static ComponentName decodeComponent(String key) {
        int separator = key.lastIndexOf(USER_SEPARATOR);
        return separator <= 0 ? null
                : ComponentName.unflattenFromString(key.substring(0, separator));
    }

    @Nullable
    public static UserHandle decodeUser(Context context, String key) {
        int separator = key.lastIndexOf(USER_SEPARATOR);
        if (separator < 0) {
            return null;
        }
        try {
            long serial = Long.parseLong(key.substring(separator + 1));
            UserManager userManager = context.getSystemService(UserManager.class);
            return userManager == null ? null : userManager.getUserForSerialNumber(serial);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static List<String> readOrder(Context context) {
        List<String> order = new ArrayList<>();
        String value = LauncherPrefs.get(context).get(PINNED_APPS_ORDER);
        if (!TextUtils.isEmpty(value)) {
            for (String key : value.split(ORDER_SEPARATOR)) {
                if (!key.isEmpty()) {
                    order.add(key);
                }
            }
        }
        return order;
    }
}
