package com.android.launcher3.applibrary;

import android.view.MotionEvent;
import com.android.launcher3.Launcher;
import com.android.launcher3.util.TouchController;

public class AppLibraryGestureHandler implements TouchController {
    private final Launcher mLauncher;
    private float mDownX;
    private boolean mIsTracking;
    
    public AppLibraryGestureHandler(Launcher launcher) {
        mLauncher = launcher;
    }
    
    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent ev) {
        if (!AppLibraryPrefs.isAppLibraryEnabled(mLauncher)) {
            return false;
        }
        
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = ev.getX();
                // Only intercept if starting from right edge
                mIsTracking = mDownX > mLauncher.getDeviceProfile().widthPx * 0.85f;
                return mIsTracking;
        }
        return false;
    }
    
    @Override
    public boolean onControllerTouchEvent(MotionEvent ev) {
        if (!mIsTracking) return false;
        
        switch (ev.getAction()) {
            case MotionEvent.ACTION_UP:
                float deltaX = ev.getX() - mDownX;
                if (deltaX < -100) { // Swipe left threshold
                    AppLibraryOverlay.show(mLauncher);
                }
                mIsTracking = false;
                return true;
                
            case MotionEvent.ACTION_CANCEL:
                mIsTracking = false;
                return true;
        }
        return true;
    }
}
