/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.quickstep.recents.style;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies the geometry and z-order of the centered Mistify stack. */
@RunWith(AndroidJUnit4.class)
public class MistifyStackLayoutTest {
    private static final float CARD = 1000f;
    private static final float DENSITY = 2.75f;

    @Test
    public void focusedCardIsUntouched() {
        assertThat(MistifyStackLayout.getPullBack(0f, CARD, DENSITY)).isEqualTo(0f);
        assertThat(MistifyStackLayout.getPeekOffset(0f, CARD, DENSITY)).isEqualTo(0f);
        assertThat(MistifyStackLayout.getScaleForDistance(0f, CARD)).isEqualTo(1f);
        assertThat(MistifyStackLayout.getAlphaForSteps(0f)).isEqualTo(1f);
    }

    @Test
    public void cardsInsideTheStapleDistanceKeepTheirSpacing() {
        float inside = CARD * MistifyStackLayout.STAPLE_RATIO * 0.99f;
        assertThat(MistifyStackLayout.getPullBack(inside, CARD, DENSITY)).isEqualTo(0f);
        assertThat(MistifyStackLayout.getScaleForDistance(inside, CARD)).isEqualTo(1f);
    }

    @Test
    public void cardsAreAlwaysDrawnInFrontOfTheOnesFurtherOut() {
        // The old curve was flat inside the staple distance and floored far out, so cards on one
        // side shared an elevation. It must now fall strictly, all the way out.
        float previous = Float.MAX_VALUE;
        for (int i = 0; i <= 500; i++) {
            float elevation = MistifyStackLayout.getElevationForDistance(
                    i / 100f * CARD, CARD, DENSITY);
            assertThat(elevation).isLessThan(previous);
            assertThat(elevation).isGreaterThan(0f);
            previous = elevation;
        }
    }

    @Test
    public void elevationIsContinuousAtTheStapleDistance() {
        float staple = CARD * MistifyStackLayout.STAPLE_RATIO;
        float before = MistifyStackLayout.getElevationForDistance(staple - 0.01f, CARD, DENSITY);
        float after = MistifyStackLayout.getElevationForDistance(staple + 0.01f, CARD, DENSITY);
        assertThat(Math.abs(before - after)).isLessThan(0.05f);
    }

    @Test
    public void visualDistanceNeverReordersCards() {
        // The distance of a card from the center after it was pulled back must not decrease as
        // the card moves further out, otherwise two cards would swap places mid-scroll.
        float previous = -1f;
        for (int i = 0; i <= 500; i++) {
            float dist = i / 100f * CARD;
            float visual = dist - MistifyStackLayout.getPullBack(dist, CARD, DENSITY);
            assertThat(visual).isAtLeast(previous);
            previous = visual;
        }
    }

    @Test
    public void scaleShrinksAndBottomsOut() {
        float previous = 2f;
        for (int i = 0; i <= 500; i++) {
            float scale = MistifyStackLayout.getScaleForDistance(i / 100f * CARD, CARD);
            assertThat(scale).isAtMost(previous);
            assertThat(scale).isAtLeast(MistifyStackLayout.MIN_SCALE);
            previous = scale;
        }
        assertThat(previous).isEqualTo(MistifyStackLayout.MIN_SCALE);
    }

    @Test
    public void farCardsFadeOutWithoutAlphaJump() {
        assertThat(MistifyStackLayout.getAlphaForSteps(3.2f)).isEqualTo(1f);
        assertThat(MistifyStackLayout.getAlphaForSteps(4.2f)).isEqualTo(0f);
        float previous = 1f;
        for (int i = 0; i <= 500; i++) {
            float alpha = MistifyStackLayout.getAlphaForSteps(i / 100f);
            assertThat(alpha).isAtMost(previous);
            previous = alpha;
        }
    }

    @Test
    public void swipedAwayCardStaysOpaqueAndFollowsDismiss() {
        MistifyStackLayout layout = new MistifyStackLayout();
        assertThat(layout.followsDismissProgress()).isTrue();
        assertThat(layout.getDismissedTaskAlpha(1f)).isEqualTo(1f);
    }
}
