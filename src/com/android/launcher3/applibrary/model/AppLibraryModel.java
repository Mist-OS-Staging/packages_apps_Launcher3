package com.android.launcher3.applibrary.model;

import android.content.Context;
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
    }

    @Override
    public void onAppsUpdated() {
        AppCategoryEngine.clearCache();
        rebuildCategories();
    }

    public void forceRefresh() {
        rebuildCategories();
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

        List<AppInfo> recentlyAdded = new ArrayList<>();
        int maxRecent = Math.min(4, allApps.size());
        for (int i = allApps.size() - 1; i >= allApps.size() - maxRecent && i >= 0; i--) {
            recentlyAdded.add(allApps.get(i));
        }
        if (allApps.size() > 4) {
            map.put(AppCategory.RECENTLY_ADDED, recentlyAdded);
        } else {
            map.put(AppCategory.RECENTLY_ADDED, new ArrayList<>());
        }

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

    @NonNull
    public List<AppCategoryGroup> getCategoryGroups() {
        return mCategoryGroups;
    }

    @NonNull
    public List<AppInfo> getAllApps() {
        return mAllAppsList;
    }

    @NonNull
    public List<AppInfo> searchApps(@Nullable String query) {
        if (TextUtils.isEmpty(query)) {
            return mAllAppsList;
        }

        String lowerQuery = query.toLowerCase(Locale.ROOT).trim();
        List<AppInfo> results = new ArrayList<>();
        for (AppInfo app : mAllAppsList) {
            if (app == null) continue;
            String title = app.title != null ? app.title.toString().toLowerCase(Locale.ROOT) : "";
            String pkg = app.componentName != null ? app.componentName.getPackageName().toLowerCase(Locale.ROOT) : "";
            if (title.contains(lowerQuery) || pkg.contains(lowerQuery)) {
                results.add(app);
            }
        }
        return results;
    }
}
