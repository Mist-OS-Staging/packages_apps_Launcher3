/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.util;

import android.view.HapticFeedbackConstants;
import android.view.View;

/** Haptic played when a list hits its edge and starts to stretch. */
public final class ScrollEdgeHaptics {
    private ScrollEdgeHaptics() {}

    public static void perform(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.SCROLL_LIMIT);
    }
}
