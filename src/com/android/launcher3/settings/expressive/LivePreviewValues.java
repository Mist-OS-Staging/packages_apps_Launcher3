/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings.expressive;

import android.os.Looper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LivePreviewValues {

    public interface Listener {
        void onLiveValueChanged(String key);
    }

    private static final Map<String, Integer> sValues = new HashMap<>();
    private static final List<Listener> sListeners = new ArrayList<>();

    private LivePreviewValues() { }

    public static void set(@Nullable String key, int value) {
        if (key == null || Looper.myLooper() != Looper.getMainLooper()) {
            return;
        }
        Integer old = sValues.put(key, value);
        if (old == null || old != value) {
            notifyListeners(key);
        }
    }

    public static void clear(@Nullable String key) {
        if (key == null || Looper.myLooper() != Looper.getMainLooper()) {
            return;
        }
        if (sValues.remove(key) != null) {
            notifyListeners(key);
        }
    }

    public static int get(String key, int fallback) {
        Integer value = sValues.get(key);
        return value == null ? fallback : value;
    }

    public static void addListener(Listener listener) {
        if (!sListeners.contains(listener)) {
            sListeners.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        sListeners.remove(listener);
    }

    private static void notifyListeners(String key) {
        for (Listener listener : new ArrayList<>(sListeners)) {
            listener.onLiveValueChanged(key);
        }
    }
}
