/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.quickstep.util;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;

import com.android.internal.util.mist.MistifyFluidMotionHelper;

/**
 * Motion parameters for the Nothing OS style app open/close transitions.
 *
 * The window travels on a critically damped spring between the icon and its final bounds. The
 * window is never squashed: it is scaled uniformly and cropped from the top, while its aspect ratio
 * morphs between the icon and the window shape.
 */
public final class NothingAppAnimation {
    public static final long LAUNCH_DURATION_MS = 500;
    public static final int FALLBACK_CLOSE_DURATION_MS = 250;
    public static final float STIFFNESS_X = 342f;
    public static final float STIFFNESS_Y = 315f;
    public static final float STIFFNESS_SCALE = 380f;
    public static final float DAMPING_RATIO = 1f;
    public static final float OPEN_ALPHA_END = 0.15f;
    public static final float CLOSE_ALPHA_END = 0.85f;
    private static final float NO_TARGET_CLOSE_ALPHA_END = 0.98f;
    public static final float SHAPE_PROGRESS_START = 0.9f;
    public static final long WORKSPACE_DURATION_MS = 350;
    public static final long WORKSPACE_FADE_DURATION_MS = 200;
    public static final float WORKSPACE_MIN_ALPHA = 0.5f;
    public static final float WORKSPACE_MIN_SCALE = 0.9f;

    private NothingAppAnimation() { }

    /**
     * Nothing transitions are the default. Mist's fluid motion toggle keeps the AOSP based
     * transitions with the fluid curves instead.
     */
    public static boolean isEnabled(Context context) {
        return !MistifyFluidMotionHelper.isFluidAnimationEnabled(context);
    }

    public static float getWindowAlpha(float progress, boolean opening) {
        float t = clamp(progress / (opening ? OPEN_ALPHA_END : CLOSE_ALPHA_END));
        float alpha = t * t * t;
        return opening ? alpha : 1f - alpha;
    }

    /** Window alpha used when closing to a point that has no icon or widget behind it. */
    public static float getFallbackWindowAlpha(float progress) {
        float t = clamp(progress / NO_TARGET_CLOSE_ALPHA_END);
        return 1f - t * t * t;
    }

    /** Target used when the closing app has no icon or widget on the home screen. */
    public static RectF getDefaultWindowTargetRect(int iconSizePx, float primaryDimension,
            float secondaryDimension) {
        int halfSize = iconSizePx / 4;
        float centerX = primaryDimension / 2f;
        float centerY = secondaryDimension / 3f;
        return new RectF(centerX - halfSize, centerY - halfSize,
                centerX + halfSize, centerY + halfSize);
    }

    public static float getSizeProgress(float progress) {
        return progress >= 0.9f ? Math.min(1f, progress * 1.002f) : progress;
    }

    /** Height that keeps the aspect ratio morphing linearly between the start and end rects. */
    public static float getHeight(float progress, float startWidth, float startHeight,
            float endWidth, float endHeight) {
        float width = startWidth + progress * (endWidth - startWidth);
        float linearHeight = startHeight + progress * (endHeight - startHeight);
        if (startWidth <= 0f || endWidth <= 0f) return linearHeight;
        float ratio = startHeight / startWidth
                + progress * (endHeight / endWidth - startHeight / startWidth);
        return Math.min(width * ratio, linearHeight);
    }

    /**
     * Scales {@code source} uniformly so that it fills {@code target}, cropping the overflow from
     * the bottom (portrait) or the end (landscape).
     *
     * @return the applied scale, or 0 if either rect is empty.
     */
    public static float updateWindowCrop(RectF source, RectF target, Matrix matrix, RectF crop) {
        if (source.isEmpty() || target.isEmpty()) return 0f;
        float scale = Math.min(1f, source.height() >= source.width()
                ? target.width() / source.width() : target.height() / source.height());
        crop.set(source.left, source.top, source.left + target.width() / scale,
                source.top + target.height() / scale);
        matrix.setScale(scale, scale);
        matrix.postTranslate(target.left - crop.left * scale, target.top - crop.top * scale);
        return scale;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
