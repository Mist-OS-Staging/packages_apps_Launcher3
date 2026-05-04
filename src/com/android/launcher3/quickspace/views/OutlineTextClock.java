/*
 * Copyright (C) 2026 crDroid Android Project
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
package com.android.launcher3.quickspace.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.TextClock;

import com.android.launcher3.R;

/**
 * A TextClock that renders text with stroke-only paint (no fill),
 * creating an outlined/hollow text effect.
 */
public class OutlineTextClock extends TextClock {

    private float mStrokeWidth;

    public OutlineTextClock(Context context) {
        this(context, null);
    }

    public OutlineTextClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OutlineTextClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mStrokeWidth = context.getResources().getDimension(R.dimen.quickspace_outline_stroke_width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = getPaint();

        // Save original values
        Paint.Style originalStyle = paint.getStyle();
        float originalStrokeWidth = paint.getStrokeWidth();

        // Draw outline only
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(mStrokeWidth);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);

        super.onDraw(canvas);

        // Restore original values
        paint.setStyle(originalStyle);
        paint.setStrokeWidth(originalStrokeWidth);
    }

    public void setOutlineStrokeWidth(float width) {
        mStrokeWidth = width;
        invalidate();
    }
}
