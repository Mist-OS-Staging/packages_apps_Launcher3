/*
 * Copyright (C) 2020 Shade Launcher
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

package com.android.launcher3.icons.clock;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;

import androidx.annotation.Nullable;

/** Sets the hands of an icon pack clock to the current time. */
public class CustomClock {

    private CustomClock() { }

    /**
     * Returns a copy of {@code drawable} with its hands set to the current time, or null if the
     * drawable does not have the layers described by {@code metadata}.
     */
    @Nullable
    public static Drawable getClock(Drawable drawable, Metadata metadata) {
        ClockLayers layers = getClockLayers(drawable, metadata);
        ClockLayers clone = layers.clone();
        if (clone == null) {
            return null;
        }
        clone.updateAngles();
        return clone.mDrawable != null ? clone.mDrawable : clone.mLayerDrawable;
    }

    private static ClockLayers getClockLayers(Drawable drawable, Metadata metadata) {
        ClockLayers layers = new ClockLayers();
        layers.setDrawable(drawable.mutate());
        layers.mHourIndex = metadata.HOUR_LAYER_INDEX;
        layers.mMinuteIndex = metadata.MINUTE_LAYER_INDEX;
        layers.mSecondIndex = metadata.SECOND_LAYER_INDEX;
        layers.mDefaultHour = metadata.DEFAULT_HOUR;
        layers.mDefaultMinute = metadata.DEFAULT_MINUTE;
        layers.mDefaultSecond = metadata.DEFAULT_SECOND;

        LayerDrawable layerDrawable = layers.mLayerDrawable;
        int numberOfLayers = layerDrawable == null ? 0 : layerDrawable.getNumberOfLayers();
        if (layers.mHourIndex < 0 || layers.mHourIndex >= numberOfLayers) {
            layers.mHourIndex = -1;
        }
        if (layers.mMinuteIndex < 0 || layers.mMinuteIndex >= numberOfLayers) {
            layers.mMinuteIndex = -1;
        }
        if (layers.mSecondIndex < 0 || layers.mSecondIndex >= numberOfLayers) {
            layers.mSecondIndex = -1;
        }
        return layers;
    }

    public static class Metadata {
        final int HOUR_LAYER_INDEX;
        final int MINUTE_LAYER_INDEX;
        final int SECOND_LAYER_INDEX;
        final int DEFAULT_HOUR;
        final int DEFAULT_MINUTE;
        final int DEFAULT_SECOND;

        public Metadata(int hourIndex, int minuteIndex, int secondIndex,
                        int defaultHour, int defaultMinute, int defaultSecond) {
            HOUR_LAYER_INDEX = hourIndex;
            MINUTE_LAYER_INDEX = minuteIndex;
            SECOND_LAYER_INDEX = secondIndex;
            DEFAULT_HOUR = defaultHour;
            DEFAULT_MINUTE = defaultMinute;
            DEFAULT_SECOND = defaultSecond;
        }
    }
}
