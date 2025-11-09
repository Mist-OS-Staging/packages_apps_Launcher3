package com.android.launcher3.applibrary;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SearchView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;
import com.android.launcher3.model.data.AppInfo;
import com.android.launcher3.util.PackageManagerHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppLibraryFragment extends Fragment {
    private LinearLayout categoriesContainer;
    private SearchView searchView;
    private Map<String, List<AppInfo>> categorizedApps;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.app_library_page, container, false);
        
        searchView = view.findViewById(R.id.search_view);
        categoriesContainer = view.findViewById(R.id.categories_container);
        
        setupSearchView();
        loadAndCategorizeApps();
        displayCategories();
        
        return view;
    }
    
    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
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
        categorizedApps = new HashMap<>();
        Context context = getContext();
        if (context == null) return;
        
        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        
        for (ApplicationInfo appInfo : installedApps) {
            // Skip system apps that shouldn't be shown
            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0 && 
                pm.getLaunchIntentForPackage(appInfo.packageName) == null) {
                continue;
            }
            
            String category = AppCategoryManager.categorizeApp(appInfo, pm);
            
            if (!categorizedApps.containsKey(category)) {
                categorizedApps.put(category, new ArrayList<>());
            }
            
            // Create AppInfo object (simplified)
            AppInfo app = new AppInfo();
            app.title = appInfo.loadLabel(pm);
            app.componentName = new android.content.ComponentName(appInfo.packageName, "");
            
            categorizedApps.get(category).add(app);
        }
    }
    
    private void displayCategories() {
        categoriesContainer.removeAllViews();
        
        // Add "Recently Added" section first
        addRecentlyAddedSection();
        
        // Add category cards
        for (Map.Entry<String, List<AppInfo>> entry : categorizedApps.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                AppLibraryCardView cardView = new AppLibraryCardView(getContext());
                cardView.setCategory(entry.getKey(), entry.getValue());
                
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(16, 16, 16, 16);
                cardView.setLayoutParams(params);
                
                categoriesContainer.addView(cardView);
            }
        }
    }
    
    private void addRecentlyAddedSection() {
        // TODO: Implement recently added apps logic
        // This would track installation dates and show newest apps
    }
    
    private void filterApps(String query) {
        categoriesContainer.removeAllViews();
        
        for (Map.Entry<String, List<AppInfo>> entry : categorizedApps.entrySet()) {
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
                
                categoriesContainer.addView(cardView);
            }
        }
    }
}
