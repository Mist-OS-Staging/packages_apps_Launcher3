/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.quickstep.views

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies when the reflow (and with it the task removal) starts after a swipe-to-dismiss. */
@RunWith(AndroidJUnit4::class)
class RecentsDismissThresholdTest {

    @Test
    fun fullSizeCard_keepsStockThreshold() {
        // A full-size card travels further than its own height, so the stock threshold is reached.
        val threshold = getReflowDriverThreshold(fullHeightThreshold = -1700f, finalPosition = -2000f)
        assertThat(threshold).isEqualTo(-1700f)
    }

    @Test
    fun scaledDownGridCard_thresholdIsReachable() {
        // A grid card is drawn at about half size, so it only travels ~1000px, never the 1700px
        // layout height. The old threshold was never crossed, so the task was never removed.
        val finalPosition = -1000f
        val threshold = getReflowDriverThreshold(fullHeightThreshold = -1700f, finalPosition)
        assertThat(Math.abs(threshold)).isLessThan(Math.abs(finalPosition))
        assertThat(threshold).isLessThan(0f)
    }

    @Test
    fun threshold_isAlwaysCrossedBeforeTheCardReachesItsFinalPosition() {
        for (finalPosition in listOf(-300f, -800f, -1700f, -2400f)) {
            val threshold = getReflowDriverThreshold(-1700f, finalPosition)
            assertThat(Math.abs(threshold)).isAtMost(Math.abs(finalPosition))
        }
    }

    @Test
    fun noDismissDistance_fallsBackToStockThreshold() {
        assertThat(getReflowDriverThreshold(fullHeightThreshold = -1700f, finalPosition = 0f))
            .isEqualTo(-1700f)
    }
}
