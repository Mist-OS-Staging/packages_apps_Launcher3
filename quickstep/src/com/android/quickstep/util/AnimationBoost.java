/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.quickstep.util;

import android.animation.ValueAnimator;
import android.os.Looper;
import android.view.View;
import android.view.ViewRootImpl;

import androidx.annotation.Nullable;

import com.android.launcher3.util.InteractionBoost;

/**
 * Prepares the device for an app transition or a home animation that is about to start.
 *
 * <p>The renderer is told that the next frame is expensive, so ADPF hint sessions raise the
 * render thread's CPU budget before the frame instead of after it was late, and an interaction
 * power boost lifts the clock floors for the length of the animation.
 */
public final class AnimationBoost {

    private AnimationBoost() { }

    public static void requestForAnimation(@Nullable View view) {
        if (view == null || !ValueAnimator.areAnimatorsEnabled() || !view.isAttachedToWindow()) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper() && view.isHardwareAccelerated()) {
            ViewRootImpl root = view.getViewRootImpl();
            if (root != null) {
                root.notifyRendererOfExpensiveFrame("Launcher animation start");
            }
        }
        InteractionBoost.request(InteractionBoost.ANIMATION_BOOST_DURATION_MS);
    }
}
