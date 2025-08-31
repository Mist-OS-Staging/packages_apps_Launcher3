package com.android.launcher3.qsb;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.PaintDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.core.view.ViewCompat;
import com.android.launcher3.Reorderable;
import com.android.launcher3.BaseActivity;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.qsb.QsbContainerView;
import com.android.launcher3.util.Themes;
import com.android.launcher3.views.ActivityContext;
import android.view.View;

public class QsbLayout extends FrameLayout implements Reorderable,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private ImageView micIcon;
    private ImageView gIcon;
    private ImageView lensIcon;
    private Context mContext;
    private ImageView mAiModeButton;
    private FrameLayout inner;

    public QsbLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public QsbLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        micIcon = findViewById(R.id.mic_icon);
        gIcon = findViewById(R.id.g_icon);
        lensIcon = findViewById(R.id.lens_icon);
        mAiModeButton = findViewById(R.id.ai_mode_button);

        setUpMainSearch();
        setUpBackground();
        clipIconRipples();

        boolean isThemed = LauncherPrefs.DOCK_THEME.get(mContext);

        if (Utilities.isMusicSearchEnabled(mContext)) {
            micIcon.setImageResource(isThemed ? R.drawable.ic_music_themed : R.drawable.ic_music_color);
        } else {
            micIcon.setImageResource(isThemed ? R.drawable.ic_mic_themed : R.drawable.ic_mic_color);
        }
        gIcon.setImageResource(isThemed ? R.drawable.ic_super_g_themed : R.drawable.ic_super_g_color);
        lensIcon.setImageResource(isThemed ? R.drawable.ic_lens_themed : R.drawable.ic_lens_color);
        mAiModeButton.setImageResource(R.drawable.ic_ai_mode_color);

        setupGIcon();
        setupLensIcon();

        // Set the custom background drawable
        post(() -> {
            View parent = (View) getParent();
            if (parent != null) {
                QsbOuterDrawable customDrawable = new QsbOuterDrawable(mContext);
                parent.setBackground(customDrawable);
            }
        });
    }

    private void clipIconRipples() {
        float cornerRadius = getCornerRadius();
        PaintDrawable pd = new PaintDrawable(Color.TRANSPARENT);
        pd.setCornerRadius(cornerRadius);
        micIcon.setClipToOutline(cornerRadius > 0);
        micIcon.setBackground(pd);
        lensIcon.setClipToOutline(cornerRadius > 0);
        lensIcon.setBackground(pd);
        gIcon.setClipToOutline(cornerRadius > 0);
        gIcon.setBackground(pd);
    }

    private void setUpBackground() {
        float cornerRadius = getCornerRadius();
        int alphaValue = (LauncherPrefs.HOTSEAT_QSB_OPACITY.get(mContext) * 255) / 100;
        int baseColor = Themes.getAttrColor(mContext, R.attr.qsbFillColor);
        if (LauncherPrefs.DOCK_THEME.get(mContext))
            baseColor = Themes.getAttrColor(mContext, R.attr.qsbFillColorThemed);
        int color = Color.argb(alphaValue, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor));
        float strokeWidth = LauncherPrefs.HOTSEAT_QSB_STROKE_WIDTH.get(mContext);

        PaintDrawable backgroundDrawable = new PaintDrawable(color);
        backgroundDrawable.setCornerRadius(cornerRadius);

        if (strokeWidth != 0f) {
            PaintDrawable strokeDrawable = new PaintDrawable(Themes.getColorAccent(mContext));
            strokeDrawable.getPaint().setStyle(Paint.Style.STROKE);
            strokeDrawable.getPaint().setStrokeWidth(strokeWidth);
            strokeDrawable.setCornerRadius(cornerRadius);
            LayerDrawable combinedDrawable = new LayerDrawable(new Drawable[]{backgroundDrawable, strokeDrawable});

        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int requestedWidth = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        DeviceProfile dp = ActivityContext.lookupContext(mContext).getDeviceProfile();
        int cellWidth = DeviceProfile.calculateCellWidth(requestedWidth, dp.cellLayoutBorderSpacePx.x, dp.numShownHotseatIcons);
        int iconSize = (int)(Math.round((dp.iconSizePx * 0.92f)));
        int width = requestedWidth;
        setMeasuredDimension(width, height);

        for (int i = 0; i < getChildCount(); i++) {
            final View child = getChildAt(i);
            if (child != null) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
          if (key.equals(LauncherPrefs.QSB_OUTER_OPACITY.getKey())) {
            // Update the drawable if it's already set
            View parent = (View) getParent();
            if (parent != null && parent.getBackground() instanceof QsbOuterDrawable) {
                ((QsbOuterDrawable) parent.getBackground()).updateOpacity();
            }
        }
    }

    private void setUpMainSearch() {
        String searchPackage = QsbContainerView.getSearchWidgetPackageName(mContext);
        setOnClickListener(view -> {
            mContext.startActivity(new Intent("android.search.action.GLOBAL_SEARCH").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TASK).setPackage(searchPackage));
        });
    }

    private void setupGIcon() {
        try {
            Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(Utilities.GSA_PACKAGE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            gIcon.setOnClickListener(view -> {
                mContext.startActivity(intent);
            });
        } catch (Exception e) {
            // Do nothing
        }
    }

    private void setupLensIcon() {
        try {
            lensIcon.setOnClickListener(view -> {
                Intent lensIntent = new Intent();
                lensIntent.setAction(Intent.ACTION_VIEW)
                        .setComponent(new ComponentName(Utilities.GSA_PACKAGE, Utilities.LENS_ACTIVITY))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .setData(Uri.parse(Utilities.LENS_URI))
                        .putExtra("LensHomescreenShortcut", true);
                mContext.startActivity(lensIntent);
            });
        } catch (Exception e) {
            lensIcon.setVisibility(View.GONE);
        }
    }

    private float getCornerRadius() {
        Resources res = mContext.getResources();
        float qsbWidgetHeight = res.getDimension(R.dimen.qsb_widget_height);
        float qsbWidgetPadding = res.getDimension(R.dimen.qsb_widget_vertical_padding);
        float innerHeight = qsbWidgetHeight - 2 * qsbWidgetPadding;
        return (innerHeight / 2) * ((float)LauncherPrefs.SEARCH_RADIUS_SIZE.get(mContext) / 100f);
    }
}
