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
import java.util.List;

public class AppLibrarySearchAdapter extends RecyclerView.Adapter<AppLibrarySearchAdapter.ViewHolder> {

    public interface OnSearchItemClickListener {
        void onSearchItemClick(View view, AppInfo app);
    }

    private final ActivityContext mActivityContext;
    private final List<AppInfo> mApps = new ArrayList<>();
    private OnSearchItemClickListener mItemClickListener;

    public AppLibrarySearchAdapter(ActivityContext activityContext) {
        mActivityContext = activityContext;
    }

    public void setOnSearchItemClickListener(OnSearchItemClickListener listener) {
        mItemClickListener = listener;
    }

    public void setApps(List<AppInfo> apps) {
        mApps.clear();
        if (apps != null) {
            mApps.addAll(apps);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.app_library_search_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppInfo app = mApps.get(position);
        holder.itemView.setTag(app);
        holder.mIcon.setTag(app);
        holder.mTitle.setText(app.title);

        if (holder.mSubtitle != null) {
            holder.mSubtitle.setVisibility(View.GONE);
        }

        if (app.bitmap != null) {
            FastBitmapDrawable drawable = app.newIcon(holder.itemView.getContext());
            holder.mIcon.setImageDrawable(drawable);
        } else {
            holder.mIcon.setImageDrawable(null);
        }

        holder.itemView.setOnClickListener(v -> {
            if (mItemClickListener != null) {
                mItemClickListener.onSearchItemClick(holder.mIcon, app);
            } else {
                mActivityContext.getItemOnClickListener().onClick(holder.mIcon);
            }
        });

        if (mActivityContext instanceof Launcher) {
            Launcher launcher = (Launcher) mActivityContext;
            holder.itemView.setOnLongClickListener(launcher.getAllAppsItemLongClickListener());
        }
    }

    @Override
    public int getItemCount() {
        return mApps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView mIcon;
        final TextView mTitle;
        final TextView mSubtitle;

        ViewHolder(View view) {
            super(view);
            mIcon = view.findViewById(R.id.app_icon);
            mTitle = view.findViewById(R.id.app_title);
            mSubtitle = view.findViewById(R.id.app_subtitle);
        }
    }
}
