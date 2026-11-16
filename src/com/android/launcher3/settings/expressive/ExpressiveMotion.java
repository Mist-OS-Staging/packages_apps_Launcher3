/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings.expressive;

import android.annotation.SuppressLint;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

public final class ExpressiveMotion {

    public static final float SPATIAL_STIFFNESS = 380f;
    public static final float SPATIAL_DAMPING = 0.62f;
    public static final float PRESS_STIFFNESS = 1400f;
    public static final float PRESS_DAMPING = 0.9f;

    private ExpressiveMotion() { }

    public static SpringAnimation spring(View view, DynamicAnimation.ViewProperty property,
            float stiffness, float damping) {
        SpringAnimation animation = new SpringAnimation(view, property);
        animation.setSpring(new SpringForce().setStiffness(stiffness).setDampingRatio(damping));
        return animation;
    }

    public static void springTo(View view, DynamicAnimation.ViewProperty property, float value,
            float stiffness, float damping) {
        spring(view, property, stiffness, damping).animateToFinalPosition(value);
    }

    public static SpringAnimation cornerSpring(GradientDrawable drawable, float start) {
        FloatValueHolder holder = new FloatValueHolder(start);
        SpringAnimation animation = new SpringAnimation(holder);
        animation.setSpring(new SpringForce(start)
                .setStiffness(SPATIAL_STIFFNESS).setDampingRatio(SPATIAL_DAMPING));
        animation.addUpdateListener((anim, value, velocity) -> drawable.setCornerRadius(value));
        return animation;
    }

    @SuppressLint("ClickableViewAccessibility")
    public static void attachPress(View target, @Nullable GradientDrawable background,
            float radius, float pressedRadius, float pressedScale, @Nullable View spinner,
            float spinDegrees) {
        SpringAnimation scaleX = spring(target, DynamicAnimation.SCALE_X,
                SPATIAL_STIFFNESS, SPATIAL_DAMPING);
        SpringAnimation scaleY = spring(target, DynamicAnimation.SCALE_Y,
                SPATIAL_STIFFNESS, SPATIAL_DAMPING);
        SpringAnimation corner = background == null ? null : cornerSpring(background, radius);
        SpringAnimation spin = spinner == null ? null : spring(spinner, DynamicAnimation.ROTATION,
                SPATIAL_STIFFNESS, 0.5f);
        target.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    scaleX.getSpring().setStiffness(PRESS_STIFFNESS).setDampingRatio(PRESS_DAMPING);
                    scaleY.getSpring().setStiffness(PRESS_STIFFNESS).setDampingRatio(PRESS_DAMPING);
                    scaleX.animateToFinalPosition(pressedScale);
                    scaleY.animateToFinalPosition(pressedScale);
                    if (corner != null) {
                        corner.animateToFinalPosition(pressedRadius);
                    }
                    if (spin != null) {
                        spin.animateToFinalPosition(spinDegrees);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    scaleX.getSpring().setStiffness(SPATIAL_STIFFNESS)
                            .setDampingRatio(SPATIAL_DAMPING);
                    scaleY.getSpring().setStiffness(SPATIAL_STIFFNESS)
                            .setDampingRatio(SPATIAL_DAMPING);
                    scaleX.animateToFinalPosition(1f);
                    scaleY.animateToFinalPosition(1f);
                    if (corner != null) {
                        corner.animateToFinalPosition(radius);
                    }
                    if (spin != null) {
                        spin.animateToFinalPosition(0f);
                    }
                    break;
                default:
                    break;
            }
            return false;
        });
    }

    public static void enter(View view, float offsetPx, long delayMs) {
        view.setAlpha(0f);
        view.setTranslationY(offsetPx);
        view.postDelayed(() -> {
            view.animate().alpha(1f).setDuration(220).start();
            springTo(view, DynamicAnimation.TRANSLATION_Y, 0f, SPATIAL_STIFFNESS, 0.72f);
        }, delayMs);
    }
}
