package com.android.launcher3.applibrary.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.BubbleTextView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.views.ActivityContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppLibrarySearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_APP = 1;

    public interface OnSearchItemClickListener {
        void onSearchItemClick(View view, AppInfo app);
    }

    public static class ListItem {
        public final int type;
        public final String title;
        public final AppInfo app;

        public ListItem(String title) {
            this.type = VIEW_TYPE_HEADER;
            this.title = title;
            this.app = null;
        }

        public ListItem(AppInfo app) {
            this.type = VIEW_TYPE_APP;
            this.title = null;
            this.app = app;
        }
    }

    private final ActivityContext mActivityContext;
    private final List<ListItem> mItems = new ArrayList<>();
    private final Map<String, Integer> mSectionPositions = new HashMap<>();
    private OnSearchItemClickListener mItemClickListener;

    public AppLibrarySearchAdapter(ActivityContext activityContext) {
        mActivityContext = activityContext;
    }

    public void setOnSearchItemClickListener(OnSearchItemClickListener listener) {
        mItemClickListener = listener;
    }

    public void setApps(List<AppInfo> apps, boolean grouped) {
        mItems.clear();
        mSectionPositions.clear();
        if (apps != null && !apps.isEmpty()) {
            if (grouped) {
                String currentSection = null;
                for (AppInfo app : apps) {
                    String title = app.title != null ? app.title.toString().trim() : "";
                    String section = "#";
                    if (!title.isEmpty()) {
                        char firstChar = title.toUpperCase(Locale.getDefault()).charAt(0);
                        if (Character.isLetter(firstChar)) {
                            section = String.valueOf(firstChar);
                        }
                    }

                    if (!section.equals(currentSection)) {
                        currentSection = section;
                        mSectionPositions.put(currentSection, mItems.size());
                        mItems.add(new ListItem(currentSection));
                    }
                    mItems.add(new ListItem(app));
                }
            } else {
                for (AppInfo app : apps) {
                    mItems.add(new ListItem(app));
                }
            }
        }
        notifyDataSetChanged();
    }

    public int getPositionForSection(String section) {
        Integer pos = mSectionPositions.get(section);
        return pos != null ? pos : -1;
    }

    public List<AppInfo> getApps() {
        List<AppInfo> apps = new ArrayList<>();
        for (ListItem item : mItems) {
            if (item.type == VIEW_TYPE_APP && item.app != null) {
                apps.add(item.app);
            }
        }
        return apps;
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_HEADER) {
            View view = inflater.inflate(R.layout.app_library_section_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.app_library_search_row, parent, false);
            return new AppViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = mItems.get(position);
        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder hvh = (HeaderViewHolder) holder;
            hvh.mTitle.setText(item.title);
        } else if (holder instanceof AppViewHolder) {
            AppViewHolder avh = (AppViewHolder) holder;
            AppInfo app = item.app;
            if (app == null) {
                return;
            }
            avh.itemView.setTag(app);
            avh.mIcon.reset();
            avh.mIcon.applyFromApplicationInfo(app);
            avh.mIcon.setText("");
            avh.mTitle.setText(app.title);

            if (avh.mSubtitle != null) {
                avh.mSubtitle.setVisibility(View.GONE);
            }

            View.OnClickListener clickListener = v -> {
                if (mItemClickListener != null) {
                    mItemClickListener.onSearchItemClick(avh.mIcon, app);
                } else {
                    mActivityContext.getItemOnClickListener().onClick(avh.mIcon);
                }
            };
            avh.itemView.setOnClickListener(clickListener);
            avh.mIcon.setOnClickListener(clickListener);

            View.OnLongClickListener longClickListener = v -> {
                if (mActivityContext instanceof Launcher) {
                    Launcher launcher = (Launcher) mActivityContext;
                    if (launcher.getPopupControllerForAppIcons() != null) {
                        return launcher.getPopupControllerForAppIcons().show(avh.mIcon) != null;
                    }
                }
                return false;
            };
            avh.itemView.setOnLongClickListener(longClickListener);
            avh.mIcon.setOnLongClickListener(longClickListener);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView mTitle;

        HeaderViewHolder(View view) {
            super(view);
            mTitle = view.findViewById(R.id.section_title);
        }
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        final BubbleTextView mIcon;
        final TextView mTitle;
        final TextView mSubtitle;

        AppViewHolder(View view) {
            super(view);
            mIcon = view.findViewById(R.id.app_icon);
            mTitle = view.findViewById(R.id.app_title);
            mSubtitle = view.findViewById(R.id.app_subtitle);
        }
    }
}
