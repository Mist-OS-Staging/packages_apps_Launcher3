/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings.expressive;

import static com.android.launcher3.util.Executors.UI_HELPER_EXECUTOR;

import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
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
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FloatValueHolder;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.Item;
import com.android.launcher3.LauncherPrefChangeListener;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.icons.pack.IconPackIcons;
import com.android.launcher3.quickspace.QuickEventsController;
import com.android.launcher3.quickspace.QuickspaceClockFonts;
import com.android.launcher3.util.Themes;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomePreviewView extends View
        implements LauncherPrefChangeListener, LivePreviewValues.Listener {

    private static final String NOTIFICATION_BADGING = "notification_badging";
    private static final String STYLE_EXTENDED = "1";
    private static final String STYLE_CLOCK = "2";
    private static final int DEFAULT_COLUMNS = 4;
    private static final int DEFAULT_ROWS = 5;

    private static final Item[] WATCHED = {
            LauncherPrefs.ICON_SIZE, LauncherPrefs.FONT_SIZE, LauncherPrefs.ICON_PACK,
            LauncherPrefs.SHOW_DESKTOP_LABELS, LauncherPrefs.SHOW_STATUS_BAR,
            LauncherPrefs.SHOW_TOP_SHADOW, LauncherPrefs.SHOW_HOTSEAT_BG,
            LauncherPrefs.HOTSEAT_OPACITY, LauncherPrefs.DOCK_SEARCH,
            LauncherPrefs.QSB_STYLE_GOOGLE, LauncherPrefs.DOCK_THEME,
            LauncherPrefs.DOCK_MUSIC_SEARCH,
            LauncherPrefs.HOTSEAT_QSB_OPACITY, LauncherPrefs.HOTSEAT_QSB_GOOGLE_OPACITY,
            LauncherPrefs.HOTSEAT_QSB_STROKE_WIDTH, LauncherPrefs.SEARCH_RADIUS_SIZE,
            LauncherPrefs.SHOW_QUICKSPACE, LauncherPrefs.QUICKSPACE_UI_STYLE,
            LauncherPrefs.QUICKSPACE_CLOCK_HOUR_COLOR, LauncherPrefs.QUICKSPACE_CLOCK_MINUTE_COLOR,
            LauncherPrefs.QUICKSPACE_CLOCK_FONT, LauncherPrefs.QUICKSPACE_CLOCK_SYSTEM_FONT,
    };

    private final Context mLauncherContext;
    private final Paint mCardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mWallpaperPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint mGradientPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mShadowPaint = new Paint();
    private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint mLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private final RectF mPhone = new RectF();
    private final Rect mBitmapDst = new Rect();
    private final Path mPhoneClip = new Path();
    private final List<Drawable> mIcons = new ArrayList<>();
    private final List<CharSequence> mLabels = new ArrayList<>();
    private final Typeface mMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface mRegular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface mSlateRegular;
    private final Typeface mSlateMedium;
    private final Typeface mSlateBold;
    private Typeface mClockRegular;
    private Typeface mClockMedium;
    private Typeface mClockBold;
    private boolean mClockFontDirty = true;
    private int mClockHourColor = Color.WHITE;
    private int mClockMinuteColor = Color.WHITE;
    private String mDayText = "";
    private String mShortDateText = "";
    private String mHourText = "";
    private String mMinuteText = "";
    private final float mRadius;

    private final int mWorkspaceText;
    private final int mWorkspaceShadow;
    private final int mQsbFill;
    private final int mQsbFillThemed;
    private final int mQsbOuter;
    private final int mAccent;
    private final int mWallpaperTop;
    private final int mWallpaperBottom;
    @Nullable
    private final Drawable mAppseat;

    private final AnimatedValue mIconScale;
    private final AnimatedValue mFontScale;
    private final AnimatedValue mLabelsShown;
    private final AnimatedValue mStatusBar;
    private final AnimatedValue mTopShadow;
    private final AnimatedValue mGlance;
    private final AnimatedValue mDockBackground;
    private final AnimatedValue mSearch;
    private final AnimatedValue mSearchAlpha;
    private final AnimatedValue mGoogleAlpha;
    private final AnimatedValue mSearchRadius;
    private final AnimatedValue mSearchStroke;
    private final AnimatedValue mBump;

    private int mScreenWidth;
    private int mScreenHeight;
    private float mPhoneScale = 1f;
    private int mColumns = DEFAULT_COLUMNS;
    private int mRows = DEFAULT_ROWS;
    private int mHotseatCount = DEFAULT_COLUMNS;
    private String mGlanceStyle = "0";
    private String mDateText = "";
    private String mTimeText = "";
    private boolean mShowDots = true;
    private boolean mGoogleStyle = true;
    private boolean mThemed;
    private boolean mGsaEnabled;
    private boolean mGeminiInstalled;
    private boolean mMusicSearch;
    private boolean mLoaded;
    private int mSearchIconsKey = -1;
    @Nullable
    private Bitmap mWallpaper;
    private final SearchIcons mPlainIcons = new SearchIcons();
    private final SearchIcons mGoogleIcons = new SearchIcons();

    public HomePreviewView(Context context) {
        this(context, null);
    }

    public HomePreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mLauncherContext = new ContextThemeWrapper(context, Themes.getActivityThemeRes(context));
        mRadius = dp(32);
        mSlateRegular = loadFont("SlateForOnePlus-Regular", mRegular);
        mSlateMedium = loadFont("SlateForOnePlus-Medium", mMedium);
        mSlateBold = loadFont("SlateForOnePlus-Bold",
                Typeface.create("sans-serif", Typeface.BOLD));
        mClockRegular = mSlateRegular;
        mClockMedium = mSlateMedium;
        mClockBold = mSlateBold;
        mCardPaint.setColor(context.getColor(R.color.settings_expressive_preview));
        mFramePaint.setStyle(Paint.Style.STROKE);
        mFramePaint.setColor(context.getColor(R.color.settings_expressive_outline_variant));
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mLabelPaint.setTextAlign(Paint.Align.CENTER);

        mWorkspaceText = Themes.getAttrColor(mLauncherContext, R.attr.workspaceTextColor);
        mWorkspaceShadow = Themes.getAttrColor(mLauncherContext, R.attr.workspaceShadowColor);
        mQsbFill = Themes.getAttrColor(mLauncherContext, R.attr.qsbFillColor);
        mQsbFillThemed = Themes.getAttrColor(mLauncherContext, R.attr.qsbFillColorThemed);
        mQsbOuter = Themes.getAttrColor(mLauncherContext, R.attr.qsbOuterColor);
        mAccent = Themes.getColorAccent(mLauncherContext);
        mWallpaperTop = context.getColor(R.color.settings_expressive_primary_container);
        mWallpaperBottom = context.getColor(R.color.settings_expressive_surface_highest);
        Drawable appseat = mLauncherContext.getDrawable(R.drawable.bkg_appseat);
        mAppseat = appseat != null ? appseat.mutate() : null;

        WindowManager wm = context.getSystemService(WindowManager.class);
        if (wm != null) {
            Rect bounds = wm.getMaximumWindowMetrics().getBounds();
            mScreenWidth = Math.min(bounds.width(), bounds.height());
            mScreenHeight = Math.max(bounds.width(), bounds.height());
        }
        if (mScreenWidth <= 0 || mScreenHeight <= 0) {
            mScreenWidth = getResources().getDisplayMetrics().widthPixels;
            mScreenHeight = getResources().getDisplayMetrics().heightPixels;
        }
        try {
            InvariantDeviceProfile idp = InvariantDeviceProfile.INSTANCE.get(context);
            mColumns = Math.max(3, idp.numColumns);
            mRows = Math.max(4, idp.numRows);
            mHotseatCount = Math.max(3, Math.min(mColumns + 1, idp.numShownHotseatIcons));
        } catch (RuntimeException ignored) {
        }

        mIconScale = new AnimatedValue(1f, ExpressiveMotion.SPATIAL_STIFFNESS,
                ExpressiveMotion.SPATIAL_DAMPING);
        mFontScale = new AnimatedValue(1f, ExpressiveMotion.SPATIAL_STIFFNESS, 0.8f);
        mLabelsShown = new AnimatedValue(1f, 500f, 0.85f);
        mStatusBar = new AnimatedValue(1f, 500f, 0.8f);
        mTopShadow = new AnimatedValue(1f, 300f, 1f);
        mGlance = new AnimatedValue(1f, ExpressiveMotion.SPATIAL_STIFFNESS, 0.75f);
        mDockBackground = new AnimatedValue(0f, 500f, 0.9f);
        mSearch = new AnimatedValue(1f, ExpressiveMotion.SPATIAL_STIFFNESS,
                ExpressiveMotion.SPATIAL_DAMPING);
        mSearchAlpha = new AnimatedValue(1f, 700f, 1f);
        mGoogleAlpha = new AnimatedValue(1f, 700f, 1f);
        mSearchRadius = new AnimatedValue(1f, 700f, 0.75f);
        mSearchStroke = new AnimatedValue(0f, 700f, 0.8f);
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
            loadWallpaper();
            readSettings(mLoaded);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float inset = dp(12);
        float phoneHeight = h - inset * 2;
        float phoneWidth = phoneHeight * mScreenWidth / (float) mScreenHeight;
        if (phoneWidth > w - inset * 2) {
            phoneWidth = w - inset * 2;
            phoneHeight = phoneWidth * mScreenHeight / (float) mScreenWidth;
        }
        float left = (w - phoneWidth) / 2f;
        float top = (h - phoneHeight) / 2f;
        mPhone.set(left, top, left + phoneWidth, top + phoneHeight);
        mPhoneScale = phoneWidth / mScreenWidth;
        float corner = phoneWidth * 0.11f;
        mPhoneClip.reset();
        mPhoneClip.addRoundRect(mPhone, corner, corner, Path.Direction.CW);
        mFramePaint.setStrokeWidth(dp(1.5f));
        mGradientPaint.setShader(new LinearGradient(0, mPhone.top, 0, mPhone.bottom,
                mWallpaperTop, mWallpaperBottom, Shader.TileMode.CLAMP));
        mShadowPaint.setShader(new LinearGradient(0, 0, 0, dp(140),
                Color.argb(90, 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        loadWallpaper();
    }

    @Override
    public void onPrefChanged(String key) {
        if (LauncherPrefs.ICON_PACK.getSharedPrefKey().equals(key)) {
            loadApps();
        }
        if (LauncherPrefs.QUICKSPACE_CLOCK_FONT.getSharedPrefKey().equals(key)
                || LauncherPrefs.QUICKSPACE_CLOCK_SYSTEM_FONT.getSharedPrefKey().equals(key)) {
            mClockFontDirty = true;
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

        mGlanceStyle = prefs.get(LauncherPrefs.QUICKSPACE_UI_STYLE);
        mGlance.set(prefs.get(LauncherPrefs.SHOW_QUICKSPACE) ? 1f : 0f, animate);
        mClockHourColor = clockColor(prefs.get(LauncherPrefs.QUICKSPACE_CLOCK_HOUR_COLOR));
        mClockMinuteColor = clockColor(prefs.get(LauncherPrefs.QUICKSPACE_CLOCK_MINUTE_COLOR));
        if (mClockFontDirty) {
            mClockFontDirty = false;
            Typeface base = null;
            try {
                base = QuickspaceClockFonts.resolveBaseTypeface(context);
            } catch (RuntimeException ignored) {
            }
            if (base == null) {
                mClockRegular = mSlateRegular;
                mClockMedium = mSlateMedium;
                mClockBold = mSlateBold;
            } else {
                mClockRegular = base;
                mClockMedium = Typeface.create(base, 500, false);
                mClockBold = Typeface.create(base, 700, false);
            }
        }

        boolean dockBackground = prefs.get(LauncherPrefs.SHOW_HOTSEAT_BG);
        int dockOpacity = live(LauncherPrefs.HOTSEAT_OPACITY,
                prefs.get(LauncherPrefs.HOTSEAT_OPACITY));
        mDockBackground.set(dockBackground ? clamp(dockOpacity / 100f, 0f, 1f) : 0f, animate);

        mGoogleStyle = prefs.get(LauncherPrefs.QSB_STYLE_GOOGLE) && mGsaEnabled;
        mThemed = prefs.get(LauncherPrefs.DOCK_THEME);
        mMusicSearch = mGsaEnabled && prefs.get(LauncherPrefs.DOCK_MUSIC_SEARCH);
        mSearch.set(prefs.get(LauncherPrefs.DOCK_SEARCH) ? 1f : 0f, animate);
        mSearchAlpha.set(clamp(live(LauncherPrefs.HOTSEAT_QSB_OPACITY,
                prefs.get(LauncherPrefs.HOTSEAT_QSB_OPACITY)) / 100f, 0f, 1f), animate);
        mGoogleAlpha.set(clamp(live(LauncherPrefs.HOTSEAT_QSB_GOOGLE_OPACITY,
                prefs.get(LauncherPrefs.HOTSEAT_QSB_GOOGLE_OPACITY)) / 100f, 0f, 1f), animate);
        mSearchRadius.set(clamp(live(LauncherPrefs.SEARCH_RADIUS_SIZE,
                prefs.get(LauncherPrefs.SEARCH_RADIUS_SIZE)) / 100f, 0f, 1f), animate);
        mSearchStroke.set(Math.max(0, live(LauncherPrefs.HOTSEAT_QSB_STROKE_WIDTH,
                prefs.get(LauncherPrefs.HOTSEAT_QSB_STROKE_WIDTH))), animate);
        updateSearchIcons();

        mShowDots = Settings.Secure.getInt(context.getContentResolver(),
                NOTIFICATION_BADGING, 1) == 1;

        Date now = new Date();
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        mDateText = DateFormat.format(
                DateFormat.getBestDateTimePattern(locale, "EEEMMMd"), now).toString();
        boolean is24 = DateFormat.is24HourFormat(context);
        mTimeText = DateFormat.format(is24 ? "HH:mm" : "h:mm", now).toString();
        mHourText = DateFormat.format(is24 ? "H" : "h", now).toString();
        mMinuteText = DateFormat.format(":mm", now).toString();
        mDayText = QuickEventsController.getDayOfWeek(context);
        mShortDateText = QuickEventsController.getShortDate(context);
        invalidate();
    }

    private static int clockColor(int color) {
        if (Color.alpha(color) == 0) {
            return color == 0 ? Color.WHITE : (color | 0xFF000000);
        }
        return color;
    }

    private Typeface loadFont(String name, Typeface fallback) {
        try {
            int id = getResources().getIdentifier(name, "font", getContext().getPackageName());
            if (id == 0) {
                id = getResources().getIdentifier(name.replace('-', '_'), "font",
                        getContext().getPackageName());
            }
            Typeface typeface = id != 0 ? getResources().getFont(id) : null;
            return typeface != null ? typeface : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private void updateSearchIcons() {
        int key = (mGsaEnabled ? 1 : 0) | (mGeminiInstalled ? 2 : 0) | (mThemed ? 4 : 0)
                | (mMusicSearch ? 8 : 0);
        if (key == mSearchIconsKey) {
            return;
        }
        mSearchIconsKey = key;
        mPlainIcons.start = mGsaEnabled
                ? load(mThemed ? R.drawable.ic_super_g_themed : R.drawable.ic_super_g_color)
                : null;
        mPlainIcons.end.clear();
        if (mGeminiInstalled) {
            mPlainIcons.end.add(load(mThemed
                    ? R.drawable.ic_gemini_themed : R.drawable.ic_gemini_color));
        }
        mPlainIcons.end.add(load(mMusicSearch
                ? (mThemed ? R.drawable.ic_music_themed : R.drawable.ic_music_color)
                : (mThemed ? R.drawable.ic_mic_themed : R.drawable.ic_mic_color)));
        if (mGsaEnabled) {
            mPlainIcons.end.add(load(mThemed
                    ? R.drawable.ic_lens_themed : R.drawable.ic_lens_color));
        }

        mGoogleIcons.start = load(mThemed
                ? R.drawable.ic_super_g_themed : R.drawable.ic_super_g_color);
        mGoogleIcons.end.clear();
        mGoogleIcons.end.add(load(mThemed
                ? (mMusicSearch ? R.drawable.ic_music_themed : R.drawable.ic_mic_themed_google)
                : (mMusicSearch ? R.drawable.ic_music_color : R.drawable.ic_mic_color_google)));
        if (mGsaEnabled) {
            mGoogleIcons.end.add(load(mThemed
                    ? R.drawable.ic_lens_themed_google : R.drawable.ic_lens_color_google));
        }
        mGoogleIcons.action = load(mThemed
                ? R.drawable.ic_ai_mode_themed_google : R.drawable.ic_ai_mode_color_google);
        mGoogleIcons.actionBackground = load(R.drawable.bg_ai_mode_button_google);
    }

    @Nullable
    private Drawable load(int res) {
        Drawable drawable = mLauncherContext.getDrawable(res);
        return drawable != null ? drawable.mutate() : null;
    }

    private static int live(Item item, int fallback) {
        return LivePreviewValues.get(item.getSharedPrefKey(), fallback);
    }

    private void loadApps() {
        Context context = getContext().getApplicationContext();
        int iconDpi = getResources().getDisplayMetrics().densityDpi;
        int maxApps = mColumns * 2 + mHotseatCount;
        UI_HELPER_EXECUTOR.execute(() -> {
            boolean gsa = Utilities.isGSAEnabled(context);
            boolean gemini = Utilities.isPackageInstalled(context, Utilities.GEMINI_PACKAGE);
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
                if (icons.size() >= maxApps) {
                    break;
                }
                Drawable icon;
                try {
                    icon = IconPackIcons.getIcon(context, info.getActivityInfo(), iconDpi,
                            () -> info.getIcon(iconDpi));
                } catch (RuntimeException e) {
                    icon = null;
                }
                try {
                    icons.add(icon != null ? icon : info.getIcon(iconDpi));
                    labels.add(info.getLabel());
                } catch (RuntimeException ignored) {
                }
            }
            post(() -> {
                mGsaEnabled = gsa;
                mGeminiInstalled = gemini;
                mIcons.clear();
                mIcons.addAll(icons);
                mLabels.clear();
                mLabels.addAll(labels);
                mLoaded = true;
                readSettings(false);
            });
        });
    }

    private void loadWallpaper() {
        int width = Math.round(mPhone.width());
        int height = Math.round(mPhone.height());
        if (width <= 0 || height <= 0) {
            return;
        }
        Context context = getContext().getApplicationContext();
        UI_HELPER_EXECUTOR.execute(() -> {
            Bitmap result = null;
            WallpaperManager wm = WallpaperManager.getInstance(context);
            try {
                WallpaperInfo info = wm.getWallpaperInfo();
                Drawable wallpaper = info != null
                        ? info.loadThumbnail(context.getPackageManager())
                        : wm.getDrawable(WallpaperManager.FLAG_SYSTEM);
                if (wallpaper != null) {
                    result = centerCrop(wallpaper, width, height);
                }
            } catch (RuntimeException ignored) {
            } finally {
                try {
                    wm.forgetLoadedWallpaper();
                } catch (RuntimeException ignored) {
                }
            }
            Bitmap bitmap = result;
            post(() -> {
                mWallpaper = bitmap;
                invalidate();
            });
        });
    }

    private static Bitmap centerCrop(Drawable drawable, int width, int height) {
        int dw = Math.max(1, drawable.getIntrinsicWidth());
        int dh = Math.max(1, drawable.getIntrinsicHeight());
        float scale = Math.max(width / (float) dw, height / (float) dh);
        int sw = Math.round(dw * scale);
        int sh = Math.round(dh * scale);
        int left = (width - sw) / 2;
        int top = (height - sh) / 2;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(left, top, left + sw, top + sh);
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        mRect.set(0, 0, w, h);
        canvas.drawRoundRect(mRect, mRadius, mRadius, mCardPaint);
        if (mPhone.isEmpty()) {
            return;
        }

        float bump = 1f + 0.015f * mBump.get();
        canvas.save();
        canvas.scale(bump, bump, mPhone.centerX(), mPhone.centerY());
        canvas.save();
        canvas.clipPath(mPhoneClip);
        if (mWallpaper != null) {
            mPhone.round(mBitmapDst);
            canvas.drawBitmap(mWallpaper, null, mBitmapDst, mWallpaperPaint);
        } else {
            canvas.drawRect(mPhone, mGradientPaint);
        }

        canvas.translate(mPhone.left, mPhone.top);
        canvas.scale(mPhoneScale, mPhoneScale);
        float sw = mScreenWidth;
        float sh = mScreenHeight;

        float shadow = clamp(mTopShadow.get(), 0f, 1f);
        if (shadow > 0.01f) {
            mShadowPaint.setAlpha(Math.round(255 * shadow));
            canvas.drawRect(0, 0, sw, dp(140), mShadowPaint);
        }

        float statusHeight = statusBarHeight();
        drawStatusBar(canvas, sw, statusHeight);

        float navHeight = dp(24);
        float iconSize = dp(52) * mIconScale.get();
        float search = mSearch.get();
        float searchHeight = dp(66);
        float searchSide = dp(22);
        float searchBottom = sh - navHeight - dp(10);
        float searchTop = searchBottom - searchHeight;
        float hotseatHeight = iconSize + dp(40);
        float hotseatBottom = (sh - navHeight - dp(16)) * (1f - search)
                + (searchTop - dp(2)) * search;
        float hotseatTop = hotseatBottom - hotseatHeight;

        drawDockBackground(canvas, sw, hotseatTop,
                Math.max(hotseatBottom, searchBottom * clamp(search, 0f, 1f)));
        drawGlance(canvas, statusHeight + dp(18));
        drawGrid(canvas, sw, statusHeight + dp(8), hotseatTop - dp(12), iconSize);
        drawHotseat(canvas, sw, hotseatTop, hotseatHeight, iconSize);
        if (search > 0.01f) {
            canvas.save();
            canvas.translate(0, dp(40) * (1f - search));
            drawSearch(canvas, searchSide, searchTop, sw - searchSide, searchHeight,
                    clamp(search, 0f, 1f));
            canvas.restore();
        }
        canvas.restore();
        canvas.drawPath(mPhoneClip, mFramePaint);
        canvas.restore();
    }

    private float statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(28);
    }

    private void drawStatusBar(Canvas canvas, float sw, float height) {
        float progress = clamp(mStatusBar.get(), 0f, 1f);
        if (progress < 0.01f) {
            return;
        }
        int alpha = Math.round(255 * progress);
        float cy = height / 2f + dp(2);
        mTextPaint.setTypeface(mMedium);
        mTextPaint.setTextSize(sp(15));
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setColor(mWorkspaceText);
        mTextPaint.setAlpha(alpha);
        canvas.drawText(mTimeText, dp(26),
                cy - (mTextPaint.descent() + mTextPaint.ascent()) / 2f, mTextPaint);
        mFillPaint.setColor(mWorkspaceText);
        mFillPaint.setAlpha(alpha);
        float right = sw - dp(26);
        mRect.set(right - dp(24), cy - dp(6), right, cy + dp(6));
        canvas.drawRoundRect(mRect, dp(3), dp(3), mFillPaint);
        canvas.drawCircle(right - dp(38), cy, dp(6), mFillPaint);
        canvas.drawCircle(right - dp(58), cy, dp(6), mFillPaint);
    }

    private void drawGlance(Canvas canvas, float top) {
        float progress = clamp(mGlance.get(), 0f, 1f);
        if (progress < 0.01f) {
            return;
        }
        int alpha = Math.round(255 * progress);
        float left = dp(28);
        canvas.save();
        canvas.translate(0, -dp(24) * (1f - progress));
        mTextPaint.setTextAlign(Paint.Align.LEFT);
        mTextPaint.setShadowLayer(dp(2), 0, dp(1), mWorkspaceShadow);
        mTextPaint.setColor(mWorkspaceText);
        mTextPaint.setAlpha(alpha);
        if (STYLE_CLOCK.equals(mGlanceStyle)) {
            drawMistifyClock(canvas, top, alpha);
        } else if (STYLE_EXTENDED.equals(mGlanceStyle)) {
            mTextPaint.setTypeface(mRegular);
            mTextPaint.setTextSize(sp(34));
            float baseline = top + sp(40);
            canvas.drawText(mDateText, left, baseline, mTextPaint);
            mTextPaint.setTextSize(sp(16));
            mTextPaint.setAlpha(Math.round(alpha * 0.8f));
            canvas.drawText("24°", left + dp(30), baseline + sp(28), mTextPaint);
            drawWeatherDot(canvas, left + dp(11), baseline + sp(22), alpha);
        } else {
            mTextPaint.setTypeface(mMedium);
            mTextPaint.setTextSize(sp(24));
            float baseline = top + sp(30);
            canvas.drawText(mDateText, left, baseline, mTextPaint);
            float end = left + mTextPaint.measureText(mDateText) + dp(16);
            drawWeatherDot(canvas, end + dp(10), baseline - sp(8), alpha);
            mTextPaint.setTextSize(sp(20));
            mTextPaint.setAlpha(alpha);
            canvas.drawText("24°", end + dp(26), baseline, mTextPaint);
        }
        mTextPaint.clearShadowLayer();
        canvas.restore();
    }

    private void drawMistifyClock(Canvas canvas, float top, int alpha) {
        float textLeft = dp(16);
        mTextPaint.setTypeface(mClockMedium);
        mTextPaint.setTextSize(sp(28));
        mTextPaint.setColor(mWorkspaceText);
        mTextPaint.setAlpha(alpha);
        float y = top - mTextPaint.ascent();
        canvas.drawText(mDayText, textLeft, y, mTextPaint);
        y += mTextPaint.descent();

        mTextPaint.clearShadowLayer();
        mTextPaint.setTypeface(mClockBold);
        mTextPaint.setTextSize(sp(80));
        Paint.FontMetrics metrics = mTextPaint.getFontMetrics();
        float clockBaseline = y - metrics.top;
        float clockLeft = dp(10);
        mTextPaint.setColor(mClockHourColor);
        mTextPaint.setAlpha(alpha);
        canvas.drawText(mHourText, clockLeft, clockBaseline, mTextPaint);
        float minuteLeft = clockLeft + mTextPaint.measureText(mHourText);
        mTextPaint.setColor(mClockMinuteColor);
        mTextPaint.setAlpha(alpha);
        canvas.drawText(mMinuteText, minuteLeft, clockBaseline, mTextPaint);
        y = clockBaseline + metrics.bottom;

        mTextPaint.setShadowLayer(dp(2), 0, dp(1), mWorkspaceShadow);
        mTextPaint.setTypeface(mClockRegular);
        mTextPaint.setTextSize(sp(16));
        mTextPaint.setColor(mWorkspaceText);
        mTextPaint.setAlpha(alpha);
        float dateBaseline = y - mTextPaint.ascent();
        canvas.drawText(mShortDateText, textLeft, dateBaseline, mTextPaint);
        float weatherLeft = textLeft + mTextPaint.measureText(mShortDateText) + dp(10);
        drawWeatherDot(canvas, weatherLeft + dp(9), dateBaseline - sp(5), alpha);
        mTextPaint.setColor(mWorkspaceText);
        mTextPaint.setAlpha(alpha);
        canvas.drawText("24°", weatherLeft + dp(24), dateBaseline, mTextPaint);
    }

    private void drawWeatherDot(Canvas canvas, float cx, float cy, int alpha) {
        mFillPaint.setColor(0xFFFFC107);
        mFillPaint.setAlpha(alpha);
        canvas.drawCircle(cx, cy, dp(9), mFillPaint);
    }

    private void drawGrid(Canvas canvas, float sw, float top, float bottom, float iconSize) {
        if (bottom <= top) {
            return;
        }
        float cellWidth = (sw - dp(16) * 2) / mColumns;
        float cellHeight = (bottom - top) / mRows;
        float labels = clamp(mLabelsShown.get(), 0f, 1f);
        float fontScale = mFontScale.get();
        mLabelPaint.setTextSize(sp(13) * fontScale);
        mLabelPaint.setTypeface(mRegular);
        mLabelPaint.setShadowLayer(dp(2), 0, dp(1), mWorkspaceShadow);
        float labelHeight = sp(18) * fontScale * labels;
        int count = Math.min(mColumns * 2, mIcons.size());
        for (int i = 0; i < count; i++) {
            int row = mRows - 2 + i / mColumns;
            int col = i % mColumns;
            float cx = dp(16) + cellWidth * col + cellWidth / 2f;
            float cellTop = top + cellHeight * row;
            float iconTop = cellTop + (cellHeight - iconSize - labelHeight) / 2f;
            drawIcon(canvas, mIcons.get(i), cx, iconTop, iconSize);
            if (i == 0 && mShowDots) {
                float dot = dp(8);
                mFillPaint.setColor(mAccent);
                mFillPaint.setAlpha(255);
                canvas.drawCircle(cx + iconSize / 2f - dot * 0.6f, iconTop + dot * 0.6f, dot,
                        mFillPaint);
            }
            if (labels > 0.01f && i < mLabels.size()) {
                mLabelPaint.setColor(mWorkspaceText);
                mLabelPaint.setAlpha(Math.round(255 * labels));
                CharSequence label = TextUtils.ellipsize(mLabels.get(i), mLabelPaint,
                        cellWidth - dp(8), TextUtils.TruncateAt.END);
                canvas.drawText(label, 0, label.length(), cx,
                        iconTop + iconSize + sp(16) * fontScale, mLabelPaint);
            }
        }
        mLabelPaint.clearShadowLayer();
    }

    private void drawDockBackground(Canvas canvas, float sw, float top, float bottom) {
        float background = clamp(mDockBackground.get(), 0f, 1f);
        if (background < 0.01f || mAppseat == null) {
            return;
        }
        mAppseat.setAlpha(Math.round(255 * background));
        mAppseat.setBounds(Math.round(dp(10)), Math.round(top - dp(6)),
                Math.round(sw - dp(10)), Math.round(bottom + dp(6)));
        mAppseat.draw(canvas);
    }

    private void drawHotseat(Canvas canvas, float sw, float top, float height, float iconSize) {
        int offset = mColumns * 2;
        float cellWidth = (sw - dp(16) * 2) / mHotseatCount;
        float iconTop = top + (height - iconSize) / 2f;
        for (int col = 0; col < mHotseatCount; col++) {
            int index = offset + col;
            Drawable icon = index < mIcons.size() ? mIcons.get(index)
                    : (col < mIcons.size() ? mIcons.get(col) : null);
            if (icon == null) {
                continue;
            }
            float cx = dp(16) + cellWidth * col + cellWidth / 2f;
            drawIcon(canvas, icon, cx, iconTop, iconSize);
        }
    }

    private void drawSearch(Canvas canvas, float left, float top, float right, float height,
            float visible) {
        if (mGoogleStyle) {
            drawGoogleSearch(canvas, left, top, right, height, visible);
        } else {
            drawPlainSearch(canvas, left, top, right, height, visible);
        }
    }

    private void drawPlainSearch(Canvas canvas, float left, float top, float right, float height,
            float visible) {
        float padding = dp(6);
        mRect.set(left, top + padding, right, top + height - padding);
        float corner = (mRect.height() / 2f) * clamp(mSearchRadius.get(), 0f, 1f);
        int base = mThemed ? mQsbFillThemed : mQsbFill;
        mFillPaint.setColor(base);
        mFillPaint.setAlpha(Math.round(255 * visible * clamp(mSearchAlpha.get(), 0f, 1f)));
        canvas.drawRoundRect(mRect, corner, corner, mFillPaint);
        float stroke = mSearchStroke.get();
        if (stroke > 0.05f) {
            mStrokePaint.setColor(mAccent);
            mStrokePaint.setAlpha(Math.round(255 * visible));
            mStrokePaint.setStrokeWidth(stroke);
            canvas.drawRoundRect(mRect, corner, corner, mStrokePaint);
        }
        int alpha = Math.round(255 * visible);
        float iconPadding = dp(10);
        float cy = mRect.centerY();
        if (mPlainIcons.start != null) {
            drawIntrinsic(canvas, mPlainIcons.start, mRect.left + dp(12) + iconPadding, cy,
                    alpha);
        }
        float x = mRect.right - dp(12);
        for (int i = mPlainIcons.end.size() - 1; i >= 0; i--) {
            Drawable icon = mPlainIcons.end.get(i);
            if (icon == null) {
                continue;
            }
            x -= iconPadding + icon.getIntrinsicWidth();
            drawIntrinsic(canvas, icon, x, cy, alpha);
            x -= iconPadding;
        }
    }

    private void drawGoogleSearch(Canvas canvas, float left, float top, float right,
            float height, float visible) {
        float bgAlpha = clamp(mGoogleAlpha.get(), 0f, 1f) * visible;
        float radius = height;
        mRect.set(left, top, right, top + height);
        mFillPaint.setColor(mQsbOuter);
        mFillPaint.setAlpha(Math.round(255 * bgAlpha));
        canvas.drawRoundRect(mRect, radius, radius, mFillPaint);
        float padding = height / 8f;
        float actionSize = dp(44);
        float actionMargin = dp(18);
        float marginEnd = dp(8);
        if (bgAlpha > 0.003f) {
            float rightPadding = actionSize + actionMargin + marginEnd - padding;
            mRect.set(left + padding, top + padding, right - rightPadding,
                    top + height - padding);
            mFillPaint.setColor(mQsbFill);
            mFillPaint.setAlpha(Math.round(255 * bgAlpha));
            canvas.drawRoundRect(mRect, radius, radius, mFillPaint);
        }

        int alpha = Math.round(255 * visible);
        float cy = top + height / 2f;
        if (mGoogleIcons.start != null) {
            drawIntrinsic(canvas, mGoogleIcons.start, left + dp(20), cy, alpha);
        }
        float tap = dp(48);
        float iconPadding = dp(10);
        float x = right - marginEnd;
        float actionLeft = x - tap;
        if (mGoogleIcons.actionBackground != null) {
            mGoogleIcons.actionBackground.setAlpha(alpha);
            mGoogleIcons.actionBackground.setBounds(Math.round(actionLeft),
                    Math.round(cy - tap / 2f), Math.round(x), Math.round(cy + tap / 2f));
            mGoogleIcons.actionBackground.draw(canvas);
        }
        if (mGoogleIcons.action != null) {
            drawFit(canvas, mGoogleIcons.action, actionLeft + iconPadding, cy,
                    tap - iconPadding * 2, alpha);
        }
        x = actionLeft - actionMargin;
        for (int i = mGoogleIcons.end.size() - 1; i >= 0; i--) {
            Drawable icon = mGoogleIcons.end.get(i);
            x -= tap;
            if (icon != null) {
                drawFit(canvas, icon, x + iconPadding, cy, tap - iconPadding * 2, alpha);
            }
        }
    }

    private static void drawIntrinsic(Canvas canvas, Drawable icon, float left, float cy,
            int alpha) {
        int w = icon.getIntrinsicWidth();
        int h = icon.getIntrinsicHeight();
        int top = Math.round(cy - h / 2f);
        icon.setAlpha(alpha);
        icon.setBounds(Math.round(left), top, Math.round(left) + w, top + h);
        icon.draw(canvas);
    }

    private static void drawFit(Canvas canvas, Drawable icon, float left, float cy, float size,
            int alpha) {
        int w = Math.max(1, icon.getIntrinsicWidth());
        int h = Math.max(1, icon.getIntrinsicHeight());
        float scale = Math.min(size / w, size / h);
        float dw = w * scale;
        float dh = h * scale;
        float l = left + (size - dw) / 2f;
        icon.setAlpha(alpha);
        icon.setBounds(Math.round(l), Math.round(cy - dh / 2f), Math.round(l + dw),
                Math.round(cy + dh / 2f));
        icon.draw(canvas);
    }

    private static void drawIcon(Canvas canvas, Drawable icon, float cx, float top, float size) {
        icon.setBounds(Math.round(cx - size / 2f), Math.round(top),
                Math.round(cx + size / 2f), Math.round(top + size));
        icon.draw(canvas);
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

    private static final class SearchIcons {
        @Nullable
        Drawable start;
        final List<Drawable> end = new ArrayList<>();
        @Nullable
        Drawable action;
        @Nullable
        Drawable actionBackground;
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
