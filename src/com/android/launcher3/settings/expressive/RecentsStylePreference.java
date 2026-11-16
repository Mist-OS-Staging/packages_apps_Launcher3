/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings.expressive;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;

public class RecentsStylePreference extends Preference {

    private final CharSequence[] mEntries;
    private final CharSequence[] mValues;
    private String mValue = RecentsStylePreviewView.STYLE_DEFAULT;

    public RecentsStylePreference(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.settings_recents_style_preference);
        setSelectable(false);
        mEntries = context.getResources().getTextArray(R.array.recents_style_entries);
        mValues = context.getResources().getTextArray(R.array.recents_style_values);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(@Nullable Object defaultValue) {
        String fallback = defaultValue instanceof String
                ? (String) defaultValue : RecentsStylePreviewView.STYLE_DEFAULT;
        mValue = getPersistedString(fallback);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        LinearLayout options = (LinearLayout) holder.findViewById(R.id.recents_style_options);
        options.removeAllViews();
        boolean showClearAll = LauncherPrefs.get(getContext()).get(LauncherPrefs.RECENTS_CLEAR_ALL);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < mValues.length; i++) {
            String value = mValues[i].toString();
            View option = inflater.inflate(R.layout.settings_recents_style_option, options, false);
            RecentsStylePreviewView preview = option.findViewById(R.id.recents_style_preview);
            preview.setStyle(value);
            preview.setShowClearAll(showClearAll);
            boolean chosen = value.equals(mValue);
            preview.setChosen(chosen, false);
            preview.setLoopDelay(450L + i * 220L);
            TextView label = option.findViewById(R.id.recents_style_label);
            label.setText(mEntries[i]);
            styleLabel(label, chosen);
            option.setTag(value);
            option.setContentDescription(mEntries[i]);
            option.setOnClickListener(v -> select(value, options));
            ExpressiveMotion.attachPress(option, null, 0, 0, 0.95f, null, 0);
            options.addView(option);
        }
    }

    private void select(String value, LinearLayout options) {
        if (!value.equals(mValue)) {
            if (!callChangeListener(value)) {
                return;
            }
            mValue = value;
            persistString(value);
        }
        for (int i = 0; i < options.getChildCount(); i++) {
            View option = options.getChildAt(i);
            boolean chosen = value.equals(option.getTag());
            RecentsStylePreviewView preview = option.findViewById(R.id.recents_style_preview);
            preview.setChosen(chosen, true);
            styleLabel(option.findViewById(R.id.recents_style_label), chosen);
            if (chosen) {
                preview.playDemo();
            }
        }
    }

    private void styleLabel(TextView label, boolean chosen) {
        Context context = getContext();
        if (chosen) {
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(context.getColor(R.color.settings_expressive_primary));
            pill.setCornerRadius(context.getResources().getDisplayMetrics().density * 15);
            label.setBackground(pill);
            int onPrimary = context.getColor(R.color.settings_expressive_on_primary);
            label.setTextColor(onPrimary);
            Drawable check = context.getDrawable(R.drawable.ic_settings_expressive_check);
            if (check != null) {
                check.mutate().setTint(onPrimary);
            }
            label.setCompoundDrawablesRelativeWithIntrinsicBounds(check, null, null, null);
        } else {
            label.setBackground(null);
            label.setTextColor(context.getColor(R.color.settings_expressive_on_surface));
            label.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
        }
    }
}
