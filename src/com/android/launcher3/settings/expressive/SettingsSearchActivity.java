/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings.expressive;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.graphics.Insets;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.launcher3.R;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SettingsSearchActivity extends Activity {

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private static final String EXTRA_HIGHLIGHT_KEY = ":settings:fragment_args_key";

    private static final int[] XMLS = {
            R.xml.launcher_icons_preferences,
            R.xml.launcher_home_screen_preferences,
            R.xml.launcher_app_drawer_preferences,
            R.xml.launcher_recents_preferences,
            R.xml.launcher_misc_preferences,
    };
    private static final SettingsCategory[] XML_CATEGORIES = {
            SettingsCategory.ICONS,
            SettingsCategory.HOME,
            SettingsCategory.APP_DRAWER,
            SettingsCategory.RECENTS,
            SettingsCategory.MISC,
    };

    private final List<Entry> mIndex = new ArrayList<>();
    private final List<Entry> mResults = new ArrayList<>();
    private ResultsAdapter mAdapter;
    private TextView mEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_expressive_search);

        View root = findViewById(R.id.settings_search_root);
        ListView list = findViewById(R.id.settings_search_results);
        int listBottom = list.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, 0);
            list.setPadding(list.getPaddingLeft(), list.getPaddingTop(),
                    list.getPaddingRight(), listBottom + bars.bottom);
            return insets;
        });

        mEmpty = findViewById(R.id.settings_search_empty);
        mAdapter = new ResultsAdapter();
        list.setAdapter(mAdapter);
        list.setOnItemClickListener((parent, view, position, id) -> open(mResults.get(position)));

        findViewById(R.id.settings_search_back).setOnClickListener(v -> finish());
        EditText input = findViewById(R.id.settings_search_input);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                filter(s.toString());
            }
        });

        buildIndex();
        filter("");
        input.requestFocus();
        input.post(() -> {
            InputMethodManager imm = getSystemService(InputMethodManager.class);
            if (imm != null) {
                imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void buildIndex() {
        for (int i = 0; i < XMLS.length; i++) {
            try (XmlResourceParser parser = getResources().getXml(XMLS[i])) {
                int event;
                while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                    if (event != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String tag = parser.getName();
                    if (tag.endsWith("PreferenceScreen") || tag.endsWith("PreferenceCategory")) {
                        continue;
                    }
                    String key = parser.getAttributeValue(ANDROID_NS, "key");
                    CharSequence title = readText(parser, "title");
                    if (TextUtils.isEmpty(key) || TextUtils.isEmpty(title)) {
                        continue;
                    }
                    CharSequence summary = readText(parser, "summary");
                    mIndex.add(new Entry(key, title, summary, XML_CATEGORIES[i]));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private CharSequence readText(XmlResourceParser parser, String attr) {
        int res = parser.getAttributeResourceValue(ANDROID_NS, attr, 0);
        if (res != 0) {
            try {
                return getText(res);
            } catch (RuntimeException e) {
                return null;
            }
        }
        String raw = parser.getAttributeValue(ANDROID_NS, attr);
        return raw == null || raw.contains("%s") ? null : raw;
    }

    private void filter(String query) {
        String q = query.trim().toLowerCase(Locale.getDefault());
        mResults.clear();
        for (Entry entry : mIndex) {
            if (q.isEmpty() || entry.matches(q)) {
                mResults.add(entry);
            }
        }
        mEmpty.setVisibility(mResults.isEmpty() ? View.VISIBLE : View.GONE);
        mAdapter.notifyDataSetChanged();
    }

    private void open(Entry entry) {
        startActivity(new Intent(this, entry.category.activity)
                .putExtra(EXTRA_HIGHLIGHT_KEY, entry.key));
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    private static final class Entry {
        final String key;
        final CharSequence title;
        final CharSequence summary;
        final SettingsCategory category;
        final String searchText;

        Entry(String key, CharSequence title, CharSequence summary, SettingsCategory category) {
            this.key = key;
            this.title = title;
            this.summary = summary;
            this.category = category;
            this.searchText = (title + " " + (summary == null ? "" : summary))
                    .toLowerCase(Locale.getDefault());
        }

        boolean matches(String query) {
            return searchText.contains(query);
        }
    }

    private final class ResultsAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mResults.size();
        }

        @Override
        public Entry getItem(int position) {
            return mResults.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView : LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.settings_expressive_search_item, parent, false);
            Entry entry = getItem(position);
            int container = getColor(entry.category.containerColorRes);
            int onContainer = getColor(entry.category.onContainerColorRes);

            float big = dp(24);
            float small = dp(6);
            boolean first = position == 0;
            boolean last = position == getCount() - 1;
            GradientDrawable background = new GradientDrawable();
            background.setColor(getColor(R.color.settings_expressive_card));
            background.setCornerRadii(new float[] {
                    first ? big : small, first ? big : small,
                    first ? big : small, first ? big : small,
                    last ? big : small, last ? big : small,
                    last ? big : small, last ? big : small});
            view.setBackground(background);

            ExpressiveShapeView shape = view.findViewById(R.id.search_item_shape);
            shape.setShape(entry.category.shape);
            shape.setColor(container);
            ImageView glyph = view.findViewById(R.id.search_item_glyph);
            glyph.setImageResource(entry.category.glyphRes);
            glyph.setImageTintList(ColorStateList.valueOf(onContainer));
            ((TextView) view.findViewById(R.id.search_item_title)).setText(entry.title);
            TextView summary = view.findViewById(R.id.search_item_summary);
            CharSequence category = getText(entry.category.titleRes);
            summary.setText(TextUtils.isEmpty(entry.summary) ? category
                    : TextUtils.concat(category, " · ", entry.summary));
            return view;
        }
    }
}
