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
package com.android.launcher3.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.preference.Preference;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.quickspace.QuickspaceClockFonts;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks a font for the At A Glance "OOS Clock" only, from the same font overlays Mistify's font
 * picker offers. Stores the overlay package name; the overlay itself is never enabled, so the
 * system font and other clocks are untouched. Empty value = the clock's own font.
 */
public class QuickspaceClockFontPreference extends Preference {

    private AlertDialog mDialog;

    public QuickspaceClockFontPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public QuickspaceClockFontPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public QuickspaceClockFontPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public QuickspaceClockFontPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        updateSummary();
    }

    @Override
    public void onDetached() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        super.onDetached();
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        updateSummary();
    }

    private String getValue() {
        return getPersistedString("");
    }

    void updateSummary() {
        if (LauncherPrefs.QUICKSPACE_CLOCK_SYSTEM_FONT.get(getContext())) {
            setSummary(R.string.quickspace_clock_font_disabled_summary);
        } else {
            setSummary(getLabelFor(getContext(), getValue()));
        }
    }

    private static CharSequence getLabelFor(Context context, String pkg) {
        if (TextUtils.isEmpty(pkg)) {
            return context.getString(R.string.quickspace_clock_font_default);
        }
        if (QuickspaceClockFonts.SYSTEM_DEFAULT_PKG.equals(pkg)) {
            return context.getString(R.string.quickspace_clock_font_system_default);
        }
        return QuickspaceClockFonts.getLabel(context, pkg);
    }

    @Override
    protected void onClick() {
        final Context context = getContext();
        final List<String> values = new ArrayList<>();
        values.add("");
        values.addAll(QuickspaceClockFonts.getFontPackages(context));

        final List<CharSequence> labels = new ArrayList<>(values.size());
        for (String pkg : values) {
            labels.add(getLabelFor(context, pkg));
        }

        String current = getValue();
        int checked = values.indexOf(current);
        if (checked < 0) checked = 0;

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context,
                android.R.layout.simple_list_item_single_choice, labels) {
            private final Typeface[] mTypefaces = new Typeface[values.size()];
            private final boolean[] mResolved = new boolean[values.size()];

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                // Show each choice in its own font; the first entry keeps the dialog font.
                tv.setTypeface(position == 0 ? Typeface.DEFAULT : typefaceAt(position));
                return tv;
            }

            private Typeface typefaceAt(int position) {
                if (!mResolved[position]) {
                    mTypefaces[position] = QuickspaceClockFonts.getTypefaceForPackage(
                            context, values.get(position));
                    mResolved[position] = true;
                }
                return mTypefaces[position] != null ? mTypefaces[position] : Typeface.DEFAULT;
            }
        };

        mDialog = new AlertDialog.Builder(context)
                .setTitle(getTitle())
                .setSingleChoiceItems(adapter, checked, (dialog, which) -> {
                    String value = values.get(which);
                    if (callChangeListener(value)) {
                        persistString(value);
                        updateSummary();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        mDialog.setOnDismissListener(d -> mDialog = null);
        mDialog.show();
    }
}
