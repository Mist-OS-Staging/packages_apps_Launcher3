package com.android.launcher3.applibrary;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.launcher3.R;
import com.android.launcher3.model.data.AppInfo;

import java.util.List;

public class AppLibraryCardView extends LinearLayout {
    private TextView mCategoryTitle;
    private GridView mAppsGrid;
    
    public AppLibraryCardView(Context context) {
        super(context);
        init();
    }
    
    public AppLibraryCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        setOrientation(VERTICAL);
        setPadding(16, 16, 16, 16);
        setBackgroundResource(R.drawable.app_library_card_background);
        
        mCategoryTitle = new TextView(getContext());
        mCategoryTitle.setTextSize(18);
        mCategoryTitle.setTextColor(0xFF000000);
        mCategoryTitle.setPadding(0, 0, 0, 16);
        
        mAppsGrid = new GridView(getContext());
        mAppsGrid.setNumColumns(2);
        mAppsGrid.setLayoutParams(new LayoutParams(200, 200));
        
        addView(mCategoryTitle);
        addView(mAppsGrid);
    }
    
    public void setCategory(String categoryName, List<AppInfo> apps) {
        mCategoryTitle.setText(categoryName);
        mAppsGrid.setAdapter(new AppGridAdapter(apps.subList(0, Math.min(4, apps.size()))));
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
            
            if (app.bitmap != null) {
                imageView.setImageDrawable(app.bitmap.newIcon(getContext()));
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
                    }
                } catch (Exception e) {
                    // Ignore
                }
            });
            
            return imageView;
        }
    }
}
