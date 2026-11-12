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
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.quickspace.QuickspaceClockFonts;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks a font for the At A Glance "OOS Clock" only. Same font list and look as Mistify's font
 * picker (FontsPickerPreference): every installed font overlay, each shown in its own font, the
 * selected one outlined in the accent color. Stores the overlay package name; the overlay is
 * never enabled, so the system font and other clocks are untouched. Empty = the clock's own font.
 */
public class QuickspaceClockFontPreference extends Preference {

    private static final int DIALOG_MAX_WIDTH_DP = 360;
    private static final int DIALOG_MAX_WIDTH_DP_LANDSCAPE = 640;

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

        boolean landscape = context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        View content = LayoutInflater.from(context).inflate(R.layout.quickspace_font_picker, null);
        RecyclerView list = content.findViewById(R.id.quickspace_font_list);
        list.setLayoutManager(new GridLayoutManager(context, landscape ? 2 : 1));
        list.setAdapter(new FontsAdapter(context, values, getValue()));
        list.post(() -> {
            float fraction = landscape ? 0.75f : 0.6f;
            ViewGroup.LayoutParams lp = list.getLayoutParams();
            lp.height = (int) (context.getResources().getDisplayMetrics().heightPixels * fraction);
            list.setLayoutParams(lp);
        });

        mDialog = new AlertDialog.Builder(context)
                .setTitle(getTitle())
                .setView(content)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        mDialog.setOnDismissListener(d -> mDialog = null);
        mDialog.show();
        applyDialogWidth(mDialog, landscape);
        tintDialogAccent(mDialog);
    }

    private void onFontPicked(String value) {
        if (!value.equals(getValue()) && callChangeListener(value)) {
            persistString(value);
            updateSummary();
        }
        if (mDialog != null) mDialog.dismiss();
    }

    private void applyDialogWidth(AlertDialog dialog, boolean landscape) {
        Window w = dialog.getWindow();
        if (w == null) return;
        Context context = getContext();
        int boundsWidth;
        try {
            boundsWidth = context.getSystemService(WindowManager.class)
                    .getCurrentWindowMetrics().getBounds().width();
        } catch (RuntimeException e) {
            boundsWidth = context.getResources().getDisplayMetrics().widthPixels;
        }
        float density = context.getResources().getDisplayMetrics().density;
        int maxWidth = (int) ((landscape ? DIALOG_MAX_WIDTH_DP_LANDSCAPE : DIALOG_MAX_WIDTH_DP)
                * density + 0.5f);
        w.setLayout(Math.min(maxWidth, (int) (boundsWidth * 0.9f)),
                WindowManager.LayoutParams.WRAP_CONTENT);
    }

    private void tintDialogAccent(AlertDialog dialog) {
        int accent = resolveColor(getContext(), android.R.attr.colorAccent, 0);
        if (accent == 0) return;
        Window w = dialog.getWindow();
        if (w != null) {
            int titleId = getContext().getResources().getIdentifier("alertTitle", "id", "android");
            TextView title = titleId != 0 ? w.getDecorView().findViewById(titleId) : null;
            if (title != null) title.setTextColor(accent);
        }
        Button cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (cancel != null) cancel.setTextColor(accent);
    }

    private static int resolveColor(Context context, int attr, int fallback) {
        TypedValue tv = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, tv, true)) return fallback;
        return tv.resourceId != 0 ? context.getColor(tv.resourceId) : tv.data;
    }

    private class FontsAdapter extends RecyclerView.Adapter<FontsAdapter.Holder> {
        private final Context mContext;
        private final List<String> mValues;
        private final Typeface[] mTypefaces;
        private final boolean[] mResolved;
        private String mSelected;

        FontsAdapter(Context context, List<String> values, String selected) {
            mContext = context;
            mValues = values;
            mTypefaces = new Typeface[values.size()];
            mResolved = new boolean[values.size()];
            mSelected = values.contains(selected) ? selected : "";
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.quickspace_font_option, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            String value = mValues.get(position);
            holder.title.setText(getLabelFor(mContext, value));
            // Each font shows in its own face; "Clock default" keeps the dialog font.
            holder.title.setTypeface(position == 0 ? Typeface.DEFAULT : typefaceAt(position));
            holder.itemView.setActivated(value.equals(mSelected));
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                mSelected = mValues.get(pos);
                notifyDataSetChanged();
                onFontPicked(mSelected);
            });
        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        private Typeface typefaceAt(int position) {
            if (!mResolved[position]) {
                mTypefaces[position] = QuickspaceClockFonts.getTypefaceForPackage(
                        mContext, mValues.get(position));
                mResolved[position] = true;
            }
            return mTypefaces[position] != null ? mTypefaces[position] : Typeface.DEFAULT;
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView title;

            Holder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.quickspace_font_option_title);
            }
        }
    }
}
