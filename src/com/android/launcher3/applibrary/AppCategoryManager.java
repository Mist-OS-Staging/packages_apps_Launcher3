package com.android.launcher3.applibrary;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.HashMap;
import java.util.Map;

public class AppCategoryManager {
    private static final Map<String, String> KEYWORD_CATEGORIES = new HashMap<>();
    
    static {
        // Social
        KEYWORD_CATEGORIES.put("social", "Social");
        KEYWORD_CATEGORIES.put("chat", "Social");
        KEYWORD_CATEGORIES.put("message", "Social");
        KEYWORD_CATEGORIES.put("instagram", "Social");
        KEYWORD_CATEGORIES.put("telegram", "Social");
        KEYWORD_CATEGORIES.put("whatsapp", "Social");
        KEYWORD_CATEGORIES.put("twitter", "Social");
        KEYWORD_CATEGORIES.put("facebook", "Social");
        
        // Finance
        KEYWORD_CATEGORIES.put("bank", "Finance");
        KEYWORD_CATEGORIES.put("pay", "Finance");
        KEYWORD_CATEGORIES.put("wallet", "Finance");
        KEYWORD_CATEGORIES.put("finance", "Finance");
        KEYWORD_CATEGORIES.put("paytm", "Finance");
        KEYWORD_CATEGORIES.put("gpay", "Finance");
        KEYWORD_CATEGORIES.put("phonepe", "Finance");
        KEYWORD_CATEGORIES.put("paypal", "Finance");
        
        // Productivity
        KEYWORD_CATEGORIES.put("office", "Productivity");
        KEYWORD_CATEGORIES.put("docs", "Productivity");
        KEYWORD_CATEGORIES.put("drive", "Productivity");
        KEYWORD_CATEGORIES.put("slack", "Productivity");
        KEYWORD_CATEGORIES.put("notion", "Productivity");
        KEYWORD_CATEGORIES.put("calendar", "Productivity");
        
        // Entertainment
        KEYWORD_CATEGORIES.put("music", "Entertainment");
        KEYWORD_CATEGORIES.put("video", "Entertainment");
        KEYWORD_CATEGORIES.put("youtube", "Entertainment");
        KEYWORD_CATEGORIES.put("netflix", "Entertainment");
        KEYWORD_CATEGORIES.put("spotify", "Entertainment");
        
        // Shopping
        KEYWORD_CATEGORIES.put("shop", "Shopping");
        KEYWORD_CATEGORIES.put("amazon", "Shopping");
        KEYWORD_CATEGORIES.put("flipkart", "Shopping");
        KEYWORD_CATEGORIES.put("store", "Shopping");
        
        // Games
        KEYWORD_CATEGORIES.put("game", "Games");
        KEYWORD_CATEGORIES.put("play", "Games");
        
        // Tools
        KEYWORD_CATEGORIES.put("tool", "Tools");
        KEYWORD_CATEGORIES.put("utility", "Tools");
        KEYWORD_CATEGORIES.put("calculator", "Tools");
        KEYWORD_CATEGORIES.put("file", "Tools");
    }
    
    public static String categorizeApp(ApplicationInfo appInfo, PackageManager pm) {
        String appName = appInfo.loadLabel(pm).toString().toLowerCase();
        String packageName = appInfo.packageName.toLowerCase();
        
        // Check ApplicationInfo.category first
        int category = appInfo.category;
        switch (category) {
            case ApplicationInfo.CATEGORY_SOCIAL:
                return "Social";
            case ApplicationInfo.CATEGORY_PRODUCTIVITY:
                return "Productivity";
            case ApplicationInfo.CATEGORY_GAME:
                return "Games";
            case ApplicationInfo.CATEGORY_AUDIO:
            case ApplicationInfo.CATEGORY_VIDEO:
                return "Entertainment";
            default:
                break;
        }
        
        // Check keywords in app name and package name
        for (Map.Entry<String, String> entry : KEYWORD_CATEGORIES.entrySet()) {
            if (appName.contains(entry.getKey()) || packageName.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        return "Others";
    }
}
