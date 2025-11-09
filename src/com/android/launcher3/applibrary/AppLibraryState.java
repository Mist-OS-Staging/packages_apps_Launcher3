package com.android.launcher3.applibrary;

import static com.android.launcher3.anim.Interpolators.DECELERATE_2;

import android.content.Context;
import android.graphics.Rect;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.R;

public class AppLibraryState extends LauncherState {
    
    private static final int STATE_FLAGS = FLAG_MULTI_PAGE | FLAG_DISABLE_ACCESSIBILITY;
    
    public AppLibraryState(int id) {
        super(id, LauncherState.NORMAL.containerType, LauncherState.NORMAL.transitionDuration, STATE_FLAGS);
    }
    
    @Override
    public String getDescription(Launcher launcher) {
        return "App Library";
    }
    
    @Override
    public int getVisibleElements(Launcher launcher) {
        return NONE;
    }
    
    @Override
    public float[] getWorkspaceScaleAndTranslation(Launcher launcher) {
        return new float[] {0.9f, 0, 0};
    }
    
    @Override
    public float getWorkspaceScrimAlpha(Launcher launcher) {
        return 0.5f;
    }
    
    @Override
    public Rect getInsets(Launcher launcher, DeviceProfile dp) {
        return dp.getInsets();
    }
    
    @Override
    public PageAlphaProvider getWorkspacePageAlphaProvider(Launcher launcher) {
        return new PageAlphaProvider(DECELERATE_2) {
            @Override
            public float getPageAlpha(int pageIndex) {
                return 0;
            }
        };
    }
    
    @Override
    public int getWorkspaceScrimColor(Launcher launcher) {
        return 0x80000000; // Semi-transparent black
    }
    
    public static final LauncherState APP_LIBRARY = new AppLibraryState(6);
}
