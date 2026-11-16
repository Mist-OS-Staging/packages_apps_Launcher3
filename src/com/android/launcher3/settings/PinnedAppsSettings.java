/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.android.launcher3.allapps.PinnedAppsStore;
import com.android.launcher3.util.Executors;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class PinnedAppsSettings extends CollapsingToolbarBaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                            new PinnedAppsFragment())
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static final class PinnedAppsFragment extends Fragment {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_APP = 1;

        private final List<AppEntry> mEntries = new ArrayList<>();
        private final Handler mHandler = new Handler(Looper.getMainLooper());
        private PinnedAppsAdapter mAdapter;
        private ItemTouchHelper mTouchHelper;
        private boolean mLoaded;
        private boolean mOrderChanged;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            RecyclerView list = (RecyclerView) inflater.inflate(
                    R.layout.pinned_apps_settings, container, false);
            list.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
            mAdapter = new PinnedAppsAdapter();
            list.setAdapter(mAdapter);
            mTouchHelper = new ItemTouchHelper(new DragCallback());
            mTouchHelper.attachToRecyclerView(list);
            loadApps();
            return list;
        }

        private void loadApps() {
            final Context appContext = requireContext().getApplicationContext();
            Executors.MODEL_EXECUTOR.execute(() -> {
                List<AppEntry> apps = queryApps(appContext);
                mHandler.post(() -> {
                    if (!isAdded()) {
                        return;
                    }
                    mEntries.clear();
                    mEntries.addAll(apps);
                    mLoaded = true;
                    mAdapter.notifyDataSetChanged();
                });
            });
        }

        private static List<AppEntry> queryApps(Context context) {
            LauncherApps launcherApps = context.getSystemService(LauncherApps.class);
            List<AppEntry> ordered = new ArrayList<>();
            List<AppEntry> unordered = new ArrayList<>();
            if (launcherApps == null) {
                return ordered;
            }
            List<String> orderedKeys = PinnedAppsStore.getOrderedKeys(context);
            Set<String> seen = new HashSet<>();
            for (String key : orderedKeys) {
                AppEntry entry = resolve(context, launcherApps, key);
                if (entry != null && seen.add(key)) {
                    ordered.add(entry);
                }
            }
            for (String key : PinnedAppsStore.getPinnedKeys(context)) {
                if (seen.contains(key)) {
                    continue;
                }
                AppEntry entry = resolve(context, launcherApps, key);
                if (entry != null && seen.add(key)) {
                    unordered.add(entry);
                }
            }
            Collator collator = Collator.getInstance();
            unordered.sort((left, right) -> collator.compare(left.label, right.label));
            ordered.addAll(unordered);
            return ordered;
        }

        @Nullable
        private static AppEntry resolve(Context context, LauncherApps launcherApps, String key) {
            ComponentName component = PinnedAppsStore.decodeComponent(key);
            UserHandle user = PinnedAppsStore.decodeUser(context, key);
            if (component == null || user == null) {
                return null;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setComponent(component);
                LauncherActivityInfo info = launcherApps.resolveActivity(intent, user);
                if (info == null) {
                    return null;
                }
                return new AppEntry(key, info.getLabel().toString(), info.getBadgedIcon(0));
            } catch (RuntimeException e) {
                return null;
            }
        }

        private void moveEntry(int from, int to) {
            if (from == to || from < 0 || to < 0
                    || from >= mEntries.size() || to >= mEntries.size()) {
                return;
            }
            mEntries.add(to, mEntries.remove(from));
            mAdapter.notifyItemMoved(from + 1, to + 1);
            mOrderChanged = true;
        }

        private void saveOrder() {
            if (!mOrderChanged) {
                return;
            }
            mOrderChanged = false;
            List<String> keys = new ArrayList<>(mEntries.size());
            for (AppEntry entry : mEntries) {
                keys.add(entry.key);
            }
            PinnedAppsStore.setOrder(requireContext(), keys);
        }

        private void refreshPositions() {
            mHandler.post(() -> {
                if (mAdapter != null && !mEntries.isEmpty()) {
                    mAdapter.notifyItemRangeChanged(1, mEntries.size());
                }
            });
        }

        private void moveAndSave(int from, int to) {
            moveEntry(from, to);
            saveOrder();
            refreshPositions();
        }

        private void unpin(int index) {
            if (index < 0 || index >= mEntries.size()) {
                return;
            }
            AppEntry entry = mEntries.remove(index);
            PinnedAppsStore.setPinned(requireContext(), entry.key, false);
            mAdapter.notifyItemRemoved(index + 1);
            if (mEntries.isEmpty()) {
                mAdapter.notifyItemChanged(0);
            } else {
                refreshPositions();
            }
        }

        private final class PinnedAppsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

            @Override
            public int getItemViewType(int position) {
                return position == 0 ? TYPE_HEADER : TYPE_APP;
            }

            @Override
            public int getItemCount() {
                return mEntries.size() + 1;
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                    int viewType) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                if (viewType == TYPE_HEADER) {
                    return new HeaderHolder(inflater.inflate(
                            R.layout.pinned_apps_settings_header, parent, false));
                }
                return new AppHolder(inflater.inflate(
                        R.layout.pinned_apps_settings_item, parent, false));
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                if (holder instanceof HeaderHolder header) {
                    Context context = header.itemView.getContext();
                    if (mLoaded && mEntries.isEmpty()) {
                        header.text.setText(R.string.pinned_apps_settings_empty);
                    } else if (PinnedAppsStore.hasRowLimit(context)) {
                        header.text.setText(context.getString(R.string.pinned_apps_settings_hint)
                                + "\n\n"
                                + context.getString(R.string.pinned_apps_settings_hint_limit));
                    } else {
                        header.text.setText(R.string.pinned_apps_settings_hint);
                    }
                } else if (holder instanceof AppHolder app) {
                    app.bind(mEntries.get(position - 1));
                }
            }
        }

        private static final class HeaderHolder extends RecyclerView.ViewHolder {
            final TextView text;

            HeaderHolder(View view) {
                super(view);
                text = (TextView) view;
            }
        }

        private final class AppHolder extends RecyclerView.ViewHolder {
            final ImageView dragHandle;
            final ImageView icon;
            final TextView title;
            final ImageButton unpin;
            int moveUpActionId = View.NO_ID;
            int moveDownActionId = View.NO_ID;

            AppHolder(View view) {
                super(view);
                dragHandle = view.findViewById(R.id.drag_handle);
                icon = view.findViewById(R.id.icon);
                title = view.findViewById(R.id.title);
                unpin = view.findViewById(R.id.unpin);
                dragHandle.setOnTouchListener((v, event) -> {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                        mTouchHelper.startDrag(this);
                    }
                    return false;
                });
                unpin.setOnClickListener(v -> unpin(getBindingAdapterPosition() - 1));
            }

            void bind(AppEntry entry) {
                icon.setImageDrawable(entry.icon);
                title.setText(entry.label);
                unpin.setContentDescription(itemView.getContext().getString(
                        R.string.pinned_apps_unpin_description, entry.label));
                updateAccessibilityActions();
            }

            void updateAccessibilityActions() {
                if (moveUpActionId != View.NO_ID) {
                    ViewCompat.removeAccessibilityAction(itemView, moveUpActionId);
                    moveUpActionId = View.NO_ID;
                }
                if (moveDownActionId != View.NO_ID) {
                    ViewCompat.removeAccessibilityAction(itemView, moveDownActionId);
                    moveDownActionId = View.NO_ID;
                }
                Context context = itemView.getContext();
                int index = getBindingAdapterPosition() - 1;
                if (index > 0) {
                    moveUpActionId = ViewCompat.addAccessibilityAction(itemView,
                            context.getString(R.string.pinned_apps_move_up), (v, args) -> {
                                int from = getBindingAdapterPosition() - 1;
                                moveAndSave(from, from - 1);
                                return true;
                            });
                }
                if (index >= 0 && index < mEntries.size() - 1) {
                    moveDownActionId = ViewCompat.addAccessibilityAction(itemView,
                            context.getString(R.string.pinned_apps_move_down), (v, args) -> {
                                int from = getBindingAdapterPosition() - 1;
                                moveAndSave(from, from + 1);
                                return true;
                            });
                }
            }
        }

        private final class DragCallback extends ItemTouchHelper.Callback {

            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder) {
                return viewHolder instanceof AppHolder
                        ? makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0)
                        : 0;
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                if (!(target instanceof AppHolder)) {
                    return false;
                }
                moveEntry(viewHolder.getBindingAdapterPosition() - 1,
                        target.getBindingAdapterPosition() - 1);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }

            @Override
            public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder,
                    int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    viewHolder.itemView.performHapticFeedback(
                            HapticFeedbackConstants.LONG_PRESS);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                saveOrder();
                refreshPositions();
            }
        }

        private static final class AppEntry {
            final String key;
            final String label;
            final Drawable icon;

            AppEntry(String key, String label, Drawable icon) {
                this.key = key;
                this.label = label;
                this.icon = icon;
            }
        }
    }
}
