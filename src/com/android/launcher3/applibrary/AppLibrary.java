package com.android.launcher3.applibrary;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;

import com.android.launcher3.icons.IconCache;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.util.ComponentKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AppLibrary {
    private static final String TAG = "AppLibrary";
    private static AppLibrary sInstance;
    
    private final Context mContext;
    private final PackageManager mPm;
    private final IconCache mIconCache;
    private final Map<String, List<AppInfo>> mCategories = new ConcurrentHashMap<>();
    private final Map<String, String> mKeywords = new HashMap<>();
    
    private AppLibrary(Context context, IconCache iconCache) {
        mContext = context.getApplicationContext();
        mPm = mContext.getPackageManager();
        mIconCache = iconCache;
        initKeywords();
    }
    
    public static AppLibrary getInstance(Context context, IconCache iconCache) {
        if (sInstance == null) {
            sInstance = new AppLibrary(context, iconCache);
        }
        return sInstance;
    }
    
    private void initKeywords() {
        mKeywords.put("social", "Social");
        mKeywords.put("chat", "Social");
        mKeywords.put("instagram", "Social");
        mKeywords.put("whatsapp", "Social");
        mKeywords.put("telegram", "Social");
        mKeywords.put("twitter", "Social");
        mKeywords.put("facebook", "Social");
        
        mKeywords.put("bank", "Finance");
        mKeywords.put("pay", "Finance");
        mKeywords.put("paytm", "Finance");
        mKeywords.put("gpay", "Finance");
        mKeywords.put("phonepe", "Finance");
        
        mKeywords.put("music", "Entertainment");
        mKeywords.put("video", "Entertainment");
        mKeywords.put("youtube", "Entertainment");
        mKeywords.put("netflix", "Entertainment");
        mKeywords.put("spotify", "Entertainment");
        
        mKeywords.put("game", "Games");
        mKeywords.put("play", "Games");
        
        mKeywords.put("shop", "Shopping");
        mKeywords.put("amazon", "Shopping");
        mKeywords.put("flipkart", "Shopping");
    }
    
    public void loadApps(Callback callback) {
        new AsyncTask<Void, Void, Map<String, List<AppInfo>>>() {
            @Override
            protected Map<String, List<AppInfo>> doInBackground(Void... voids) {
                return loadAppsSync();
            }
            
            @Override
            protected void onPostExecute(Map<String, List<AppInfo>> result) {
                mCategories.clear();
                mCategories.putAll(result);
                callback.onAppsLoaded(result);
            }
        }.execute();
    }
    
    private Map<String, List<AppInfo>> loadAppsSync() {
        Map<String, List<AppInfo>> categories = new HashMap<>();
        
        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps = mPm.queryIntentActivities(mainIntent, 0);
            
            for (ResolveInfo info : apps) {
                try {
                    AppInfo appInfo = createAppInfo(info);
                    if (appInfo != null) {
                        String category = categorizeApp(info.activityInfo.applicationInfo);
                        categories.computeIfAbsent(category, k -> new ArrayList<>()).add(appInfo);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to process app: " + info.activityInfo.packageName, e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load apps", e);
        }
        
        return categories;
    }
    
    private AppInfo createAppInfo(ResolveInfo info) {
        try {
            AppInfo appInfo = new AppInfo();
            appInfo.componentName = new android.content.ComponentName(
                info.activityInfo.packageName, info.activityInfo.name);
            appInfo.title = info.loadLabel(mPm);
            appInfo.user = android.os.Process.myUserHandle();
            
            // Load icon using IconCache
            ComponentKey key = new ComponentKey(appInfo.componentName, appInfo.user);
            appInfo.iconBitmap = mIconCache.getIconBitmap(key);
            
            return appInfo;
        } catch (Exception e) {
            Log.w(TAG, "Failed to create AppInfo", e);
            return null;
        }
    }
    
    private String categorizeApp(ApplicationInfo appInfo) {
        String appName = appInfo.loadLabel(mPm).toString().toLowerCase();
        String packageName = appInfo.packageName.toLowerCase();
        
        // Check system category first
        switch (appInfo.category) {
            case ApplicationInfo.CATEGORY_SOCIAL:
                return "Social";
            case ApplicationInfo.CATEGORY_GAME:
                return "Games";
            case ApplicationInfo.CATEGORY_PRODUCTIVITY:
                return "Productivity";
            case ApplicationInfo.CATEGORY_AUDIO:
            case ApplicationInfo.CATEGORY_VIDEO:
                return "Entertainment";
        }
        
        // Check keywords
        for (Map.Entry<String, String> entry : mKeywords.entrySet()) {
            if (appName.contains(entry.getKey()) || packageName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return "Others";
    }
    
    public Map<String, List<AppInfo>> getCategories() {
        return new HashMap<>(mCategories);
    }
    
    public interface Callback {
        void onAppsLoaded(Map<String, List<AppInfo>> categories);
    }
}
