package com.android.launcher3.applibrary;

import static com.android.launcher3.LauncherState.NORMAL;

import android.view.MotionEvent;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.touch.AbstractStateChangeTouchController;
import com.android.launcher3.touch.SingleAxisSwipeDetector;

public class AppLibraryTouchController extends AbstractStateChangeTouchController {
    
    public AppLibraryTouchController(Launcher launcher) {
        super(launcher, SingleAxisSwipeDetector.HORIZONTAL);
    }
    
    @Override
    protected boolean canInterceptTouch(MotionEvent ev) {
        if (!AppLibraryPrefs.isAppLibraryEnabled(mLauncher)) {
            return false;
        }
        
        if (mCurrentAnimation != null) {
            return true;
        }
        
        if (!mLauncher.isInState(NORMAL)) {
            return false;
        }
        
        // Only intercept swipes from the right edge
        return ev.getX() > mLauncher.getDeviceProfile().widthPx * 0.9f;
    }
    
    @Override
    protected LauncherState getTargetState(LauncherState fromState, boolean isDragTowardPositive) {
        if (fromState == NORMAL && isDragTowardPositive) {
            // Show App Library floating view instead of state transition
            mLauncher.showAppLibrary();
            return NORMAL; // Stay in normal state
        }
        return fromState;
    }
    
    @Override
    protected float initCurrentAnimation() {
        // No animation needed since we're showing a floating view
        return 0f;
    }
    
    private float getShiftRange() {
        return mLauncher.getDeviceProfile().widthPx;
    }
}
