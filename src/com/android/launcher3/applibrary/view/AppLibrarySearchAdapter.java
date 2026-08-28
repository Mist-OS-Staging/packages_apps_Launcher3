package com.android.launcher3.applibrary.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.icons.FastBitmapDrawable;
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
        final int type;
        final String title;
        final AppInfo app;

        ListItem(String title) {
            this.type = VIEW_TYPE_HEADER;
            this.title = title;
            this.app = null;
        }

        ListItem(AppInfo app) {
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

    public void setApps(List<AppInfo> apps) {
        setApps(apps, false);
    }

    public void setApps(List<AppInfo> apps, boolean groupAlphabetically) {
        mItems.clear();
        mSectionPositions.clear();
        if (apps != null && !apps.isEmpty()) {
            if (groupAlphabetically) {
                List<AppInfo> sorted = new ArrayList<>(apps);
                Collections.sort(sorted, (a, b) -> {
                    String ta = a.title != null ? a.title.toString() : "";
                    String tb = b.title != null ? b.title.toString() : "";
                    return ta.compareToIgnoreCase(tb);
                });

                String currentSection = null;
                for (AppInfo app : sorted) {
                    String title = app.title != null ? app.title.toString().trim() : "";
                    String firstChar = "#";
                    if (!title.isEmpty()) {
                        char c = Character.toUpperCase(title.charAt(0));
                        if (c >= 'A' && c <= 'Z') {
                            firstChar = String.valueOf(c);
                        }
                    }
                    if (!firstChar.equals(currentSection)) {
                        currentSection = firstChar;
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
        if (section == null) {
            return -1;
        }
        Integer pos = mSectionPositions.get(section.toUpperCase(Locale.ROOT));
        return pos != null ? pos : -1;
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
            avh.mIcon.setTag(app);
            avh.mTitle.setText(app.title);

            if (avh.mSubtitle != null) {
                avh.mSubtitle.setVisibility(View.GONE);
            }

            if (app.bitmap != null) {
                FastBitmapDrawable drawable = app.newIcon(avh.itemView.getContext());
                avh.mIcon.setImageDrawable(drawable);
            } else {
                avh.mIcon.setImageDrawable(null);
            }

            avh.itemView.setOnClickListener(v -> {
                if (mItemClickListener != null) {
                    mItemClickListener.onSearchItemClick(avh.mIcon, app);
                } else {
                    mActivityContext.getItemOnClickListener().onClick(avh.mIcon);
                }
            });

            if (mActivityContext instanceof Launcher) {
                Launcher launcher = (Launcher) mActivityContext;
                avh.itemView.setOnLongClickListener(launcher.getAllAppsItemLongClickListener());
            }
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
        final ImageView mIcon;
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
