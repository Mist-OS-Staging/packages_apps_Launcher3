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

import androidx.annotation.WorkerThread;

import com.android.launcher3.R;
import com.android.launcher3.icons.pack.IconPackManager;
import com.android.launcher3.util.AppReloader;
import com.android.launcher3.util.ComponentKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The icon packs that can provide the icon of one app, for the per app icon pack picker. */
final class IconPackChoices {
    final CharSequence[] labels;
    final String[] values;
    final int checkedIndex;

    private IconPackChoices(CharSequence[] labels, String[] values, int checkedIndex) {
        this.labels = labels;
        this.values = values;
        this.checkedIndex = checkedIndex;
    }

    /** Lists the icon packs with an icon for {@code key}, after a "Default" entry. */
    @WorkerThread
    static IconPackChoices load(Context context, ComponentKey key) {
        IconPackManager ipm = IconPackManager.get(context);
        Map<String, CharSequence> packs = ipm.getProviderNames();
        packs.keySet().removeIf(pkg -> !ipm.packContainsActivity(pkg, key.componentName));
        String globalPack = IconDatabase.getGlobal(context);

        List<Map.Entry<String, CharSequence>> sorted = new ArrayList<>(packs.entrySet());
        sorted.sort((a, b) -> a.getValue().toString().compareToIgnoreCase(
                b.getValue().toString()));

        CharSequence[] labels = new CharSequence[sorted.size() + 1];
        String[] values = new String[labels.length];
        // "Default" follows the global icon pack, unless that pack has an icon for this app: then
        // it explicitly selects the system icon.
        labels[0] = context.getString(R.string.icon_pack_default_label);
        values[0] = packs.containsKey(globalPack) ? IconDatabase.VALUE_DEFAULT : globalPack;
        for (int i = 0; i < sorted.size(); i++) {
            labels[i + 1] = sorted.get(i).getValue();
            values[i + 1] = sorted.get(i).getKey();
        }

        String current = IconDatabase.getByComponent(context, key);
        int checked = 0;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                checked = i;
                break;
            }
        }
        return new IconPackChoices(labels, values, checked);
    }

    /** Applies the choice at {@code index} to {@code key} and reloads its icon. */
    void apply(Context context, ComponentKey key, int index) {
        String value = values[index];
        if (value.equals(IconDatabase.getGlobal(context))) {
            IconDatabase.resetForComponent(context, key);
        } else {
            IconDatabase.setForComponent(context, key, value);
        }
        AppReloader.get(context).reload(key);
    }

    /** Returns the label of the icon pack currently used for {@code key}. */
    @WorkerThread
    static CharSequence currentLabel(Context context, ComponentKey key) {
        String pkg = IconDatabase.getByComponent(context, key);
        CharSequence label = pkg.isEmpty()
                ? null : IconPackManager.get(context).getProviderNames().get(pkg);
        return label != null ? label : context.getString(R.string.icon_pack_default_label);
    }
}
