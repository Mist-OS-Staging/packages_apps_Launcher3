/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings.expressive;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.android.launcher3.R;

import java.util.Arrays;
import java.util.Comparator;

public class RecentsStylePreviewView extends View {

    public static final String STYLE_DEFAULT = "default";
    public static final String STYLE_GRID = "oneui_grid";
    public static final String STYLE_STACK = "oneui_stack";
    public static final String STYLE_IOS = "ios";
    public static final String STYLE_HORIZONTAL = "miui_horizontal";

    private static final int CARD_COUNT = 6;
    private static final long LOOP_PERIOD_MS = 3400L;
    private static final long LOOP_RETURN_MS = 1500L;
    private static final int[] HEADER_COLORS = {
            R.color.homepage_blue_bg, R.color.homepage_orange_bg, R.color.homepage_yellow_bg,
            R.color.homepage_red_bg, R.color.homepage_cyan_bg, R.color.homepage_blue_bg,
    };

    private final Paint mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mCardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mHeaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint mPillText = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final int[] mHeaderColors = new int[CARD_COUNT];
    private final RectF mRect = new RectF();
    private final Path mFrameClip = new Path();
    private final Path mCardClip = new Path();
    private final float[][] mTransforms = new float[CARD_COUNT][6];
    private final Integer[] mOrder = new Integer[CARD_COUNT];
    private final FloatValueHolder mScrollHolder = new FloatValueHolder(0f);
    private final SpringAnimation mScrollSpring;
    private final SpringAnimation mSelectSpring;
    private String mStyle = STYLE_DEFAULT;
    private float mScroll;
    private float mSelectProgress;
    private boolean mShowClearAll = true;
    private long mLoopDelay;
    private boolean mLooping;
    private final Runnable mReturn = new Runnable() {
        @Override
        public void run() {
            mScrollSpring.animateToFinalPosition(0f);
        }
    };
    private final Runnable mLoop = new Runnable() {
        @Override
        public void run() {
            if (!mLooping) {
                return;
            }
            mScrollSpring.animateToFinalPosition(STYLE_GRID.equals(mStyle) ? 1f : -1.15f);
            postDelayed(mReturn, LOOP_RETURN_MS);
            postDelayed(this, LOOP_PERIOD_MS);
        }
    };

    public RecentsStylePreviewView(Context context) {
        this(context, null);
    }

    public RecentsStylePreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mFramePaint.setColor(context.getColor(R.color.settings_expressive_preview));
        mBorderPaint.setColor(context.getColor(R.color.settings_expressive_primary));
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(dp(3));
        mCardPaint.setColor(context.getColor(R.color.settings_expressive_card));
        mLinePaint.setColor(context.getColor(R.color.settings_expressive_surface_highest));
        mPillPaint.setColor(context.getColor(R.color.settings_expressive_card));
        mPillText.setColor(context.getColor(R.color.settings_expressive_on_surface_variant));
        mPillText.setTextSize(dp(8));
        mPillText.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < CARD_COUNT; i++) {
            mHeaderColors[i] = context.getColor(HEADER_COLORS[i]);
            mOrder[i] = i;
        }

        mScrollSpring = new SpringAnimation(mScrollHolder);
        mScrollSpring.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
        mScrollSpring.setSpring(new SpringForce(0f).setStiffness(110f).setDampingRatio(0.78f));
        mScrollSpring.addUpdateListener((a, value, velocity) -> {
            mScroll = value;
            invalidate();
        });

        FloatValueHolder select = new FloatValueHolder(0f);
        mSelectSpring = new SpringAnimation(select);
        mSelectSpring.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
        mSelectSpring.setSpring(new SpringForce(0f)
                .setStiffness(ExpressiveMotion.SPATIAL_STIFFNESS).setDampingRatio(0.6f));
        mSelectSpring.addUpdateListener((a, value, velocity) -> {
            mSelectProgress = value;
            invalidate();
        });
    }

    public void setStyle(String style) {
        mStyle = style;
        invalidate();
    }

    public void setShowClearAll(boolean show) {
        mShowClearAll = show;
        invalidate();
    }

    public void setChosen(boolean selected, boolean animate) {
        if (animate) {
            mSelectSpring.animateToFinalPosition(selected ? 1f : 0f);
        } else {
            mSelectSpring.cancel();
            mSelectProgress = selected ? 1f : 0f;
            invalidate();
        }
    }

    public void setLoopDelay(long delayMs) {
        mLoopDelay = delayMs;
    }

    public void playDemo() {
        stopLoop();
        mLooping = isAttachedToWindow() && isShown();
        if (mLooping) {
            mLoop.run();
        }
    }

    private void startLoop(long delayMs) {
        if (mLooping) {
            return;
        }
        mLooping = true;
        postDelayed(mLoop, delayMs);
    }

    private void stopLoop() {
        mLooping = false;
        removeCallbacks(mLoop);
        removeCallbacks(mReturn);
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        if (isVisible && isAttachedToWindow()) {
            startLoop(mLoopDelay);
        } else {
            stopLoop();
            mScrollSpring.animateToFinalPosition(0f);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        stopLoop();
        mScrollSpring.cancel();
        mScrollHolder.setValue(0f);
        mScroll = 0f;
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float radius = dp(22) + dp(12) * mSelectProgress;
        mRect.set(0, 0, w, h);
        canvas.save();
        mFrameClip.reset();
        mFrameClip.addRoundRect(mRect, radius, radius, Path.Direction.CW);
        canvas.clipPath(mFrameClip);
        canvas.drawRoundRect(mRect, radius, radius, mFramePaint);

        for (int i = 0; i < CARD_COUNT; i++) {
            computeTransform(i, w, h, mTransforms[i]);
        }
        Arrays.sort(mOrder, Comparator.comparingDouble(i -> mTransforms[i][4]));
        for (Integer i : mOrder) {
            drawCard(canvas, i, mTransforms[i]);
        }

        if (mShowClearAll) {
            String clearAll = getContext().getString(R.string.recents_clear_all_title);
            float pillWidth = mPillText.measureText(clearAll) + dp(14);
            float pillHeight = dp(16);
            float bottom = h - dp(9);
            mRect.set((w - pillWidth) / 2f, bottom - pillHeight, (w + pillWidth) / 2f, bottom);
            canvas.drawRoundRect(mRect, pillHeight / 2f, pillHeight / 2f, mPillPaint);
            float baseline = mRect.centerY() - (mPillText.descent() + mPillText.ascent()) / 2f;
            canvas.drawText(clearAll, mRect.centerX(), baseline, mPillText);
        }
        canvas.restore();

        if (mSelectProgress > 0.01f) {
            mBorderPaint.setAlpha(Math.round(255 * Math.min(1f, mSelectProgress)));
            float inset = mBorderPaint.getStrokeWidth() / 2f;
            mRect.set(inset, inset, w - inset, h - inset);
            canvas.drawRoundRect(mRect, radius - inset, radius - inset, mBorderPaint);
        }
    }

    private void computeTransform(int i, float w, float h, float[] out) {
        float cx = w / 2f;
        float cy = h * 0.45f;
        float cw = w * 0.58f;
        float ch = h * 0.6f;
        float gap = w * 0.06f;
        float d = i - (2 + mScroll);
        float a = Math.abs(d);
        float sign = Math.signum(d);
        float x = cx;
        float y = cy;
        float scale = 1f;
        float z = i;
        float alpha = 1f;
        switch (mStyle) {
            case STYLE_GRID: {
                int page = i / 4;
                int slot = i % 4;
                int col = slot % 2;
                int row = slot / 2;
                cw = w * 0.4f;
                ch = h * 0.36f;
                x = w * (col == 0 ? 0.28f : 0.72f) + (page - mScroll) * w;
                y = h * (row == 0 ? 0.25f : 0.64f);
                break;
            }
            case STYLE_IOS: {
                x = cx + sign * cw * 0.42f
                        * (0.96f * (1f - (float) Math.pow(0.6, a)) / 0.4f);
                scale = 1f - 0.15f * Math.min(a, 1f);
                alpha = a > 3f ? 0f : 1f;
                break;
            }
            case STYLE_HORIZONTAL: {
                scale = a < 1 ? 1f - 0.1f * a
                        : a < 2 ? 0.9f - 0.1f * (a - 1) : Math.max(0.6f, 0.8f - 0.1f * (a - 2));
                x = cx + sign * a * cw * 0.74f;
                z = 20f - a * 10f;
                alpha = a > 2.6f ? 0f : 1f;
                break;
            }
            case STYLE_STACK: {
                if (d >= 0) {
                    x = cx + d * (cw + gap);
                } else {
                    float pile = Math.min(a, 3.5f);
                    x = cx - pile * cw * 0.066f;
                    y = cy - pile * h * 0.04f;
                    scale = Math.max(0.74f, 1f - 0.1f * a);
                    alpha = a > 3.5f ? 0f : 1f;
                }
                break;
            }
            default: {
                x = cx + d * (cw + gap);
                alpha = a > 2f ? 0f : 1f;
                break;
            }
        }
        out[0] = x;
        out[1] = y;
        out[2] = cw * scale;
        out[3] = ch * scale;
        out[4] = z;
        out[5] = alpha;
    }

    private void drawCard(Canvas canvas, int index, float[] t) {
        if (t[5] <= 0f) {
            return;
        }
        float left = t[0] - t[2] / 2f;
        float top = t[1] - t[3] / 2f;
        float corner = dp(9) * (t[2] / (getWidth() * 0.58f));
        int alpha = Math.round(255 * t[5]);
        mCardPaint.setAlpha(alpha);
        mRect.set(left, top, left + t[2], top + t[3]);
        canvas.drawRoundRect(mRect, corner, corner, mCardPaint);

        canvas.save();
        mCardClip.reset();
        mCardClip.addRoundRect(mRect, corner, corner, Path.Direction.CW);
        canvas.clipPath(mCardClip);
        mHeaderPaint.setColor(mHeaderColors[index]);
        mHeaderPaint.setAlpha(alpha);
        canvas.drawRect(left, top, left + t[2], top + t[3] * 0.18f, mHeaderPaint);
        canvas.restore();

        mLinePaint.setAlpha(alpha);
        float lineHeight = t[3] * 0.035f;
        float lineLeft = left + t[2] * 0.1f;
        float[] widths = {0.8f, 0.5f, 0.65f};
        for (int i = 0; i < widths.length; i++) {
            float lineTop = top + t[3] * (0.28f + i * 0.09f);
            mRect.set(lineLeft, lineTop, lineLeft + t[2] * widths[i], lineTop + lineHeight);
            canvas.drawRoundRect(mRect, lineHeight / 2f, lineHeight / 2f, mLinePaint);
        }
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }
}
