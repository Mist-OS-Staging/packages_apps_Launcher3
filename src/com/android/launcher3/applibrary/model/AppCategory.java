package com.android.launcher3.applibrary.model;

import androidx.annotation.StringRes;
import com.android.launcher3.R;

public enum AppCategory {
    SUGGESTIONS(R.string.app_category_suggestions, 0),
    RECENTLY_ADDED(R.string.app_category_recently_added, 1),
    SOCIAL(R.string.app_category_social, 2),
    COMMUNICATION(R.string.app_category_communication, 3),
    PRODUCTIVITY(R.string.app_category_productivity, 4),
    FINANCE(R.string.app_category_finance, 5),
    SHOPPING(R.string.app_category_shopping, 6),
    ENTERTAINMENT(R.string.app_category_entertainment, 7),
    GAMES(R.string.app_category_games, 8),
    PHOTOGRAPHY(R.string.app_category_photography, 9),
    VIDEO(R.string.app_category_video, 10),
    MUSIC(R.string.app_category_music, 11),
    TRAVEL(R.string.app_category_travel, 12),
    NAVIGATION(R.string.app_category_navigation, 13),
    EDUCATION(R.string.app_category_education, 14),
    HEALTH_FITNESS(R.string.app_category_health_fitness, 15),
    UTILITIES(R.string.app_category_utilities, 16),
    TOOLS(R.string.app_category_tools, 17),
    LIFESTYLE(R.string.app_category_lifestyle, 18),
    NEWS(R.string.app_category_news, 19),
    BOOKS(R.string.app_category_books, 20),
    FOOD(R.string.app_category_food, 21),
    OTHER(R.string.app_category_other, 22);

    @StringRes
    private final int mTitleRes;
    private final int mPriority;

    AppCategory(@StringRes int titleRes, int priority) {
        mTitleRes = titleRes;
        mPriority = priority;
    }

    @StringRes
    public int getTitleRes() {
        return mTitleRes;
    }

    public int getPriority() {
        return mPriority;
    }
}
