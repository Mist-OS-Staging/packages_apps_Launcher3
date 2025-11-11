package com.android.launcher3.applibrary;

import static com.android.app.animation.Interpolators.DECELERATE_2;
import static com.android.launcher3.logging.StatsLogManager.LAUNCHER_STATE_HOME;

import android.content.Context;
import android.graphics.Rect;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;
import com.android.launcher3.statemanager.StateManager;
import com.android.launcher3.views.ActivityContext;

public class AppLibraryState extends LauncherState {
    
    private static final int STATE_FLAGS = FLAG_MULTI_PAGE;
    
    public AppLibraryState(int id) {
        super(id, LAUNCHER_STATE_HOME, STATE_FLAGS);
    }
    
    public int getTransitionDuration(ActivityContext context, boolean isToState) {
        return 300;
    }
    
    public ScaleAndTranslation getWorkspaceScaleAndTranslation(Launcher launcher) {
        return new ScaleAndTranslation(0.9f, 0f, 0f);
    }
    
    public ScaleAndTranslation getHotseatScaleAndTranslation(Launcher launcher) {
        return new ScaleAndTranslation(0.9f, 0f, 0f);
    }
    
    public float getWorkspaceBackgroundAlpha(Launcher launcher) {
        return 0.5f;
    }
    
    public PageAlphaProvider getWorkspacePageAlphaProvider(Launcher launcher) {
        return new PageAlphaProvider(DECELERATE_2) {
            @Override
            public float getPageAlpha(int pageIndex) {
                return 0.5f;
            }
        };
    }
}
