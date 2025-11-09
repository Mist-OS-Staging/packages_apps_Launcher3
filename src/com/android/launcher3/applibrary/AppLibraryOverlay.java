package com.android.launcher3.applibrary;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.model.data.AppInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppLibraryOverlay extends AbstractFloatingView {
    private SearchView mSearchView;
    private LinearLayout mCategoriesContainer;
    private AppLibrary mAppLibrary;
    private Map<String, List<AppInfo>> mAllCategories;
    
    public AppLibraryOverlay(Context context) {
        super(context, null);
        init();
    }
    
    private void init() {
        setBackgroundColor(Color.parseColor("#E6FFFFFF"));
        
        LinearLayout mainLayout = new LinearLayout(getContext());
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 64, 32, 32);
        
        // Search
        mSearchView = new SearchView(getContext());
        mSearchView.setQueryHint("Search apps");
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            
            @Override
            public boolean onQueryTextChange(String newText) {
                filterApps(newText);
                return true;
            }
        });
        
        // Categories container
        mCategoriesContainer = new LinearLayout(getContext());
        mCategoriesContainer.setOrientation(LinearLayout.VERTICAL);
        
        mainLayout.addView(mSearchView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mainLayout.addView(mCategoriesContainer, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        addView(mainLayout);
        
        // Load apps
        Launcher launcher = Launcher.getLauncher(getContext());
        mAppLibrary = AppLibrary.getInstance(getContext(), launcher.getIconCache());
        mAppLibrary.loadApps(categories -> {
            mAllCategories = categories;
            displayCategories(categories);
        });
    }
    
    private void displayCategories(Map<String, List<AppInfo>> categories) {
        mCategoriesContainer.removeAllViews();
        
        for (Map.Entry<String, List<AppInfo>> entry : categories.entrySet()) {
            if (entry.getValue().isEmpty()) continue;
            
            View categoryView = createCategoryView(entry.getKey(), entry.getValue());
            mCategoriesContainer.addView(categoryView);
        }
    }
    
    private View createCategoryView(String categoryName, List<AppInfo> apps) {
        LinearLayout categoryLayout = new LinearLayout(getContext());
        categoryLayout.setOrientation(LinearLayout.VERTICAL);
        categoryLayout.setPadding(16, 16, 16, 16);
        categoryLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
        
        // Category title
        TextView titleView = new TextView(getContext());
        titleView.setText(categoryName);
        titleView.setTextSize(18);
        titleView.setTextColor(Color.BLACK);
        titleView.setPadding(0, 0, 0, 16);
        
        // Apps grid (2x2)
        GridView gridView = new GridView(getContext());
        gridView.setNumColumns(2);
        gridView.setAdapter(new AppGridAdapter(apps.subList(0, Math.min(4, apps.size()))));
        gridView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
        
        categoryLayout.addView(titleView);
        categoryLayout.addView(gridView);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        categoryLayout.setLayoutParams(params);
        
        return categoryLayout;
    }
    
    private void filterApps(String query) {
        if (mAllCategories == null) return;
        
        if (TextUtils.isEmpty(query)) {
            displayCategories(mAllCategories);
            return;
        }
        
        Map<String, List<AppInfo>> filtered = new java.util.HashMap<>();
        for (Map.Entry<String, List<AppInfo>> entry : mAllCategories.entrySet()) {
            List<AppInfo> matchingApps = new ArrayList<>();
            for (AppInfo app : entry.getValue()) {
                if (app.title.toString().toLowerCase().contains(query.toLowerCase())) {
                    matchingApps.add(app);
                }
            }
            if (!matchingApps.isEmpty()) {
                filtered.put(entry.getKey(), matchingApps);
            }
        }
        displayCategories(filtered);
    }
    
    private class AppGridAdapter extends BaseAdapter {
        private final List<AppInfo> mApps;
        
        AppGridAdapter(List<AppInfo> apps) {
            mApps = apps;
        }
        
        @Override
        public int getCount() { return mApps.size(); }
        
        @Override
        public Object getItem(int position) { return mApps.get(position); }
        
        @Override
        public long getItemId(int position) { return position; }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView = new ImageView(getContext());
            AppInfo app = mApps.get(position);
            
            if (app.iconBitmap != null) {
                imageView.setImageBitmap(app.iconBitmap.icon);
            }
            
            imageView.setLayoutParams(new GridView.LayoutParams(80, 80));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(8, 8, 8, 8);
            
            imageView.setOnClickListener(v -> {
                try {
                    Intent intent = getContext().getPackageManager()
                        .getLaunchIntentForPackage(app.componentName.getPackageName());
                    if (intent != null) {
                        getContext().startActivity(intent);
                        close(true);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            });
            
            return imageView;
        }
    }
    
    @Override
    protected void handleClose(boolean animate) {
        if (mIsOpen) {
            if (animate) {
                animate().alpha(0f).setDuration(200).withEndAction(() -> {
                    setVisibility(GONE);
                    mIsOpen = false;
                    ((ViewGroup) getParent()).removeView(this);
                }).start();
            } else {
                setVisibility(GONE);
                mIsOpen = false;
                ((ViewGroup) getParent()).removeView(this);
            }
        }
    }
    
    @Override
    public void logActionCommand(int command) {}
    
    @Override
    protected boolean isOfType(int type) {
        return (type & TYPE_ACCESSIBLE) != 0;
    }
    
    public static void show(Launcher launcher) {
        if (!AppLibraryPrefs.isAppLibraryEnabled(launcher)) return;
        
        AppLibraryOverlay overlay = new AppLibraryOverlay(launcher);
        launcher.getDragLayer().addView(overlay, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        
        overlay.mIsOpen = true;
        overlay.setAlpha(0f);
        overlay.animate().alpha(1f).setDuration(200).start();
        
        overlay.setOnClickListener(v -> overlay.close(true));
    }
}
