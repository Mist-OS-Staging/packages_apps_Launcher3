/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.quickstep.recents.style;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies the geometry and z-order of the MIUI horizontal carousel. */
@RunWith(AndroidJUnit4.class)
public class HorizontalLayoutTest {
    private static final float CARD = 1000f;
    private static final float DENSITY = 2.75f;

    @Test
    public void focusedCardIsUntouched() {
        assertThat(HorizontalLayout.getScaleForDistance(0f)).isEqualTo(1f);
        assertThat(HorizontalLayout.getVisualDistance(0f, CARD)).isEqualTo(0f);
        assertThat(HorizontalLayout.getAlphaForDistance(0f)).isEqualTo(1f);
    }

    @Test
    public void cardsArePulledTowardsTheFocusedOneNeverAway() {
        for (int i = 0; i <= 500; i++) {
            float d = i / 100f;
            assertThat(d * CARD - HorizontalLayout.getVisualDistance(d, CARD)).isAtLeast(-0.01f);
        }
    }

    @Test
    public void visualDistanceNeverReordersCards() {
        float previous = -1f;
        for (int i = 0; i <= 500; i++) {
            float visual = HorizontalLayout.getVisualDistance(i / 100f, CARD);
            assertThat(visual).isAtLeast(previous);
            previous = visual;
        }
    }

    @Test
    public void scaleShrinksAndBottomsOut() {
        float previous = 2f;
        for (int i = 0; i <= 500; i++) {
            float scale = HorizontalLayout.getScaleForDistance(i / 100f);
            assertThat(scale).isAtMost(previous);
            assertThat(scale).isAtLeast(HorizontalLayout.MIN_SCALE);
            previous = scale;
        }
    }

    @Test
    public void closerCardsAreAlwaysDrawnInFront() {
        // Strictly decreasing for as long as the card is still visible (it is fully faded out at
        // d = 3.8); after that nothing is drawn, so ties no longer matter.
        float previous = Float.MAX_VALUE;
        for (int i = 0; i < 380; i++) {
            float elevation = HorizontalLayout.getElevationForDistance(i / 100f, DENSITY);
            assertThat(elevation).isLessThan(previous);
            assertThat(elevation).isGreaterThan(0f);
            previous = elevation;
        }
    }

    @Test
    public void farCardsFadeOutWithoutAlphaJump() {
        assertThat(HorizontalLayout.getAlphaForDistance(2.8f)).isEqualTo(1f);
        assertThat(HorizontalLayout.getAlphaForDistance(3.8f)).isEqualTo(0f);
        float previous = 1f;
        for (int i = 0; i <= 500; i++) {
            float alpha = HorizontalLayout.getAlphaForDistance(i / 100f);
            assertThat(alpha).isAtMost(previous);
            previous = alpha;
        }
    }

    @Test
    public void swipedAwayCardStaysOpaqueAndFollowsDismiss() {
        HorizontalLayout layout = new HorizontalLayout();
        assertThat(layout.followsDismissProgress()).isTrue();
        assertThat(layout.getDismissedTaskAlpha(1f)).isEqualTo(1f);
    }
}
