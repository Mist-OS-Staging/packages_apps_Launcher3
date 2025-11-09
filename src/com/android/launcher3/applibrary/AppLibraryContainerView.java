package com.android.launcher3.applibrary;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.launcher3.R;

public class AppLibraryContainerView extends FrameLayout {
    
    private AppLibraryFragment mFragment;
    
    public AppLibraryContainerView(Context context) {
        this(context, null);
    }
    
    public AppLibraryContainerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public AppLibraryContainerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.app_library_container, this, true);
        setupFragment();
    }
    
    private void setupFragment() {
        if (getContext() instanceof FragmentActivity) {
            FragmentActivity activity = (FragmentActivity) getContext();
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            
            mFragment = new AppLibraryFragment();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.fragment_container, mFragment);
            transaction.commit();
        }
    }
    
    public void refresh() {
        if (mFragment != null) {
            // Refresh the fragment content
            FragmentTransaction transaction = ((FragmentActivity) getContext())
                    .getSupportFragmentManager().beginTransaction();
            transaction.detach(mFragment);
            transaction.attach(mFragment);
            transaction.commit();
        }
    }
}
