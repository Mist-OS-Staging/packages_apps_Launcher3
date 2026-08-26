package com.android.launcher3.applibrary;

import static com.android.launcher3.AbstractFloatingView.TYPE_ALL;
import static com.android.launcher3.AbstractFloatingView.getTopOpenViewWithType;
import static com.android.launcher3.LauncherState.APP_LIBRARY;
import static com.android.launcher3.LauncherState.NORMAL;

import android.view.MotionEvent;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.LauncherState;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.states.StateAnimationConfig;
import com.android.launcher3.touch.AbstractStateChangeTouchController;
import com.android.launcher3.touch.SingleAxisSwipeDetector;

public class AppLibraryTouchController extends AbstractStateChangeTouchController {

    public AppLibraryTouchController(Launcher launcher) {
        super(launcher, SingleAxisSwipeDetector.HORIZONTAL);
    }

    private boolean isAppLibraryEnabled() {
        return LauncherPrefs.get(mLauncher).get(LauncherPrefs.APP_LIBRARY_ENABLED);
    }

    private boolean isSinglePageMode() {
        return LauncherPrefs.get(mLauncher).get(LauncherPrefs.APP_LIBRARY_SINGLE_PAGE);
    }

    @Override
    protected boolean canInterceptTouch(MotionEvent ev) {
        if (!isAppLibraryEnabled()) {
            return false;
        }

        if (mCurrentAnimation != null) {
            return true;
        }

        if (getTopOpenViewWithType(mLauncher, TYPE_ALL) != null) {
            return false;
        }

        if (mLauncher.isInState(NORMAL)) {
            Workspace<?> workspace = mLauncher.getWorkspace();
            if (workspace == null) {
                return false;
            }
            if (workspace.isHandlingTouch()) {
                return false;
            }
            if (isSinglePageMode()) {
                return true;
            }
            int currentPage = workspace.getNextPage();
            int pageCount = workspace.getPageCount();
            return currentPage >= pageCount - 1;
        } else if (mLauncher.isInState(APP_LIBRARY)) {
            return true;
        }

        return false;
    }

    @Override
    protected LauncherState getTargetState(LauncherState fromState, boolean isDragTowardPositive) {
        if (!isAppLibraryEnabled()) {
            return fromState;
        }

        boolean isRtl = Utilities.isRtl(mLauncher.getResources());
        boolean isTowardAppLibrary = isRtl ? isDragTowardPositive : !isDragTowardPositive;
        boolean isTowardWorkspace = isRtl ? !isDragTowardPositive : isDragTowardPositive;

        if (fromState == NORMAL && isTowardAppLibrary) {
            return APP_LIBRARY;
        } else if (fromState == APP_LIBRARY && isTowardWorkspace) {
            return NORMAL;
        }

        return fromState;
    }

    @Override
    protected StateAnimationConfig getConfigForStates(LauncherState fromState, LauncherState toState) {
        StateAnimationConfig config = new StateAnimationConfig();
        config.animProps |= StateAnimationConfig.USER_CONTROLLED;
        return config;
    }

    @Override
    protected float initCurrentAnimation() {
        float range = mLauncher.getDeviceProfile().getDeviceProperties().getWidthPx();
        long maxAccuracy = (long) (2 * range);

        boolean isRtl = Utilities.isRtl(mLauncher.getResources());
        float totalShift = (mToState == APP_LIBRARY) ? (isRtl ? range : -range) : (isRtl ? -range : range);

        final StateAnimationConfig config = getConfigForStates(mFromState, mToState);
        config.duration = maxAccuracy;

        if (mCurrentAnimation != null) {
            mCurrentAnimation.getTarget().removeListener(mClearStateOnCancelListener);
            mCurrentAnimation.dispatchOnCancel();
        }

        mGoingBetweenStates = true;
        mCurrentAnimation = mLauncher.getStateManager().createAnimationToNewWorkspace(mToState, config);
        mCurrentAnimation.getTarget().addListener(mClearStateOnCancelListener);

        return 1f / totalShift;
    }
}
