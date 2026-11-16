/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings.expressive;

import android.graphics.Path;

public enum ExpressiveShape {
    CIRCLE(1, 0f),
    COOKIE(9, 0.07f),
    CLOVER(4, 0.16f),
    BURST(8, 0.10f),
    PENTAGON(5, 0.12f),
    FLOWER(6, 0.12f);

    public final int lobes;
    public final float depth;

    ExpressiveShape(int lobes, float depth) {
        this.lobes = lobes;
        this.depth = depth;
    }

    public void buildPath(Path path, float width, float height) {
        path.reset();
        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(width, height) / 2f;
        int points = Math.max(96, Math.round(radius * 1.5f));
        for (int i = 0; i <= points; i++) {
            double t = Math.PI * 2 * i / points;
            double r = radius * (1 - depth + depth * Math.cos(lobes * t));
            float x = (float) (cx + r * Math.cos(t));
            float y = (float) (cy + r * Math.sin(t));
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
    }
}
