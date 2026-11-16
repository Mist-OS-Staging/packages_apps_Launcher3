/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings.expressive;

import static com.android.launcher3.util.Executors.UI_HELPER_EXECUTOR;

import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.provider.Settings;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;

public class HomePreviewView extends View {

    private static final int COLUMNS = 4;
    private static final int MAX_APPS = 8;
    private static final String NOTIFICATION_BADGING = "notification_badging";

    private final Paint mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDotRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint mLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint mDockTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private final List<Drawable> mIcons = new ArrayList<>();
    private final List<CharSequence> mLabels = new ArrayList<>();
    private final Drawable mSearchIcon;
    private final float mRadius;
    private final float mPadding;
    private final float mBaseIconSize;
    private final float mDockHeight;
    private final SpringAnimation mScaleSpring;
    private float mIconScale = 1f;
    private boolean mShowLabels = true;
    private boolean mShowDots = true;

    public HomePreviewView(Context context) {
        this(context, null);
    }

    public HomePreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mRadius = dp(32);
        mPadding = dp(14);
        mBaseIconSize = dp(36);
        mDockHeight = dp(44);
        mBackgroundPaint.setColor(context.getColor(R.color.settings_expressive_preview));
        mDockPaint.setColor(context.getColor(R.color.settings_expressive_card));
        mDotPaint.setColor(context.getColor(R.color.settings_expressive_primary));
        mDotRingPaint.setColor(context.getColor(R.color.settings_expressive_preview));
        int onSurface = context.getColor(R.color.settings_expressive_on_surface);
        int onSurfaceVariant = context.getColor(R.color.settings_expressive_on_surface_variant);
        mLabelPaint.setColor(onSurface);
        mLabelPaint.setTextSize(sp(10));
        mLabelPaint.setTextAlign(Paint.Align.CENTER);
        mDockTextPaint.setColor(onSurfaceVariant);
        mDockTextPaint.setTextSize(sp(13));
        mSearchIcon = context.getDrawable(R.drawable.ic_settings_expressive_search);
        if (mSearchIcon != null) {
            mSearchIcon.mutate().setTint(onSurfaceVariant);
        }
        FloatValueHolder holder = new FloatValueHolder(1f);
        mScaleSpring = new SpringAnimation(holder);
        mScaleSpring.setSpring(new SpringForce(1f)
                .setStiffness(ExpressiveMotion.SPATIAL_STIFFNESS)
                .setDampingRatio(ExpressiveMotion.SPATIAL_DAMPING));
        mScaleSpring.addUpdateListener((anim, value, velocity) -> {
            mIconScale = value;
            invalidate();
        });
        readSettings(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        loadApps();
    }

    public void refresh() {
        readSettings(true);
    }

    private void readSettings(boolean animate) {
        Context context = getContext();
        int size = LauncherPrefs.get(context).get(LauncherPrefs.ICON_SIZE);
        float scale = Math.max(0.6f, Math.min(1.4f, size / 100f));
        mShowLabels = LauncherPrefs.get(context).get(LauncherPrefs.SHOW_DESKTOP_LABELS);
        mShowDots = Settings.Secure.getInt(context.getContentResolver(),
                NOTIFICATION_BADGING, 1) == 1;
        if (animate) {
            mScaleSpring.animateToFinalPosition(scale);
        } else {
            mIconScale = scale;
        }
        invalidate();
    }

    private void loadApps() {
        Context context = getContext().getApplicationContext();
        UI_HELPER_EXECUTOR.execute(() -> {
            LauncherApps launcherApps = context.getSystemService(LauncherApps.class);
            List<LauncherActivityInfo> infos = launcherApps == null ? new ArrayList<>()
                    : new ArrayList<>(launcherApps.getActivityList(null,
                            Process.myUserHandle()));
            Collator collator = Collator.getInstance();
            infos.sort((a, b) -> collator.compare(String.valueOf(a.getLabel()),
                    String.valueOf(b.getLabel())));
            List<Drawable> icons = new ArrayList<>();
            List<CharSequence> labels = new ArrayList<>();
            for (LauncherActivityInfo info : infos) {
                if (icons.size() >= MAX_APPS) {
                    break;
                }
                try {
                    icons.add(info.getIcon(0));
                    labels.add(info.getLabel());
                } catch (RuntimeException ignored) {
                }
            }
            post(() -> {
                mIcons.clear();
                mIcons.addAll(icons);
                mLabels.clear();
                mLabels.addAll(labels);
                invalidate();
            });
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        mRect.set(0, 0, w, h);
        canvas.drawRoundRect(mRect, mRadius, mRadius, mBackgroundPaint);

        float cellWidth = (w - mPadding * 2) / COLUMNS;
        float iconSize = mBaseIconSize * mIconScale;
        float labelHeight = mShowLabels ? sp(14) : 0;
        float rowHeight = iconSize + labelHeight + dp(8);
        float top = mPadding;
        for (int i = 0; i < mIcons.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            float cx = mPadding + cellWidth * col + cellWidth / 2f;
            float iconTop = top + row * rowHeight;
            if (iconTop + iconSize > h - mDockHeight - dp(14)) {
                break;
            }
            Drawable icon = mIcons.get(i);
            icon.setBounds(Math.round(cx - iconSize / 2f), Math.round(iconTop),
                    Math.round(cx + iconSize / 2f), Math.round(iconTop + iconSize));
            icon.draw(canvas);
            if (i == 0 && mShowDots) {
                float dotRadius = dp(5);
                float dx = cx + iconSize / 2f - dotRadius;
                float dy = iconTop + dotRadius;
                canvas.drawCircle(dx, dy, dotRadius + dp(2), mDotRingPaint);
                canvas.drawCircle(dx, dy, dotRadius, mDotPaint);
            }
            if (mShowLabels) {
                CharSequence label = TextUtils.ellipsize(mLabels.get(i), mLabelPaint,
                        cellWidth - dp(4), TextUtils.TruncateAt.END);
                canvas.drawText(label, 0, label.length(), cx,
                        iconTop + iconSize + sp(12), mLabelPaint);
            }
        }

        float dockLeft = dp(10);
        float dockTop = h - mDockHeight - dp(10);
        mRect.set(dockLeft, dockTop, w - dockLeft, dockTop + mDockHeight);
        canvas.drawRoundRect(mRect, mDockHeight / 2f, mDockHeight / 2f, mDockPaint);
        float iconLeft = dockLeft + dp(14);
        int glyph = Math.round(dp(20));
        int glyphTop = Math.round(dockTop + (mDockHeight - glyph) / 2f);
        if (mSearchIcon != null) {
            mSearchIcon.setBounds(Math.round(iconLeft), glyphTop,
                    Math.round(iconLeft) + glyph, glyphTop + glyph);
            mSearchIcon.draw(canvas);
        }
        String search = getContext().getString(R.string.settings_preview_search);
        float baseline = dockTop + mDockHeight / 2f
                - (mDockTextPaint.descent() + mDockTextPaint.ascent()) / 2f;
        canvas.drawText(search, iconLeft + glyph + dp(10), baseline, mDockTextPaint);
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}
