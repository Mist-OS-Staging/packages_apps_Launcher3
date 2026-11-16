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
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.provider.Settings;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.android.launcher3.Item;
import com.android.launcher3.LauncherPrefChangeListener;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.icons.pack.IconPackIcons;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomePreviewView extends View
        implements LauncherPrefChangeListener, LivePreviewValues.Listener {

    private static final int COLUMNS = 4;
    private static final int GRID_APPS = 8;
    private static final int MAX_APPS = GRID_APPS + COLUMNS;
    private static final String NOTIFICATION_BADGING = "notification_badging";
    private static final String STYLE_EXTENDED = "1";
    private static final String STYLE_CLOCK = "2";

    private static final Item[] WATCHED = {
            LauncherPrefs.ICON_SIZE, LauncherPrefs.FONT_SIZE, LauncherPrefs.ICON_PACK,
            LauncherPrefs.SHOW_DESKTOP_LABELS, LauncherPrefs.SHOW_STATUS_BAR,
            LauncherPrefs.SHOW_TOP_SHADOW, LauncherPrefs.SHOW_HOTSEAT_BG,
            LauncherPrefs.HOTSEAT_OPACITY, LauncherPrefs.DOCK_SEARCH,
            LauncherPrefs.QSB_STYLE_GOOGLE, LauncherPrefs.DOCK_THEME,
            LauncherPrefs.HOTSEAT_QSB_OPACITY, LauncherPrefs.HOTSEAT_QSB_GOOGLE_OPACITY,
            LauncherPrefs.HOTSEAT_QSB_STROKE_WIDTH, LauncherPrefs.SEARCH_RADIUS_SIZE,
            LauncherPrefs.SHOW_QUICKSPACE, LauncherPrefs.QUICKSPACE_UI_STYLE,
    };

    private final Paint mWallpaperPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mShadowPaint = new Paint();
    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint mLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private final List<Drawable> mIcons = new ArrayList<>();
    private final List<CharSequence> mLabels = new ArrayList<>();
    @Nullable
    private final Drawable mSearchIcon;
    private final int mOnSurface;
    private final int mOnSurfaceVariant;
    private final int mCardColor;
    private final int mPrimary;
    private final int mPrimaryContainer;
    private final int mPreviewColor;
    private final float mRadius;
    private final float mPadding;
    private final float mBaseIconSize;
    private final String mSearchLabel;
    private final Typeface mMediumBold = Typeface.create("sans-serif-medium", Typeface.BOLD);
    private final Typeface mMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface mRegular = Typeface.create("sans-serif", Typeface.NORMAL);

    private final AnimatedValue mIconScale;
    private final AnimatedValue mFontScale;
    private final AnimatedValue mLabelsShown;
    private final AnimatedValue mStatusBar;
    private final AnimatedValue mTopShadow;
    private final AnimatedValue mGlance;
    private final AnimatedValue mGlanceHeight;
    private final AnimatedValue mDockBackground;
    private final AnimatedValue mSearch;
    private final AnimatedValue mSearchAlpha;
    private final AnimatedValue mSearchRadius;
    private final AnimatedValue mSearchStroke;
    private final AnimatedValue mGoogle;
    private final AnimatedValue mThemed;
    private final AnimatedValue mBump;

    private String mGlanceStyle = "0";
    private String mDateText = "";
    private String mTimeText = "";
    private boolean mShowDots = true;
    private boolean mLoaded;

    public HomePreviewView(Context context) {
        this(context, null);
    }

    public HomePreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mRadius = dp(32);
        mPadding = dp(14);
        mBaseIconSize = dp(34);
        mOnSurface = context.getColor(R.color.settings_expressive_on_surface);
        mOnSurfaceVariant = context.getColor(R.color.settings_expressive_on_surface_variant);
        mCardColor = context.getColor(R.color.settings_expressive_card);
        mPrimary = context.getColor(R.color.settings_expressive_primary);
        mPrimaryContainer = context.getColor(R.color.settings_expressive_primary_container);
        mPreviewColor = context.getColor(R.color.settings_expressive_preview);
        mSearchLabel = context.getString(R.string.settings_preview_search);
        mDotPaint.setColor(mPrimary);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mLabelPaint.setTextAlign(Paint.Align.CENTER);
        mSearchIcon = context.getDrawable(R.drawable.ic_settings_expressive_search);
        if (mSearchIcon != null) {
            mSearchIcon.mutate().setTint(mOnSurfaceVariant);
        }

        mIconScale = new AnimatedValue(1f, ExpressiveMotion.SPATIAL_STIFFNESS,
                ExpressiveMotion.SPATIAL_DAMPING);
        mFontScale = new AnimatedValue(1f, ExpressiveMotion.SPATIAL_STIFFNESS, 0.8f);
        mLabelsShown = new AnimatedValue(1f, 500f, 0.85f);
        mStatusBar = new AnimatedValue(1f, 500f, 0.8f);
        mTopShadow = new AnimatedValue(1f, 300f, 1f);
        mGlance = new AnimatedValue(1f, ExpressiveMotion.SPATIAL_STIFFNESS, 0.75f);
        mGlanceHeight = new AnimatedValue(0f, ExpressiveMotion.SPATIAL_STIFFNESS, 0.75f);
        mDockBackground = new AnimatedValue(0f, 500f, 0.9f);
        mSearch = new AnimatedValue(1f, ExpressiveMotion.SPATIAL_STIFFNESS,
                ExpressiveMotion.SPATIAL_DAMPING);
        mSearchAlpha = new AnimatedValue(1f, 700f, 1f);
        mSearchRadius = new AnimatedValue(1f, 700f, 0.75f);
        mSearchStroke = new AnimatedValue(0f, 700f, 0.8f);
        mGoogle = new AnimatedValue(1f, 500f, 0.9f);
        mThemed = new AnimatedValue(0f, 500f, 1f);
        mBump = new AnimatedValue(0f, 900f, 0.35f);
        readSettings(false);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LauncherPrefs.get(getContext()).addListener(this, WATCHED);
        LivePreviewValues.addListener(this);
        readSettings(false);
    }

    @Override
    protected void onDetachedFromWindow() {
        LivePreviewValues.removeListener(this);
        LauncherPrefs.get(getContext()).removeListener(this, WATCHED);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            loadApps();
            readSettings(mLoaded);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWallpaperPaint.setShader(new LinearGradient(0, 0, 0, h,
                mPrimaryContainer, mPreviewColor, Shader.TileMode.CLAMP));
        mShadowPaint.setShader(new LinearGradient(0, 0, 0, dp(40),
                Color.argb(70, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP));
    }

    @Override
    public void onPrefChanged(String key) {
        if (LauncherPrefs.ICON_PACK.getSharedPrefKey().equals(key)) {
            loadApps();
        }
        readSettings(true);
        mBump.jump(1f, 0f);
    }

    @Override
    public void onLiveValueChanged(String key) {
        readSettings(true);
    }

    public void refresh() {
        readSettings(true);
    }

    private void readSettings(boolean animate) {
        Context context = getContext();
        LauncherPrefs prefs = LauncherPrefs.get(context);

        int iconSize = live(LauncherPrefs.ICON_SIZE, prefs.get(LauncherPrefs.ICON_SIZE));
        mIconScale.set(clamp(iconSize / 100f, 0.5f, 1.5f), animate);
        int fontSize = live(LauncherPrefs.FONT_SIZE, prefs.get(LauncherPrefs.FONT_SIZE));
        mFontScale.set(clamp(fontSize / 100f, 0.5f, 1.5f), animate);
        mLabelsShown.set(prefs.get(LauncherPrefs.SHOW_DESKTOP_LABELS) ? 1f : 0f, animate);
        mStatusBar.set(prefs.get(LauncherPrefs.SHOW_STATUS_BAR) ? 1f : 0f, animate);
        mTopShadow.set(prefs.get(LauncherPrefs.SHOW_TOP_SHADOW) ? 1f : 0f, animate);

        boolean glance = prefs.get(LauncherPrefs.SHOW_QUICKSPACE);
        mGlanceStyle = prefs.get(LauncherPrefs.QUICKSPACE_UI_STYLE);
        mGlance.set(glance ? 1f : 0f, animate);
        mGlanceHeight.set(glanceHeight(mGlanceStyle), animate);

        boolean dockBackground = prefs.get(LauncherPrefs.SHOW_HOTSEAT_BG);
        int dockOpacity = live(LauncherPrefs.HOTSEAT_OPACITY,
                prefs.get(LauncherPrefs.HOTSEAT_OPACITY));
        mDockBackground.set(dockBackground ? clamp(dockOpacity / 100f, 0f, 1f) : 0f, animate);

        boolean google = prefs.get(LauncherPrefs.QSB_STYLE_GOOGLE);
        mSearch.set(prefs.get(LauncherPrefs.DOCK_SEARCH) ? 1f : 0f, animate);
        mGoogle.set(google ? 1f : 0f, animate);
        mThemed.set(prefs.get(LauncherPrefs.DOCK_THEME) ? 1f : 0f, animate);
        int searchOpacity = google
                ? live(LauncherPrefs.HOTSEAT_QSB_GOOGLE_OPACITY,
                        prefs.get(LauncherPrefs.HOTSEAT_QSB_GOOGLE_OPACITY))
                : live(LauncherPrefs.HOTSEAT_QSB_OPACITY,
                        prefs.get(LauncherPrefs.HOTSEAT_QSB_OPACITY));
        mSearchAlpha.set(clamp(searchOpacity / 100f, 0f, 1f), animate);
        int radius = live(LauncherPrefs.SEARCH_RADIUS_SIZE,
                prefs.get(LauncherPrefs.SEARCH_RADIUS_SIZE));
        mSearchRadius.set(clamp(radius / 100f, 0f, 1f), animate);
        int stroke = live(LauncherPrefs.HOTSEAT_QSB_STROKE_WIDTH,
                prefs.get(LauncherPrefs.HOTSEAT_QSB_STROKE_WIDTH));
        mSearchStroke.set(Math.max(0, stroke), animate);

        mShowDots = Settings.Secure.getInt(context.getContentResolver(),
                NOTIFICATION_BADGING, 1) == 1;

        Date now = new Date();
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        mDateText = DateFormat.format(
                DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), now).toString();
        mTimeText = DateFormat.format(DateFormat.is24HourFormat(context) ? "HH:mm" : "h:mm",
                now).toString();
        invalidate();
    }

    private static int live(Item item, int fallback) {
        return LivePreviewValues.get(item.getSharedPrefKey(), fallback);
    }

    private float glanceHeight(String style) {
        if (STYLE_CLOCK.equals(style)) {
            return dp(64);
        } else if (STYLE_EXTENDED.equals(style)) {
            return dp(50);
        }
        return dp(34);
    }

    private void loadApps() {
        Context context = getContext().getApplicationContext();
        int iconDpi = getResources().getDisplayMetrics().densityDpi;
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
                    Drawable icon = IconPackIcons.getIcon(context, info.getActivityInfo(),
                            iconDpi, () -> info.getIcon(iconDpi));
                    icons.add(icon != null ? icon : info.getIcon(iconDpi));
                    labels.add(info.getLabel());
                } catch (RuntimeException e) {
                    try {
                        icons.add(info.getIcon(iconDpi));
                        labels.add(info.getLabel());
                    } catch (RuntimeException ignored) {
                    }
                }
            }
            post(() -> {
                mIcons.clear();
                mIcons.addAll(icons);
                mLabels.clear();
                mLabels.addAll(labels);
                mLoaded = true;
                invalidate();
            });
        });
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        mRect.set(0, 0, w, h);
        canvas.drawRoundRect(mRect, mRadius, mRadius, mWallpaperPaint);

        float shadow = mTopShadow.get();
        if (shadow > 0.01f) {
            mShadowPaint.setAlpha(Math.round(255 * clamp(shadow, 0f, 1f)));
            canvas.save();
            canvas.clipRect(0, 0, w, dp(40));
            mRect.set(0, 0, w, h);
            canvas.drawRoundRect(mRect, mRadius, mRadius, mShadowPaint);
            canvas.restore();
        }

        float bump = 1f + 0.02f * mBump.get();
        canvas.save();
        canvas.scale(bump, bump, w / 2f, h / 2f);

        float top = dp(10);
        top = drawStatusBar(canvas, w, top);
        top = drawGlance(canvas, w, top);

        float bottom = h - dp(12);
        float dockBottom = drawSearch(canvas, w, bottom);
        float dockTop = drawDock(canvas, w, dockBottom);
        drawGrid(canvas, w, top + dp(4), dockTop - dp(6));
        canvas.restore();
    }

    private float drawStatusBar(Canvas canvas, float w, float top) {
        float progress = clamp(mStatusBar.get(), 0f, 1f);
        float height = dp(16) * progress;
        if (progress > 0.01f) {
            int alpha = Math.round(255 * progress);
            mTextPaint.setTypeface(Typeface.DEFAULT_BOLD);
            mTextPaint.setTextSize(sp(10));
            mTextPaint.setTextAlign(Paint.Align.LEFT);
            mTextPaint.setColor(mOnSurface);
            mTextPaint.setAlpha(alpha);
            float baseline = top + height / 2f
                    - (mTextPaint.descent() + mTextPaint.ascent()) / 2f;
            canvas.drawText(mTimeText, mPadding + dp(4), baseline, mTextPaint);
            mFillPaint.setColor(mOnSurface);
            mFillPaint.setAlpha(alpha);
            float cy = top + height / 2f;
            float right = w - mPadding - dp(4);
            mRect.set(right - dp(16), cy - dp(4), right, cy + dp(4));
            canvas.drawRoundRect(mRect, dp(2), dp(2), mFillPaint);
            canvas.drawCircle(right - dp(24), cy, dp(3), mFillPaint);
            canvas.drawCircle(right - dp(33), cy, dp(3), mFillPaint);
        }
        return top + height;
    }

    private float drawGlance(Canvas canvas, float w, float top) {
        float progress = clamp(mGlance.get(), 0f, 1f);
        float height = mGlanceHeight.get() * progress;
        if (progress < 0.01f) {
            return top;
        }
        int alpha = Math.round(255 * progress);
        float left = mPadding + dp(6);
        canvas.save();
        canvas.clipRect(0, top, w, top + height);
        canvas.translate(0, -dp(10) * (1f - progress));
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setColor(mOnSurface);
        mTextPaint.setAlpha(alpha);
        if (STYLE_CLOCK.equals(mGlanceStyle)) {
            mTextPaint.setTypeface(mMediumBold);
            mTextPaint.setTextSize(sp(36));
            float baseline = top + dp(40);
            canvas.drawText(mTimeText, left, baseline, mTextPaint);
            mTextPaint.setTypeface(Typeface.DEFAULT);
            mTextPaint.setTextSize(sp(11));
            mTextPaint.setColor(mOnSurfaceVariant);
            mTextPaint.setAlpha(alpha);
            canvas.drawText(mDateText, left + dp(2), baseline + sp(16), mTextPaint);
        } else if (STYLE_EXTENDED.equals(mGlanceStyle)) {
            mTextPaint.setTypeface(mRegular);
            mTextPaint.setTextSize(sp(20));
            float baseline = top + dp(24);
            canvas.drawText(mDateText, left, baseline, mTextPaint);
            mFillPaint.setColor(mOnSurfaceVariant);
            mFillPaint.setAlpha(Math.round(alpha * 0.45f));
            float lineTop = baseline + dp(9);
            mRect.set(left, lineTop, left + w * 0.38f, lineTop + dp(6));
            canvas.drawRoundRect(mRect, dp(3), dp(3), mFillPaint);
        } else {
            mTextPaint.setTypeface(mMedium);
            mTextPaint.setTextSize(sp(14));
            float baseline = top + dp(22);
            canvas.drawText(mDateText, left, baseline, mTextPaint);
            float textEnd = left + mTextPaint.measureText(mDateText) + dp(10);
            mFillPaint.setColor(mPrimary);
            mFillPaint.setAlpha(alpha);
            canvas.drawCircle(textEnd + dp(5), baseline - sp(5), dp(5), mFillPaint);
        }
        canvas.restore();
        return top + height;
    }

    private float drawSearch(Canvas canvas, float w, float bottom) {
        float progress = mSearch.get();
        float visible = clamp(progress, 0f, 1f);
        float height = dp(30);
        if (visible < 0.01f) {
            return bottom;
        }
        float slide = dp(14) * (1f - progress);
        float left = mPadding;
        float right = w - mPadding;
        float searchTop = bottom - height + slide;
        mRect.set(left, searchTop, right, searchTop + height);
        float corner = (height / 2f) * clamp(mSearchRadius.get(), 0f, 1.2f);
        int background = blend(mCardColor, mPrimaryContainer, clamp(mThemed.get(), 0f, 1f));
        mFillPaint.setColor(background);
        mFillPaint.setAlpha(Math.round(255 * visible * clamp(mSearchAlpha.get(), 0f, 1f)));
        canvas.drawRoundRect(mRect, corner, corner, mFillPaint);

        float stroke = mSearchStroke.get();
        if (stroke > 0.05f) {
            mStrokePaint.setColor(mPrimary);
            mStrokePaint.setAlpha(Math.round(255 * visible));
            mStrokePaint.setStrokeWidth(dp(0.5f) * stroke);
            canvas.drawRoundRect(mRect, corner, corner, mStrokePaint);
        }

        float google = clamp(mGoogle.get(), 0f, 1f);
        float cy = mRect.centerY();
        int contentAlpha = Math.round(255 * visible);
        if (google > 0.01f) {
            mTextPaint.setTypeface(mMediumBold);
            mTextPaint.setTextSize(sp(15));
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setColor(0xFF4285F4);
            mTextPaint.setAlpha(Math.round(contentAlpha * google));
            float gx = left + dp(18);
            canvas.drawText("G", gx, cy - (mTextPaint.descent() + mTextPaint.ascent()) / 2f,
                    mTextPaint);
            float micX = right - dp(18);
            mFillPaint.setColor(0xFF4285F4);
            mFillPaint.setAlpha(Math.round(contentAlpha * google));
            mRect.set(micX - dp(3), cy - dp(7), micX + dp(3), cy + dp(3));
            canvas.drawRoundRect(mRect, dp(3), dp(3), mFillPaint);
            mFillPaint.setColor(0xFFEA4335);
            mFillPaint.setAlpha(Math.round(contentAlpha * google));
            canvas.drawCircle(micX - dp(14), cy, dp(4), mFillPaint);
        }
        if (google < 0.99f) {
            int plainAlpha = Math.round(contentAlpha * (1f - google));
            int glyph = Math.round(dp(16));
            int glyphLeft = Math.round(left + dp(12));
            int glyphTop = Math.round(cy - glyph / 2f);
            if (mSearchIcon != null) {
                mSearchIcon.setAlpha(plainAlpha);
                mSearchIcon.setBounds(glyphLeft, glyphTop, glyphLeft + glyph, glyphTop + glyph);
                mSearchIcon.draw(canvas);
            }
            mTextPaint.setTypeface(Typeface.DEFAULT);
            mTextPaint.setTextSize(sp(12));
            mTextPaint.setTextAlign(Paint.Align.LEFT);
            mTextPaint.setColor(mOnSurfaceVariant);
            mTextPaint.setAlpha(plainAlpha);
            canvas.drawText(mSearchLabel,
                    glyphLeft + glyph + dp(8),
                    cy - (mTextPaint.descent() + mTextPaint.ascent()) / 2f, mTextPaint);
        }
        return bottom - (height + dp(8)) * progress;
    }

    private float drawDock(Canvas canvas, float w, float bottom) {
        float iconSize = mBaseIconSize * mIconScale.get();
        float height = iconSize + dp(14);
        float top = bottom - height;
        float background = clamp(mDockBackground.get(), 0f, 1f);
        if (background > 0.01f) {
            mFillPaint.setColor(mCardColor);
            mFillPaint.setAlpha(Math.round(255 * background));
            mRect.set(mPadding * 0.6f, top, w - mPadding * 0.6f, bottom);
            canvas.drawRoundRect(mRect, dp(22), dp(22), mFillPaint);
        }
        float cellWidth = (w - mPadding * 2) / COLUMNS;
        float iconTop = top + (height - iconSize) / 2f;
        for (int col = 0; col < COLUMNS; col++) {
            int index = GRID_APPS + col;
            Drawable icon = index < mIcons.size() ? mIcons.get(index)
                    : (col < mIcons.size() ? mIcons.get(col) : null);
            if (icon == null) {
                continue;
            }
            float cx = mPadding + cellWidth * col + cellWidth / 2f;
            drawIcon(canvas, icon, cx, iconTop, iconSize);
        }
        return top;
    }

    private void drawGrid(Canvas canvas, float w, float top, float limit) {
        float cellWidth = (w - mPadding * 2) / COLUMNS;
        float iconSize = mBaseIconSize * mIconScale.get();
        float labels = clamp(mLabelsShown.get(), 0f, 1f);
        float fontScale = mFontScale.get();
        mLabelPaint.setTextSize(sp(9.5f) * fontScale);
        mLabelPaint.setColor(mOnSurface);
        float labelHeight = (sp(13) * fontScale) * labels;
        float rowHeight = iconSize + labelHeight + dp(8);
        int count = Math.min(GRID_APPS, mIcons.size());
        for (int i = 0; i < count; i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            float iconTop = top + row * rowHeight;
            if (iconTop + iconSize + labelHeight > limit) {
                break;
            }
            float cx = mPadding + cellWidth * col + cellWidth / 2f;
            drawIcon(canvas, mIcons.get(i), cx, iconTop, iconSize);
            if (i == 0 && mShowDots) {
                float dotRadius = dp(4.5f);
                float dx = cx + iconSize / 2f - dotRadius;
                float dy = iconTop + dotRadius;
                mFillPaint.setColor(mPreviewColor);
                mFillPaint.setAlpha(255);
                canvas.drawCircle(dx, dy, dotRadius + dp(1.5f), mFillPaint);
                canvas.drawCircle(dx, dy, dotRadius, mDotPaint);
            }
            if (labels > 0.01f && i < mLabels.size()) {
                mLabelPaint.setAlpha(Math.round(255 * labels));
                CharSequence label = TextUtils.ellipsize(mLabels.get(i), mLabelPaint,
                        cellWidth - dp(4), TextUtils.TruncateAt.END);
                canvas.drawText(label, 0, label.length(), cx,
                        iconTop + iconSize + sp(11) * fontScale, mLabelPaint);
            }
        }
    }

    private static void drawIcon(Canvas canvas, Drawable icon, float cx, float top, float size) {
        icon.setBounds(Math.round(cx - size / 2f), Math.round(top),
                Math.round(cx + size / 2f), Math.round(top + size));
        icon.draw(canvas);
    }

    private static int blend(int from, int to, float t) {
        int a = Math.round(Color.alpha(from) + (Color.alpha(to) - Color.alpha(from)) * t);
        int r = Math.round(Color.red(from) + (Color.red(to) - Color.red(from)) * t);
        int g = Math.round(Color.green(from) + (Color.green(to) - Color.green(from)) * t);
        int b = Math.round(Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t);
        return Color.argb(a, r, g, b);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float dp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }

    private final class AnimatedValue {

        private final FloatValueHolder mHolder;
        private final SpringAnimation mSpring;

        AnimatedValue(float start, float stiffness, float damping) {
            mHolder = new FloatValueHolder(start);
            mSpring = new SpringAnimation(mHolder);
            mSpring.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
            mSpring.setSpring(new SpringForce(start)
                    .setStiffness(stiffness)
                    .setDampingRatio(damping));
            mSpring.addUpdateListener((animation, value, velocity) -> invalidate());
        }

        float get() {
            return mHolder.getValue();
        }

        void set(float target, boolean animate) {
            if (animate && isAttachedToWindow()) {
                mSpring.animateToFinalPosition(target);
            } else {
                mSpring.cancel();
                mHolder.setValue(target);
                mSpring.getSpring().setFinalPosition(target);
            }
        }

        void jump(float from, float to) {
            if (!isAttachedToWindow()) {
                return;
            }
            mSpring.cancel();
            mHolder.setValue(from);
            mSpring.animateToFinalPosition(to);
        }
    }
}
