package com.android.launcher3.applibrary.view;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.android.launcher3.applibrary.model.AppCategoryGroup;
import com.android.launcher3.views.ActivityContext;

import java.util.ArrayList;
import java.util.List;

public class AppLibraryAdapter extends RecyclerView.Adapter<AppLibraryAdapter.ViewHolder> {

    private final ActivityContext mActivityContext;
    private final List<AppCategoryGroup> mGroups = new ArrayList<>();
    private AppCategoryCardView.OnCategoryExpandListener mExpandListener;

    public AppLibraryAdapter(ActivityContext activityContext) {
        mActivityContext = activityContext;
    }

    public void setOnCategoryExpandListener(AppCategoryCardView.OnCategoryExpandListener listener) {
        mExpandListener = listener;
    }

    public void setGroups(List<AppCategoryGroup> groups) {
        mGroups.clear();
        if (groups != null) {
            mGroups.addAll(groups);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        AppCategoryCardView view = (AppCategoryCardView) inflater.inflate(
                R.layout.app_library_category_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppCategoryGroup group = mGroups.get(position);
        holder.mCardView.setOnCategoryExpandListener(mExpandListener);
        holder.mCardView.bindGroup(group, mActivityContext);
    }

    @Override
    public int getItemCount() {
        return mGroups.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final AppCategoryCardView mCardView;

        ViewHolder(AppCategoryCardView cardView) {
            super(cardView);
            mCardView = cardView;
        }
    }
}
