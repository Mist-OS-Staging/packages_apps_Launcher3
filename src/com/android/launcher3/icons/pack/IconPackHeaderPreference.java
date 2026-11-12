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

import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;
import static com.android.launcher3.util.Executors.THREAD_POOL_EXECUTOR;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.PreferenceViewHolder;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.icons.IconProvider;

import java.util.List;
import java.util.stream.Collectors;

/** Shows a few app icons in the currently selected icon pack. */
public class IconPackHeaderPreference extends RadioHeaderPreference {
    private static final int PREVIEW_ICON_NUM = 8;
    // This value has been selected as an average of usual "device profile-computed" values
    private static final int PREVIEW_ICON_DPI = 500;

    private ImageView[] mIcons = null;
    private int mGeneration;

    public IconPackHeaderPreference(Context context) {
        this(context, null);
    }

    public IconPackHeaderPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                androidx.preference.R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));
    }

    public IconPackHeaderPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutResource(R.layout.preference_widget_icons_preview);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mIcons = new ImageView[] {
            (ImageView) holder.findViewById(R.id.pref_icon_a),
            (ImageView) holder.findViewById(R.id.pref_icon_b),
            (ImageView) holder.findViewById(R.id.pref_icon_c),
            (ImageView) holder.findViewById(R.id.pref_icon_d),
            (ImageView) holder.findViewById(R.id.pref_icon_e),
            (ImageView) holder.findViewById(R.id.pref_icon_f),
            (ImageView) holder.findViewById(R.id.pref_icon_g),
            (ImageView) holder.findViewById(R.id.pref_icon_h)
        };
        onRadioElementSelected(null);
    }

    @Override
    public void onDetached() {
        mIcons = null;
        super.onDetached();
    }

    @Override
    public void onRadioElementSelected(String key) {
        if (mIcons == null) {
            return;
        }
        final int generation = ++mGeneration;
        final Context context = getContext().getApplicationContext();
        // Icon packs are parsed and drawn off the main thread.
        THREAD_POOL_EXECUTOR.execute(() -> {
            IconProvider iconProvider = LauncherAppState.getInstance(context).getIconProvider();
            PackageManager pm = context.getPackageManager();
            List<ActivityInfo> apps = pm.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                    .stream()
                    .sorted(new ResolveInfo.DisplayNameComparator(pm))
                    .limit(PREVIEW_ICON_NUM)
                    .map(ri -> ri.activityInfo)
                    .collect(Collectors.toList());
            Drawable[] drawables = new Drawable[apps.size()];
            for (int i = 0; i < drawables.length; i++) {
                drawables[i] = iconProvider.getIcon(apps.get(i), PREVIEW_ICON_DPI);
            }
            MAIN_EXECUTOR.execute(() -> {
                if (mIcons == null || generation != mGeneration) return;
                for (int i = 0; i < mIcons.length; i++) {
                    if (mIcons[i] != null) {
                        mIcons[i].setImageDrawable(i < drawables.length ? drawables[i] : null);
                    }
                }
            });
        });
    }
}
