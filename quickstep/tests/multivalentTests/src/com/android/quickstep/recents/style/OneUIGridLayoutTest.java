/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.quickstep.recents.style;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Verifies the z-order of the OneUI grid, which matters while cards slide between slots. */
@RunWith(AndroidJUnit4.class)
public class OneUIGridLayoutTest {
    private static final float DENSITY = 2.75f;

    @Test
    public void earlierSlotsAreDrawnInFrontOfLaterOnes() {
        float previous = Float.MAX_VALUE;
        for (int slot = 0; slot <= 100; slot++) {
            float elevation = OneUIGridLayout.getElevationForSlot(slot, DENSITY);
            assertThat(elevation).isLessThan(previous);
            assertThat(elevation).isGreaterThan(0f);
            previous = elevation;
        }
    }

    @Test
    public void elevationStaysBoundedForHugeTaskLists() {
        float atLimit = OneUIGridLayout.getElevationForSlot(100, DENSITY);
        assertThat(OneUIGridLayout.getElevationForSlot(10_000, DENSITY)).isEqualTo(atLimit);
        assertThat(atLimit).isGreaterThan(0f);
    }

    @Test
    public void sweptCardsStayBelowTheCardBeingDragged() {
        // The dragged card gets a 48dp translation Z on top of its own elevation.
        float highest = OneUIGridLayout.getElevationForSlot(0, 1f);
        assertThat(highest).isLessThan(48f);
    }

    @Test
    public void swipedAwayCardStaysOpaque() {
        assertThat(new OneUIGridLayout().getDismissedTaskAlpha(1f)).isEqualTo(1f);
    }
}
