package com.android.launcher3.applibrary;

import static com.android.app.animation.Interpolators.DECELERATE_2;

import android.content.Context;
import android.graphics.Rect;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.statemanager.StateManager;
import com.android.launcher3.util.ActivityContextWrapper;

public class AppLibraryState extends LauncherState {
    
    private static final int STATE_FLAGS = FLAG_MULTI_PAGE;
    
    public AppLibraryState(int id) {
        super(id, NORMAL.containerType, NORMAL.transitionDuration, STATE_FLAGS);
    }
    
    @Override
    public int getTransitionDuration(Context context, boolean isToState) {
        return 300;
    }
    
    @Override
    public ScaleAndTranslation getWorkspaceScaleAndTranslation(Launcher launcher) {
        return new ScaleAndTranslation(0.9f, 0f, 0f);
    }
    
    @Override
    public ScaleAndTranslation getHotseatScaleAndTranslation(Launcher launcher) {
        return new ScaleAndTranslation(0.9f, 0f, 0f);
    }
    
    @Override
    public float getWorkspaceBackgroundAlpha(Launcher launcher) {
        return 0.5f;
    }
    
    @Override
    public PageAlphaProvider getWorkspacePageAlphaProvider(Launcher launcher) {
        return new PageAlphaProvider(DECELERATE_2) {
            @Override
            public float getPageAlpha(int pageIndex) {
                return 0.5f;
            }
        };
    }
}
