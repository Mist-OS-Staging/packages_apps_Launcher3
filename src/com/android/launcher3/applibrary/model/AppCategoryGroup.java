package com.android.launcher3.applibrary.model;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.android.launcher3.model.data.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppCategoryGroup {

    private final AppCategory mCategory;
    private final List<AppInfo> mApps;

    public AppCategoryGroup(@NonNull AppCategory category, @NonNull List<AppInfo> apps) {
        mCategory = category;
        mApps = new ArrayList<>(apps);
    }

    @NonNull
    public AppCategory getCategory() {
        return mCategory;
    }

    @NonNull
    public List<AppInfo> getApps() {
        return Collections.unmodifiableList(mApps);
    }

    @StringRes
    public int getTitleRes() {
        return mCategory.getTitleRes();
    }

    public int getAppCount() {
        return mApps.size();
    }

    public AppInfo getAppAt(int index) {
        if (index >= 0 && index < mApps.size()) {
            return mApps.get(index);
        }
        return null;
    }
}
