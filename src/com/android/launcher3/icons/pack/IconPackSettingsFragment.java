/*
 * Copyright (C) 2020 Shift GmbH
 * Copyright (C) 2026 crDroid Android Project
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

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.launcher3.R;
import com.android.launcher3.customization.IconDatabase;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lets the user pick the global icon pack, with a live preview of a few apps. */
public final class IconPackSettingsFragment extends RadioSettingsFragment {
    private static final String KEY_INSTALL_MORE = "pref_icon_pack_install_more";

    private static final IntentFilter PKG_UPDATE_INTENT = new IntentFilter();

    static {
        PKG_UPDATE_INTENT.addAction(Intent.ACTION_PACKAGE_ADDED);
        PKG_UPDATE_INTENT.addAction(Intent.ACTION_PACKAGE_CHANGED);
        PKG_UPDATE_INTENT.addAction(Intent.ACTION_PACKAGE_REMOVED);
        PKG_UPDATE_INTENT.addDataScheme("package");
    }

    private final BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadPreferences();
        }
    };

    @Override
    public void onCreatePreferences(android.os.Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        PreferenceScreen screen = getPreferenceScreen();
        Preference installMore = new Preference(screen.getContext());
        installMore.setKey(KEY_INSTALL_MORE);
        installMore.setTitle(R.string.icon_pack_add);
        installMore.setIcon(R.drawable.ic_icon_pack_plus);
        installMore.setPersistent(false);
        installMore.setOrder(Integer.MAX_VALUE);
        installMore.setOnPreferenceClickListener(p -> {
            openStore(p.getContext());
            return true;
        });
        screen.addPreference(installMore);
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadPreferences();
        requireContext().registerReceiver(mPackageReceiver, PKG_UPDATE_INTENT,
                Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(mPackageReceiver);
    }

    @Override
    protected List<SelectorWithWidgetPreference> getPreferences(Context context) {
        final String currentIconPack = IconDatabase.getGlobal(context);
        final List<SelectorWithWidgetPreference> prefsList = new ArrayList<>();
        for (Map.Entry<String, String> entry : getAvailableIconPacks(context).entrySet()) {
            final boolean isCurrent = currentIconPack.equals(entry.getKey());
            final SelectorWithWidgetPreference pref = buildPreference(context,
                    entry.getKey(), entry.getValue(), isCurrent);
            prefsList.add(pref);
            if (isCurrent) {
                setSelectedPreference(pref);
            }
        }
        return prefsList;
    }

    @Override
    public void onSelected(String key) {
        IconDatabase.setGlobal(requireContext(), key);
        super.onSelected(key);
    }

    @Override
    protected RadioHeaderPreference getHeader(Context context) {
        return new IconPackHeaderPreference(context);
    }

    private static Map<String, String> getAvailableIconPacks(Context context) {
        final PackageManager pm = context.getPackageManager();
        final Map<String, String> availablePacks = new LinkedHashMap<>();
        availablePacks.put(IconDatabase.VALUE_DEFAULT,
                context.getString(R.string.icon_pack_default_label));
        for (String action : IconPackManager.ICON_INTENT_ACTIONS) {
            for (ResolveInfo r : pm.queryIntentActivities(
                    new Intent(action), PackageManager.GET_META_DATA)) {
                availablePacks.putIfAbsent(r.activityInfo.packageName,
                        r.loadLabel(pm).toString());
            }
        }
        return availablePacks;
    }

    private static SelectorWithWidgetPreference buildPreference(Context context, String pkgName,
            String label, boolean isChecked) {
        final SelectorWithWidgetPreference pref = new SelectorWithWidgetPreference(context);
        pref.setKey(pkgName);
        pref.setTitle(label);
        pref.setPersistent(false);
        pref.setChecked(isChecked);
        if (!pkgName.equals(IconDatabase.VALUE_DEFAULT)) {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkgName);
            if (intent != null) {
                // Opens the icon pack app, e.g. to request missing icons.
                pref.setExtraWidgetOnClickListener(v -> context.startActivity(intent));
            }
        }
        return pref;
    }

    private static void openStore(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://search?c=apps&q=icon%20pack"));
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.icon_pack_no_market, Toast.LENGTH_SHORT).show();
        }
    }
}
