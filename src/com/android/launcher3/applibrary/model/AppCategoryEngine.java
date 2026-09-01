package com.android.launcher3.applibrary.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.util.ComponentKey;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public class AppCategoryEngine {

    private static final ConcurrentHashMap<ComponentKey, AppCategory> sCategoryCache =
            new ConcurrentHashMap<>();

    public static void clearCache() {
        sCategoryCache.clear();
    }

    public static AppCategory getCategoryForApp(Context context, AppInfo appInfo) {
        if (appInfo == null) {
            return AppCategory.OTHER;
        }

        ComponentKey key = new ComponentKey(appInfo.componentName, appInfo.user);
        AppCategory cached = sCategoryCache.get(key);
        if (cached != null) {
            return cached;
        }

        AppCategory resolved = resolveCategory(context, appInfo);
        sCategoryCache.put(key, resolved);
        return resolved;
    }

    private static AppCategory resolveCategory(Context context, AppInfo appInfo) {
        if (appInfo == null || appInfo.componentName == null) {
            return AppCategory.OTHER;
        }

        String packageName = appInfo.componentName.getPackageName();
        if (context != null && !TextUtils.isEmpty(packageName)) {
            try {
                PackageManager pm = context.getPackageManager();
                ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);
                if (ai != null) {
                    if ((ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0) {
                        return AppCategory.GAMES;
                    }
                    int category = ai.category;
                    switch (category) {
                        case ApplicationInfo.CATEGORY_GAME:
                            return AppCategory.GAMES;
                        case ApplicationInfo.CATEGORY_AUDIO:
                            return AppCategory.MUSIC;
                        case ApplicationInfo.CATEGORY_VIDEO:
                            return AppCategory.VIDEO;
                        case ApplicationInfo.CATEGORY_IMAGE:
                            return AppCategory.PHOTOGRAPHY;
                        case ApplicationInfo.CATEGORY_SOCIAL:
                            return AppCategory.SOCIAL;
                        case ApplicationInfo.CATEGORY_NEWS:
                            return AppCategory.NEWS;
                        case ApplicationInfo.CATEGORY_MAPS:
                            return AppCategory.NAVIGATION;
                        case ApplicationInfo.CATEGORY_PRODUCTIVITY:
                            return AppCategory.PRODUCTIVITY;
                        case ApplicationInfo.CATEGORY_ACCESSIBILITY:
                            return AppCategory.TOOLS;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        String target = buildSearchString(appInfo);
        return classifyByKeywords(target);
    }

    private static String buildSearchString(AppInfo appInfo) {
        StringBuilder sb = new StringBuilder();
        if (appInfo.title != null) {
            sb.append(appInfo.title).append(" ");
        }
        if (appInfo.componentName != null) {
            sb.append(appInfo.componentName.getPackageName()).append(" ");
            sb.append(appInfo.componentName.getClassName()).append(" ");
        }
        return sb.toString().toLowerCase(Locale.ROOT);
    }

    private static AppCategory classifyByKeywords(String text) {
        if (containsAny(text, "game", "arcade", "puzzle", "action", "rpg", "steam", "epicgames",
                "roblox", "minecraft", "chess", "poker", "casino", "shooter", "quest", "play.games")) {
            return AppCategory.GAMES;
        }

        if (containsAny(text, "dialer", "phone", "messaging", "message", "sms", "mms", "contacts",
                "call", "talk", "chat", "email", "mail", "gmail", "outlook", "zoom", "teams",
                "meet", "whatsapp", "signal", "viber", "skype", "telegram", "messenger")) {
            return AppCategory.COMMUNICATION;
        }

        if (containsAny(text, "facebook", "instagram", "twitter", "tiktok", "snapchat", "reddit",
                "linkedin", "discord", "threads", "wechat", "mastodon", "bluesky", "pinterest",
                "tumblr", "social", "community", "feed", "post")) {
            return AppCategory.SOCIAL;
        }

        if (containsAny(text, "bank", "wallet", "pay", "paypal", "venmo", "cash", "crypto",
                "bitcoin", "binance", "coinbase", "invest", "stock", "trade", "robinhood",
                "tax", "mint", "credit", "finance", "money", "banking", "ledger")) {
            return AppCategory.FINANCE;
        }

        if (containsAny(text, "shop", "store", "amazon", "ebay", "walmart", "target", "aliexpress",
                "temu", "etsy", "buy", "cart", "market", "deal", "coupon", "retail", "flipkart",
                "shein", "shopping")) {
            return AppCategory.SHOPPING;
        }

        if (containsAny(text, "netflix", "hulu", "disney", "hbo", "max", "primevideo", "tv",
                "movie", "stream", "cinema", "theater", "twitch", "plex", "crunchyroll", "imdb",
                "entertainment", "media")) {
            return AppCategory.ENTERTAINMENT;
        }

        if (containsAny(text, "camera", "photo", "gallery", "image", "picture", "snapseed",
                "lightroom", "vsco", "photoshop", "filter", "retouch", "lens", "capture")) {
            return AppCategory.PHOTOGRAPHY;
        }

        if (containsAny(text, "video", "player", "vlc", "youtube", "capcut", "inshot",
                "recorder", "broadcast", "studio", "editor", "clip", "cut")) {
            return AppCategory.VIDEO;
        }

        if (containsAny(text, "music", "audio", "spotify", "soundcloud", "deezer", "pandora",
                "tidal", "shazam", "podcast", "radio", "tunein", "bandcamp", "equalizer",
                "sound", "tune", "song", "track")) {
            return AppCategory.MUSIC;
        }

        if (containsAny(text, "travel", "trip", "hotel", "flight", "airline", "booking",
                "airbnb", "expedia", "kayak", "uber", "lyft", "taxi", "grab", "bolt",
                "vacation", "journey", "ticket")) {
            return AppCategory.TRAVEL;
        }

        if (containsAny(text, "nav", "map", "maps", "gps", "waze", "transit", "commute",
                "direction", "route", "compass", "speedometer", "location")) {
            return AppCategory.NAVIGATION;
        }

        if (containsAny(text, "edu", "learn", "school", "college", "course", "study",
                "duolingo", "khan", "coursera", "udemy", "quizlet", "dictionary", "translate",
                "math", "science", "academy", "lesson", "teach")) {
            return AppCategory.EDUCATION;
        }

        if (containsAny(text, "health", "fit", "fitness", "gym", "workout", "run", "running",
                "nike", "strava", "diet", "calorie", "step", "pedometer", "yoga", "sleep",
                "meditation", "headspace", "calm", "myfitnesspal", "garmin", "fitbit",
                "wellness", "heart", "pulse", "training")) {
            return AppCategory.HEALTH_FITNESS;
        }

        if (containsAny(text, "docs", "sheets", "slides", "word", "excel", "powerpoint",
                "office", "drive", "dropbox", "notion", "evernote", "keep", "trello", "asana",
                "todoist", "tasks", "calendar", "agenda", "scanner", "pdf", "acrobat",
                "work", "project", "kanban", "note", "notes", "memo")) {
            return AppCategory.PRODUCTIVITY;
        }

        if (containsAny(text, "clock", "alarm", "timer", "stopwatch", "calculator", "weather",
                "flashlight", "files", "filemanager", "archive", "zip", "recorder", "voice",
                "utility", "calculator", "torch")) {
            return AppCategory.UTILITIES;
        }

        if (containsAny(text, "tool", "setting", "system", "security", "antivirus", "cleaner",
                "terminal", "termux", "benchmark", "speedtest", "cpu", "sensor", "wifi",
                "bluetooth", "network", "analyzer", "developer", "settings", "installer",
                "package", "admin", "debug")) {
            return AppCategory.TOOLS;
        }

        if (containsAny(text, "news", "bbc", "cnn", "reuters", "times", "journal", "daily",
                "paper", "press", "feed", "rss", "magazine", "medium", "flipboard", "article")) {
            return AppCategory.NEWS;
        }

        if (containsAny(text, "book", "reader", "kindle", "novel", "comic", "manga",
                "epub", "library", "audible", "wattpad", "webtoon", "story", "stories")) {
            return AppCategory.BOOKS;
        }

        if (containsAny(text, "food", "drink", "recipe", "cook", "cooking", "kitchen",
                "chef", "pizza", "burger", "coffee", "starbucks", "restaurant", "order",
                "delivery", "doordash", "ubereats", "grubhub", "zomato", "swiggy", "yelp",
                "cafe", "bake", "bakery", "dining", "menu")) {
            return AppCategory.FOOD;
        }

        if (containsAny(text, "home", "house", "smart", "iot", "style", "fashion", "beauty",
                "makeup", "hair", "baby", "pet", "dog", "cat", "astro", "horoscope", "decor",
                "interior", "lifestyle", "garden", "plant")) {
            return AppCategory.LIFESTYLE;
        }

        return AppCategory.OTHER;
    }

    private static boolean containsAny(String text, String... keywords) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
