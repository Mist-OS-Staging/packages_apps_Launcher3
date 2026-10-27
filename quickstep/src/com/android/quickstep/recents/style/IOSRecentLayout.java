/*
 * Copyright (C) 2026 MistOS
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

package com.android.quickstep.recents.style;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.quickstep.views.RecentsView;
import com.android.quickstep.views.TaskView;

/**
 * Stacked app switcher in the style of ColorOS / iOS.
 *
 * The focused card keeps its normal place and size. Older tasks are tucked behind it: each one
 * only shows a strip of its edge, the strips get thinner the deeper the task is, and deeper tasks
 * are slightly smaller. Newer tasks (only visible after scrolling into older ones) keep the normal
 * carousel spacing and are drawn on top. Z-order is therefore strictly "newer in front of older".
 *
 * Proportions follow the ColorOS stack: the first older card shows ~23% of its width, the next
 * ones a few percent each, and each card is ~4% smaller than the one in front of it.
 *
 * Swiping a card up removes it; the cards behind it then slide forward through the stack and the
 * next card grows into focus. That reflow is only visible because every transform is derived from
 * the cards' live geometry (see {@link #followsDismissProgress()}).
 *
 * "Depth" below is the signed distance, in pages, from the focused card towards the older tasks:
 * 0 is the focused card, 1 the first older card, negative values are newer tasks. It is derived
 * from the laid-out card center and the viewport center, which keeps it correct while Recents is
 * scaled for app launch / Overview entry and in both LTR and RTL.
 */
public final class IOSRecentLayout implements RecentLayoutHandler {

    /** Fraction of the card width the first older card shows. */
    static final float FIRST_EXPOSED = 0.23f;
    /** Extra fraction shown by the second older card; each deeper card shows DECAY of that. */
    static final float NEXT_EXPOSED = 0.035f;
    static final float DECAY = 0.55f;
    /** Scale lost per step into the stack, the depth where it stops, and the smallest scale. */
    static final float SCALE_STEP = 0.04f;
    static final float SCALE_DEPTH_CAP = 3f;
    static final float MIN_SCALE = 0.88f;

    /** Older cards past this depth are thin slivers; fade them out and stop drawing them. */
    private static final float OLDER_FADE_START = 3.8f;
    private static final float OLDER_VISIBLE_DEPTH = 4.5f;
    /** Newer cards sit at the normal carousel spacing, so they leave the screen sooner. */
    private static final float NEWER_FADE_START = 2.65f;
    private static final float NEWER_FADE_END = 3.50f;
    private static final float NEWER_VISIBLE_DEPTH = 3.55f;

    /**
     * Elevation (dp) falls by this much per page of depth, so newer cards are always in front of
     * older ones. Only the ordering matters; the range is kept small to keep shadows subtle.
     */
    private static final float ELEVATION_MID_DP = 8f;
    private static final float ELEVATION_PER_PAGE_DP = 0.6f;
    private static final float ELEVATION_DEPTH_LIMIT = 6f;

    @NonNull
    @Override
    public RecentStyle getStyle() {
        return RecentStyle.IOS;
    }

    @Override
    public void onDetached(@NonNull RecentsView<?, ?> recentsView) {
        for (TaskView task : recentsView.getTaskViews()) {
            task.setColorTint(0f, 0);
        }
    }

    @Override
    public boolean followsDismissProgress() {
        return true;
    }

    /** The card is flung off the top of the screen without fading, like iOS / ColorOS. */
    @Override
    public float getDismissedTaskAlpha(float dismissProgress) {
        return 1f;
    }

    @Nullable
    @Override
    public Boolean isTaskViewVisible(@NonNull RecentsView<?, ?> recentsView,
            @NonNull TaskView taskView) {
        if (recentsView.indexOfChild(taskView) < 0) return false;
        if (!isStackLayout(recentsView)) return null;
        if (taskView.getWidth() <= 0 || recentsView.getWidth() <= 0
                || recentsView.getLastComputedTaskSize().width() <= 0) return true;
        float depth = getDepth(recentsView, taskView);
        return depth < OLDER_VISIBLE_DEPTH && -depth < NEWER_VISIBLE_DEPTH;
    }

    @Override
    public void calculateTransform(@NonNull RecentsView<?, ?> recentsView,
            @NonNull TaskView taskView, int taskIndex, int taskCount, float scrollProgress,
            @NonNull StyleTransform outTransform) {
        outTransform.reset();

        // Only the phone carousel in its natural orientation is stacked; a rotated Recents keeps
        // the stock layout because the geometry below is horizontal.
        if (!isStackLayout(recentsView)) {
            return;
        }
        int cardWidth = recentsView.getLastComputedTaskSize().width();
        if (cardWidth <= 0 || taskView.getWidth() <= 0 || recentsView.getWidth() <= 0) {
            return;
        }

        // Child left/top positions are untransformed layout coordinates. This geometric offset
        // stays correct while the parent is being scaled for app launch or Overview entry, and
        // it includes the reflow translation of a dismiss, so cards move continuously.
        final float nativeOffset = getNativeCenterOffset(recentsView, taskView);
        final float depth = getDepth(recentsView, taskView);
        final float density = recentsView.getResources().getDisplayMetrics().density;

        if (depth >= OLDER_VISIBLE_DEPTH || -depth >= NEWER_VISIBLE_DEPTH) {
            // Don't apply enormous offscreen translations or repeatedly tint hidden thumbnails.
            outTransform.scale = depth > 0 ? MIN_SCALE : 1f;
            outTransform.elevation = 0f;
            outTransform.alpha = 0f;
            return;
        }

        if (depth > 0f) {
            // Older card: tuck it behind the focused one so only a strip of its edge shows.
            float stackedOffset = getStackedCenterOffset(depth, cardWidth);
            outTransform.translationX = Math.copySign(stackedOffset, nativeOffset) - nativeOffset;
            outTransform.scale = getScaleForDepth(depth);
        }
        // Focused and newer cards keep their normal size and spacing.
        outTransform.translationY = 0f;
        outTransform.elevation = getElevationForDepth(depth, density);
        outTransform.alpha = getAlphaForDepth(depth);
    }

    private static boolean isStackLayout(@NonNull RecentsView<?, ?> recentsView) {
        return StyleGeometry.isNaturalOrientation(recentsView);
    }

    private static float getNativeCenterOffset(@NonNull RecentsView<?, ?> recentsView,
            @NonNull TaskView taskView) {
        return StyleGeometry.getNativeCenterOffset(recentsView, taskView);
    }

    /**
     * Pages from the focused card towards the older tasks. Recents lays older tasks out on the
     * left on a normal LTR phone, which {@link RecentsView#isRtl()} reflects.
     */
    private static float getDepth(@NonNull RecentsView<?, ?> recentsView,
            @NonNull TaskView taskView) {
        float olderSide = recentsView.isRtl() ? -1f : 1f;
        return olderSide * getNativeCenterOffset(recentsView, taskView)
                / StyleGeometry.getNominalPageSize(recentsView);
    }

    /**
     * Fraction of the card width by which a card {@code depth} positions into the stack sticks
     * out beyond the focused card's edge (0 = focused card, may be fractional while scrolling).
     * A geometric series, continuous in {@code depth}.
     */
    static float getExposedFraction(float depth) {
        if (depth <= 0f) return 0f;
        if (depth <= 1f) return FIRST_EXPOSED * depth;
        float extra = depth - 1f;
        return FIRST_EXPOSED
                + NEXT_EXPOSED * (1f - (float) Math.pow(DECAY, extra)) / (1f - DECAY);
    }

    /** Scale of a card {@code depth} positions into the stack. */
    static float getScaleForDepth(float depth) {
        if (depth <= 0f) return 1f;
        return Math.max(MIN_SCALE, 1f - SCALE_STEP * Math.min(depth, SCALE_DEPTH_CAP));
    }

    /**
     * Distance from the focused card's center to the center of a card {@code depth} positions
     * into the stack, such that only {@link #getExposedFraction} of its (scaled) width sticks out.
     */
    static float getStackedCenterOffset(float depth, float cardWidth) {
        float scale = getScaleForDepth(depth);
        return getExposedFraction(depth) * cardWidth + (1f - scale) * cardWidth / 2f;
    }

    /**
     * Strictly decreasing with depth within +/-{@link #ELEVATION_DEPTH_LIMIT}, which is what
     * orders the cards front-to-back: newer cards are always drawn over older ones.
     */
    static float getElevationForDepth(float depth, float density) {
        float clamped = Math.max(-ELEVATION_DEPTH_LIMIT, Math.min(ELEVATION_DEPTH_LIMIT, depth));
        return (ELEVATION_MID_DP - ELEVATION_PER_PAGE_DP * clamped) * density;
    }

    static float getAlphaForDepth(float depth) {
        if (depth >= 0f) {
            return 1f - smoothstep(OLDER_FADE_START, OLDER_VISIBLE_DEPTH, depth);
        }
        return 1f - smoothstep(NEWER_FADE_START, NEWER_FADE_END, -depth);
    }

    private static float smoothstep(float start, float end, float value) {
        float t = Math.max(0f, Math.min(1f, (value - start) / (end - start)));
        return t * t * (3f - 2f * t);
    }
}
