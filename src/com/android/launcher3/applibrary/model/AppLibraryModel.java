package com.android.launcher3.applibrary.model;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.launcher3.allapps.AllAppsStore;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.util.Executors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AppLibraryModel implements AllAppsStore.OnUpdateListener {

    public interface AppLibraryModelListener {
        void onCategoriesUpdated(List<AppCategoryGroup> groups);
        void onAllAppsUpdated(List<AppInfo> allApps);
    }

    private static final Comparator<AppInfo> APP_NAME_COMPARATOR = (a, b) -> {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        String titleA = a.title != null ? a.title.toString() : "";
        String titleB = b.title != null ? b.title.toString() : "";
        int res = String.CASE_INSENSITIVE_ORDER.compare(titleA, titleB);
        if (res != 0) return res;
        return a.user.hashCode() - b.user.hashCode();
    };

    private final Context mContext;
    private final AllAppsStore mAppsStore;
    private final List<AppLibraryModelListener> mListeners = new CopyOnWriteArrayList<>();
    private final Map<String, Long> mInstallTimeCache = new ConcurrentHashMap<>();

    private List<AppInfo> mAllAppsList = Collections.emptyList();
    private List<AppCategoryGroup> mCategoryGroups = Collections.emptyList();

    public AppLibraryModel(Context context, AllAppsStore appsStore) {
        mContext = context.getApplicationContext();
        mAppsStore = appsStore;
        if (mAppsStore != null) {
            mAppsStore.addUpdateListener(this);
        }
        rebuildCategories();
    }

    public void addListener(AppLibraryModelListener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
            listener.onCategoriesUpdated(mCategoryGroups);
            listener.onAllAppsUpdated(mAllAppsList);
        }
    }

    public void removeListener(AppLibraryModelListener listener) {
        if (listener != null) {
            mListeners.remove(listener);
        }
    }

    public void destroy() {
        if (mAppsStore != null) {
            mAppsStore.removeUpdateListener(this);
        }
        mListeners.clear();
        mInstallTimeCache.clear();
    }

    @Override
    public void onAppsUpdated() {
        AppCategoryEngine.clearCache();
        mInstallTimeCache.clear();
        rebuildCategories();
    }

    public void forceRefresh() {
        rebuildCategories();
    }

    private long getAppInstallTime(AppInfo app) {
        if (app == null || app.componentName == null) {
            return 0;
        }
        String packageName = app.componentName.getPackageName();
        int userId = app.user != null ? app.user.getIdentifier() : 0;
        String key = packageName + "#" + userId;
        Long cached = mInstallTimeCache.get(key);
        if (cached != null) {
            return cached;
        }
        long installTime = 0;
        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo packageInfo;
            if (app.user != null) {
                packageInfo = pm.getPackageInfoAsUser(packageName, 0, userId);
            } else {
                packageInfo = pm.getPackageInfo(packageName, 0);
            }
            if (packageInfo != null) {
                installTime = packageInfo.firstInstallTime;
            }
        } catch (Exception e) {
            try {
                PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(packageName, 0);
                if (packageInfo != null) {
                    installTime = packageInfo.firstInstallTime;
                }
            } catch (Exception ignored) {
            }
        }
        mInstallTimeCache.put(key, installTime);
        return installTime;
    }

    private void rebuildCategories() {
        if (mAppsStore == null) {
            return;
        }

        AppInfo[] rawApps = mAppsStore.getApps();
        if (rawApps == null || rawApps.length == 0) {
            mAllAppsList = Collections.emptyList();
            mCategoryGroups = Collections.emptyList();
            notifyListeners();
            return;
        }

        List<AppInfo> allApps = new ArrayList<>(rawApps.length);
        for (AppInfo app : rawApps) {
            if (app != null && app.componentName != null) {
                allApps.add(app);
            }
        }
        allApps.sort(APP_NAME_COMPARATOR);
        mAllAppsList = Collections.unmodifiableList(allApps);

        EnumMap<AppCategory, List<AppInfo>> map = new EnumMap<>(AppCategory.class);
        for (AppCategory cat : AppCategory.values()) {
            map.put(cat, new ArrayList<>());
        }

        for (AppInfo app : allApps) {
            AppCategory category = AppCategoryEngine.getCategoryForApp(mContext, app);
            List<AppInfo> list = map.get(category);
            if (list != null) {
                list.add(app);
            }
        }

        List<AppInfo> suggestions = new ArrayList<>();
        int maxSuggestions = Math.min(4, allApps.size());
        for (int i = 0; i < maxSuggestions; i++) {
            suggestions.add(allApps.get(i));
        }
        map.put(AppCategory.SUGGESTIONS, suggestions);

        List<AppInfo> appsByInstallTime = new ArrayList<>(allApps);
        appsByInstallTime.sort((a, b) -> Long.compare(getAppInstallTime(b), getAppInstallTime(a)));
        List<AppInfo> recentlyAdded = new ArrayList<>();
        int maxRecent = Math.min(12, appsByInstallTime.size());
        for (int i = 0; i < maxRecent; i++) {
            recentlyAdded.add(appsByInstallTime.get(i));
        }
        map.put(AppCategory.RECENTLY_ADDED, recentlyAdded);

        List<AppCategoryGroup> groups = new ArrayList<>();
        for (AppCategory cat : AppCategory.values()) {
            List<AppInfo> appsInCat = map.get(cat);
            if (appsInCat != null && !appsInCat.isEmpty()) {
                groups.add(new AppCategoryGroup(cat, appsInCat));
            }
        }

        groups.sort(Comparator.comparingInt(g -> g.getCategory().getPriority()));
        mCategoryGroups = Collections.unmodifiableList(groups);
        notifyListeners();
    }

    private void notifyListeners() {
        Executors.MAIN_EXECUTOR.execute(() -> {
            for (AppLibraryModelListener listener : mListeners) {
                listener.onCategoriesUpdated(mCategoryGroups);
                listener.onAllAppsUpdated(mAllAppsList);
            }
        });
    }

    public List<AppCategoryGroup> getCategoryGroups() {
        return mCategoryGroups;
    }

    public List<AppInfo> getAllAppsList() {
        return mAllAppsList;
    }

    public List<AppInfo> getAllApps() {
        return mAllAppsList;
    }

    public List<AppInfo> filterApps(String query) {
        return searchApps(query);
    }

    public List<AppInfo> searchApps(String query) {
        if (TextUtils.isEmpty(query)) {
            return mAllAppsList;
        }

        String lowerQuery = query.toLowerCase(Locale.getDefault()).trim();
        List<AppInfo> results = new ArrayList<>();
        for (AppInfo app : mAllAppsList) {
            if (app.title != null && app.title.toString().toLowerCase(Locale.getDefault()).contains(lowerQuery)) {
                results.add(app);
            }
        }
        return results;
    }
}
