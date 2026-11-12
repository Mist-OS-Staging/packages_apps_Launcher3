/*
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

import static com.android.launcher3.util.Executors.MAIN_EXECUTOR;
import static com.android.launcher3.util.Executors.THREAD_POOL_EXECUTOR;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.icons.pack.IconPack;
import com.android.launcher3.icons.pack.IconPackManager;
import com.android.launcher3.util.ComponentKey;
import com.android.launcher3.views.AbstractSlideInView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lets the user pick any icon of any installed icon pack as the icon of one app. */
public class IconPickerBottomSheet extends AbstractSlideInView<Launcher> {
    private static final int DEFAULT_CLOSE_DURATION = 200;
    private static final long SEARCH_DEBOUNCE_MS = 300;
    private static final int GRID_COLUMNS = 5;

    /** Called after the user picked an icon. */
    public interface OnIconChosen {
        void onIconChosen(CharSequence packLabel);
    }

    private RecyclerView mRecyclerView;
    private TextView mTitle;
    private EditText mSearchField;
    private final IconPackManager mManager;
    private ComponentKey mKey;
    private OnIconChosen mCallback;

    private final Handler mDebounceHandler = new Handler(Looper.getMainLooper());
    private Runnable mPendingFilter;
    private List<IconPack.IconEntry> mAllEntriesForCurrentPack;
    private Resources mCurrentPackRes;
    private String mCurrentPackPackage;
    private CharSequence mCurrentPackLabel;

    public IconPickerBottomSheet(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IconPickerBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mManager = IconPackManager.get(context);
        setWillNotDraw(false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mContent = findViewById(R.id.icon_picker_content);
        mRecyclerView = findViewById(R.id.icon_picker_recycler_view);
        mTitle = findViewById(R.id.icon_picker_title);
        mSearchField = findViewById(R.id.icon_picker_search);
        mSearchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleFilter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        mRecyclerView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard();
            }
            return false;
        });
        setContentBackgroundWithParent(
                getContext().getDrawable(R.drawable.bg_rounded_corner_bottom_sheet), mContent);
    }

    /** Shows the list of icon packs for {@code key}. */
    public void show(ComponentKey key, OnIconChosen callback) {
        mKey = key;
        mCallback = callback;
        mTitle.setText(R.string.app_info_custom_icon_title);
        showPackList();
        attachToContainer();
        mIsOpen = true;
        setUpDefaultOpenAnimation().start();
    }

    private void showPackList() {
        mSearchField.setVisibility(View.GONE);
        mSearchField.setText("");
        List<Map.Entry<String, CharSequence>> packs =
                new ArrayList<>(mManager.getProviderNames().entrySet());
        packs.sort((a, b) -> a.getValue().toString().compareToIgnoreCase(
                b.getValue().toString()));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setAdapter(new PackListAdapter(packs, this::showIconGrid));
    }

    private void showIconGrid(String packPackage, CharSequence packLabel) {
        mCurrentPackPackage = packPackage;
        mCurrentPackLabel = packLabel;
        mTitle.setText(packLabel);
        mSearchField.setText("");
        mSearchField.setVisibility(View.VISIBLE);
        Context context = getContext();
        THREAD_POOL_EXECUTOR.execute(() -> {
            List<IconPack.IconEntry> entries = mManager.getAllIconEntries(packPackage);
            Resources packRes;
            try {
                packRes = context.getPackageManager().getResourcesForApplication(packPackage);
            } catch (PackageManager.NameNotFoundException e) {
                packRes = null;
            }
            final Resources finalPackRes = packRes;
            MAIN_EXECUTOR.execute(() -> {
                if (!isOpen() || !packPackage.equals(mCurrentPackPackage)) return;
                mAllEntriesForCurrentPack = entries;
                mCurrentPackRes = finalPackRes;
                mRecyclerView.setLayoutManager(new GridLayoutManager(context, GRID_COLUMNS));
                mRecyclerView.setAdapter(new IconGridAdapter(entries, finalPackRes,
                        this::onIconPicked));
            });
        });
    }

    private void scheduleFilter(String rawQuery) {
        if (mPendingFilter != null) {
            mDebounceHandler.removeCallbacks(mPendingFilter);
        }
        mPendingFilter = () -> applyFilter(rawQuery.trim().toLowerCase(Locale.ROOT));
        mDebounceHandler.postDelayed(mPendingFilter, SEARCH_DEBOUNCE_MS);
    }

    private void applyFilter(String query) {
        if (mAllEntriesForCurrentPack == null) return;

        List<IconPack.IconEntry> filtered;
        if (query.isEmpty()) {
            filtered = mAllEntriesForCurrentPack;
        } else {
            filtered = new ArrayList<>();
            for (IconPack.IconEntry entry : mAllEntriesForCurrentPack) {
                if (entry.drawableName.toLowerCase(Locale.ROOT).contains(query)) {
                    filtered.add(entry);
                }
            }
        }
        mRecyclerView.setAdapter(new IconGridAdapter(filtered, mCurrentPackRes,
                this::onIconPicked));
    }

    private void onIconPicked(IconPack.IconEntry entry) {
        hideKeyboard();
        IconDatabase.setExplicitIconForComponent(
                getContext(), mKey, mCurrentPackPackage, entry.drawableName);
        if (mCallback != null) {
            mCallback.onIconChosen(mCurrentPackLabel);
        }
        close(true);
    }

    private void hideKeyboard() {
        if (mSearchField == null) return;
        InputMethodManager imm = getContext().getSystemService(InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mSearchField.getWindowToken(), 0);
        }
        mSearchField.clearFocus();
    }

    @Override
    protected void handleClose(boolean animate) {
        hideKeyboard();
        mDebounceHandler.removeCallbacksAndMessages(null);
        handleClose(animate, DEFAULT_CLOSE_DURATION);
    }

    @Override
    protected boolean isOfType(@FloatingViewType int type) {
        return (type & TYPE_WIDGETS_BOTTOM_SHEET) != 0;
    }

    @Override
    protected Pair<View, String> getAccessibilityTarget() {
        return Pair.create(mTitle, getContext().getString(R.string.app_info_custom_icon_title));
    }

    private static class PackListAdapter extends RecyclerView.Adapter<PackListAdapter.VH> {
        interface OnPackClick {
            void onClick(String packPackage, CharSequence packLabel);
        }

        private final List<Map.Entry<String, CharSequence>> mItems;
        private final OnPackClick mListener;

        PackListAdapter(List<Map.Entry<String, CharSequence>> packs, OnPackClick listener) {
            mItems = packs;
            mListener = listener;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            Map.Entry<String, CharSequence> entry = mItems.get(position);
            holder.text.setText(entry.getValue());
            holder.itemView.setOnClickListener(
                    v -> mListener.onClick(entry.getKey(), entry.getValue()));
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final TextView text;

            VH(View itemView) {
                super(itemView);
                text = itemView.findViewById(android.R.id.text1);
            }
        }
    }

    private static class IconGridAdapter extends RecyclerView.Adapter<IconGridAdapter.VH> {
        interface OnIconClick {
            void onClick(IconPack.IconEntry entry);
        }

        private final List<IconPack.IconEntry> mEntries;
        private final OnIconClick mListener;
        private final Resources mPackRes;

        IconGridAdapter(List<IconPack.IconEntry> entries, Resources packRes,
                OnIconClick listener) {
            mEntries = entries;
            mPackRes = packRes;
            mListener = listener;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            ImageView iv = new ImageView(parent.getContext());
            Resources res = parent.getContext().getResources();
            int size = res.getDimensionPixelSize(R.dimen.icon_picker_cell_size);
            iv.setLayoutParams(new ViewGroup.LayoutParams(size, size));
            int pad = res.getDimensionPixelSize(R.dimen.icon_picker_cell_padding);
            iv.setPadding(pad, pad, pad, pad);
            iv.setBackgroundResource(R.drawable.icon_picker_cell_background);
            return new VH(iv);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            IconPack.IconEntry entry = mEntries.get(position);
            Drawable d = null;
            if (mPackRes != null) {
                try {
                    d = mPackRes.getDrawable(entry.resId, null);
                } catch (Resources.NotFoundException ignored) {
                }
            }
            holder.image.setImageDrawable(d);
            holder.image.setContentDescription(entry.drawableName);
            holder.itemView.setOnClickListener(v -> mListener.onClick(entry));
        }

        @Override
        public int getItemCount() {
            return mEntries.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            final ImageView image;

            VH(View itemView) {
                super(itemView);
                image = (ImageView) itemView;
            }
        }
    }
}
