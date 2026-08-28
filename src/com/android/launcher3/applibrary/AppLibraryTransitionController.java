package com.android.launcher3.applibrary;

import static com.android.app.animation.Interpolators.DECELERATE_1_7;
import static com.android.launcher3.LauncherState.APP_LIBRARY;
import static com.android.launcher3.LauncherState.NORMAL;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.util.FloatProperty;
import android.view.View;
import android.view.animation.Interpolator;

import com.android.launcher3.DeviceProfile;
import com.android.launcher3.DeviceProfile.OnDeviceProfileChangeListener;
import com.android.launcher3.Hotseat;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherState;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.anim.PendingAnimation;
import com.android.launcher3.applibrary.view.AppLibraryContainerView;
import com.android.launcher3.statemanager.StateManager.StateHandler;
import com.android.launcher3.states.StateAnimationConfig;
import com.android.launcher3.views.ScrimView;

public class AppLibraryTransitionController
        implements StateHandler<LauncherState>, OnDeviceProfileChangeListener {

    public static final FloatProperty<AppLibraryTransitionController> APP_LIBRARY_PROGRESS =
            new FloatProperty<AppLibraryTransitionController>("appLibraryProgress") {
                @Override
                public Float get(AppLibraryTransitionController controller) {
                    return controller.mProgress;
                }

                @Override
                public void setValue(AppLibraryTransitionController controller, float progress) {
                    controller.setProgress(progress);
                }
            };

    private final Launcher mLauncher;
    private AppLibraryContainerView mAppLibraryView;
    private ScrimView mScrimView;
    private float mProgress = 1f;

    public AppLibraryTransitionController(Launcher launcher) {
        mLauncher = launcher;
        mLauncher.addOnDeviceProfileChangeListener(this);
    }

    public void setupViews(ScrimView scrimView, AppLibraryContainerView appLibraryView) {
        mScrimView = scrimView;
        mAppLibraryView = appLibraryView;
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float progress) {
        mProgress = Utilities.boundToRange(progress, 0f, 1f);
        if (mAppLibraryView == null) {
            return;
        }

        float width = mLauncher.getDeviceProfile().getDeviceProperties().getWidthPx();
        boolean isRtl = Utilities.isRtl(mLauncher.getResources());

        if (Float.compare(mProgress, 1f) == 0) {
            mAppLibraryView.setVisibility(View.INVISIBLE);
            mAppLibraryView.setTranslationX(isRtl ? -width : width);
            mAppLibraryView.setAlpha(0f);
        } else {
            if (mAppLibraryView.getVisibility() != View.VISIBLE) {
                mAppLibraryView.setVisibility(View.VISIBLE);
            }
            float translationX = isRtl ? -width * mProgress : width * mProgress;
            mAppLibraryView.setTranslationX(translationX);
            mAppLibraryView.setAlpha(1f - mProgress);
        }

        Workspace<?> workspace = mLauncher.getWorkspace();
        if (workspace != null) {
            float shift = isRtl ? (1f - mProgress) * (width * 0.35f) : -(1f - mProgress) * (width * 0.35f);
            workspace.setTranslationX(shift);
            workspace.setAlpha(mProgress);
        }

        Hotseat hotseat = mLauncher.getHotseat();
        if (hotseat != null) {
            hotseat.setAlpha(mProgress);
        }
    }

    @Override
    public void setState(LauncherState state) {
        if (state == APP_LIBRARY) {
            setProgress(0f);
        } else {
            setProgress(1f);
            if (mAppLibraryView != null) {
                mAppLibraryView.reset();
            }
        }
    }

    @Override
    public void setStateWithAnimation(LauncherState toState, StateAnimationConfig config,
            PendingAnimation animation) {
        if (toState == APP_LIBRARY) {
            if (mLauncher.isInState(NORMAL) && Float.compare(mProgress, 1f) != 0) {
                setProgress(1f);
            }
            float targetProgress = 0f;
            Interpolator interpolator = config.getInterpolator(
                    StateAnimationConfig.ANIM_WORKSPACE_TRANSLATE, DECELERATE_1_7);
            animation.setFloat(this, APP_LIBRARY_PROGRESS, targetProgress, interpolator);
        } else {
            if (mLauncher.isInState(APP_LIBRARY) && Float.compare(mProgress, 0f) != 0) {
                setProgress(0f);
            }
            float targetProgress = 1f;
            Interpolator interpolator = config.getInterpolator(
                    StateAnimationConfig.ANIM_WORKSPACE_TRANSLATE, DECELERATE_1_7);
            animation.setFloat(this, APP_LIBRARY_PROGRESS, targetProgress, interpolator);
            animation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator anim) {
                    if (mAppLibraryView != null) {
                        mAppLibraryView.reset();
                    }
                }
            });
        }
    }

    @Override
    public void onDeviceProfileChanged(DeviceProfile dp) {
        setProgress(mProgress);
    }
}
