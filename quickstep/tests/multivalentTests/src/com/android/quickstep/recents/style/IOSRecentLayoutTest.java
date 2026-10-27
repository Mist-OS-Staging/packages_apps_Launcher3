/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.quickstep.recents.style;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies the geometry of the stacked (ColorOS / iOS-like) Recents layout. */
@RunWith(AndroidJUnit4.class)
public class IOSRecentLayoutTest {
    private static final float CARD_WIDTH = 1000f;

    @Test
    public void focusedCardIsUntouched() {
        assertThat(IOSRecentLayout.getExposedFraction(0f)).isEqualTo(0f);
        assertThat(IOSRecentLayout.getScaleForDepth(0f)).isEqualTo(1f);
        assertThat(IOSRecentLayout.getAlphaForDepth(0f)).isEqualTo(1f);
        assertThat(IOSRecentLayout.getStackedCenterOffset(0f, CARD_WIDTH)).isEqualTo(0f);
    }

    @Test
    public void newerCardsKeepNormalSize() {
        for (int i = 1; i <= 30; i++) {
            assertThat(IOSRecentLayout.getScaleForDepth(-i / 10f)).isEqualTo(1f);
        }
    }

    @Test
    public void firstOlderCardShowsTwentyThreePercentOfItsWidth() {
        assertThat(IOSRecentLayout.getExposedFraction(1f)).isWithin(0.0001f).of(0.23f);
        // The strip that sticks out equals the exposed fraction, whatever the card's scale.
        float depth = 1f;
        float outerEdge = IOSRecentLayout.getStackedCenterOffset(depth, CARD_WIDTH)
                + IOSRecentLayout.getScaleForDepth(depth) * CARD_WIDTH / 2f;
        assertThat(outerEdge - CARD_WIDTH / 2f).isWithin(0.01f).of(230f);
    }

    @Test
    public void deeperStripsGetThinnerButStayVisible() {
        float previousStrip = Float.MAX_VALUE;
        for (int depth = 1; depth <= 4; depth++) {
            float strip = IOSRecentLayout.getExposedFraction(depth)
                    - IOSRecentLayout.getExposedFraction(depth - 1);
            assertThat(strip).isGreaterThan(0f);
            assertThat(strip).isLessThan(previousStrip);
            previousStrip = strip;
        }
    }

    @Test
    public void stackedOffsetIsContinuousAndMonotonic() {
        float previous = -1f;
        for (int i = 0; i <= 500; i++) {
            float offset = IOSRecentLayout.getStackedCenterOffset(i / 100f, CARD_WIDTH);
            assertThat(Float.isFinite(offset)).isTrue();
            assertThat(offset).isAtLeast(previous);
            assertThat(offset - previous).isLessThan(10f);
            previous = offset;
        }
        // The stack never grows beyond ~31% of the card plus the scaling inset.
        assertThat(previous).isLessThan(0.40f * CARD_WIDTH);
    }

    @Test
    public void scaleStepsDownAndBottomsOut() {
        assertThat(IOSRecentLayout.getScaleForDepth(1f)).isWithin(0.0001f).of(0.96f);
        assertThat(IOSRecentLayout.getScaleForDepth(2f)).isWithin(0.0001f).of(0.92f);
        assertThat(IOSRecentLayout.getScaleForDepth(3f)).isWithin(0.0001f).of(0.88f);
        assertThat(IOSRecentLayout.getScaleForDepth(9f)).isWithin(0.0001f).of(0.88f);
    }

    @Test
    public void newerCardsAreAlwaysDrawnOverOlderOnes() {
        float previous = Float.MAX_VALUE;
        for (int i = -590; i <= 590; i += 5) {
            float elevation = IOSRecentLayout.getElevationForDepth(i / 100f, 1f);
            assertThat(elevation).isLessThan(previous);
            assertThat(elevation).isGreaterThan(0f);
            previous = elevation;
        }
    }

    @Test
    public void farCardsFadeOutWithoutAlphaJump() {
        assertThat(IOSRecentLayout.getAlphaForDepth(3.8f)).isEqualTo(1f);
        assertThat(IOSRecentLayout.getAlphaForDepth(4.5f)).isEqualTo(0f);
        assertThat(IOSRecentLayout.getAlphaForDepth(-2.65f)).isEqualTo(1f);
        assertThat(IOSRecentLayout.getAlphaForDepth(-3.5f)).isEqualTo(0f);
        for (int i = -500; i <= 500; i++) {
            float alpha = IOSRecentLayout.getAlphaForDepth(i / 100f);
            assertThat(alpha).isAtLeast(0f);
            assertThat(alpha).isAtMost(1f);
        }
    }

    @Test
    public void swipedAwayCardStaysOpaqueAndFollowsDismiss() {
        IOSRecentLayout layout = new IOSRecentLayout();
        assertThat(layout.followsDismissProgress()).isTrue();
        assertThat(layout.getDismissedTaskAlpha(0f)).isEqualTo(1f);
        assertThat(layout.getDismissedTaskAlpha(1f)).isEqualTo(1f);
    }
}
