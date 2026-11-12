/*
 * Copyright (C) 2024-2026 Lunaris AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.launcher3.util;

import android.content.Context;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Requests a short {@code Boost.INTERACTION} power boost, so CPU and GPU clocks are already up
 * when an animation or a fling starts instead of ramping up after the first frames were missed.
 *
 * <p>This lives in the shared sources that are also built against the public SDK, so the hidden
 * power manager binder is resolved reflectively and every failure only disables the boost.
 * Platform signed builds (Launcher3QuickStep holds {@code DEVICE_POWER}) get the real boost.
 */
public final class InteractionBoost {
    private static final String TAG = "InteractionBoost";

    /** Boost length used when an app transition or a home animation starts. */
    public static final int ANIMATION_BOOST_DURATION_MS = 500;
    private static final int MIN_BOOST_INTERVAL_MS = 100;
    /** {@code android.hardware.power.Boost.INTERACTION}, used if the constant can't be read. */
    private static final int DEFAULT_BOOST_INTERACTION = 0;

    private static boolean sResolved;
    @Nullable
    private static Object sPowerManager;
    @Nullable
    private static Method sSetPowerBoost;
    private static int sBoostInteraction = DEFAULT_BOOST_INTERACTION;

    private static long sLastBoostTime = -MIN_BOOST_INTERVAL_MS;
    private static int sLastBoostDuration;

    private InteractionBoost() { }

    /** Requests an interaction boost for {@code durationMs}, rate limited to one per 100ms. */
    public static synchronized void request(int durationMs) {
        if (durationMs <= 0) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        long elapsed = now - sLastBoostTime;
        if (elapsed < MIN_BOOST_INTERVAL_MS && elapsed < sLastBoostDuration
                && durationMs <= sLastBoostDuration) {
            // A boost that covers this request is already running.
            return;
        }
        if (!resolve()) {
            return;
        }
        try {
            // IPowerManager#setPowerBoost is oneway, this never blocks the caller.
            sSetPowerBoost.invoke(sPowerManager, sBoostInteraction, durationMs);
            sLastBoostTime = now;
            sLastBoostDuration = durationMs;
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof SecurityException) {
                // Not allowed to boost (missing DEVICE_POWER), stop trying.
                sSetPowerBoost = null;
            }
            Log.w(TAG, "Unable to request interaction boost", e.getCause());
        } catch (ReflectiveOperationException | RuntimeException e) {
            sSetPowerBoost = null;
            Log.w(TAG, "Unable to request interaction boost", e);
        }
    }

    private static boolean resolve() {
        if (sResolved) {
            return sSetPowerBoost != null;
        }
        sResolved = true;
        try {
            IBinder binder = (IBinder) Class.forName("android.os.ServiceManager")
                    .getMethod("getService", String.class)
                    .invoke(null, Context.POWER_SERVICE);
            if (binder == null) {
                return false;
            }
            Object powerManager = Class.forName("android.os.IPowerManager$Stub")
                    .getMethod("asInterface", IBinder.class)
                    .invoke(null, binder);
            Method setPowerBoost = Class.forName("android.os.IPowerManager")
                    .getMethod("setPowerBoost", int.class, int.class);
            try {
                sBoostInteraction = Class.forName("android.hardware.power.Boost")
                        .getField("INTERACTION").getInt(null);
            } catch (ReflectiveOperationException | RuntimeException e) {
                sBoostInteraction = DEFAULT_BOOST_INTERACTION;
            }
            sPowerManager = powerManager;
            sSetPowerBoost = setPowerBoost;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            Log.w(TAG, "Interaction boosts are not available", e);
        }
        return sSetPowerBoost != null;
    }
}
