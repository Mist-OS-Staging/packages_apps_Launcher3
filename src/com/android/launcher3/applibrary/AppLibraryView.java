package com.android.launcher3.applibrary;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SearchView;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.model.data.AppInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppLibraryView extends AbstractFloatingView {
    
    private LinearLayout mCategoriesContainer;
    private SearchView mSearchView;
    private Map<String, List<AppInfo>> mCategorizedApps;
    
    public AppLibraryView(Context context) {
        this(context, null);
    }
    
    public AppLibraryView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public AppLibraryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.app_library_page, this, true);
        
        mSearchView = findViewById(R.id.search_view);
        mCategoriesContainer = findViewById(R.id.categories_container);
        
        setupSearchView();
        loadAndCategorizeApps();
        displayCategories();
    }
    
    private void setupSearchView() {
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterApps(query);
                return true;
            }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    displayCategories();
                } else {
                    filterApps(newText);
                }
                return true;
            }
        });
    }
    
    private void loadAndCategorizeApps() {
        mCategorizedApps = new HashMap<>();
        Context context = getContext();
        
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        
        for (ApplicationInfo appInfo : installedApps) {
            // Skip system apps that shouldn't be shown
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && 
                pm.getLaunchIntentForPackage(appInfo.packageName) == null) {
                continue;
            }
            
            String category = AppCategoryManager.categorizeApp(appInfo, pm);
            
            if (!mCategorizedApps.containsKey(category)) {
                mCategorizedApps.put(category, new ArrayList<>());
            }
            
            // Create simplified AppInfo
            AppInfo app = new AppInfo();
            app.title = appInfo.loadLabel(pm);
            app.componentName = new android.content.ComponentName(appInfo.packageName, "");
            
            mCategorizedApps.get(category).add(app);
        }
    }
    
    private void displayCategories() {
        mCategoriesContainer.removeAllViews();
        
        for (Map.Entry<String, List<AppInfo>> entry : mCategorizedApps.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                AppLibraryCardView cardView = new AppLibraryCardView(getContext());
                cardView.setCategory(entry.getKey(), entry.getValue());
                
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(16, 16, 16, 16);
                cardView.setLayoutParams(params);
                
                mCategoriesContainer.addView(cardView);
            }
        }
    }
    
    private void filterApps(String query) {
        mCategoriesContainer.removeAllViews();
        
        for (Map.Entry<String, List<AppInfo>> entry : mCategorizedApps.entrySet()) {
            List<AppInfo> filteredApps = new ArrayList<>();
            
            for (AppInfo app : entry.getValue()) {
                if (app.title.toString().toLowerCase().contains(query.toLowerCase())) {
                    filteredApps.add(app);
                }
            }
            
            if (!filteredApps.isEmpty()) {
                AppLibraryCardView cardView = new AppLibraryCardView(getContext());
                cardView.setCategory(entry.getKey(), filteredApps);
                
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(16, 16, 16, 16);
                cardView.setLayoutParams(params);
                
                mCategoriesContainer.addView(cardView);
            }
        }
    }
    
    @Override
    protected void handleClose(boolean animate) {
        if (mIsOpen) {
            if (animate) {
                animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    setVisibility(GONE);
                    mIsOpen = false;
                }).start();
            } else {
                setVisibility(GONE);
                mIsOpen = false;
            }
        }
    }
    
    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        return false;
    }
    
    public void logActionCommand(int command) {
        // Log action if needed
    }
    
    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_ACCESSIBLE) != 0;
    }
    
    public static AppLibraryView show(Launcher launcher) {
        AppLibraryView view = new AppLibraryView(launcher);
        launcher.getDragLayer().addView(view);
        view.mIsOpen = true;
        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(200).start();
        return view;
    }
}
