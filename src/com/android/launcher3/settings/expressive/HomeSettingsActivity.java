/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings.expressive;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Insets;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.dynamicanimation.animation.DynamicAnimation;

import com.android.launcher3.R;
import com.android.launcher3.settings.SettingsHomescreen;

public class HomeSettingsActivity extends Activity {

    private static final int[] TILE_IDS = {
            R.id.settings_tile_icons,
            R.id.settings_tile_home,
            R.id.settings_tile_app_drawer,
            R.id.settings_tile_recents,
            R.id.settings_tile_misc,
    };
    private static final SettingsCategory[] TILE_CATEGORIES = {
            SettingsCategory.ICONS,
            SettingsCategory.HOME,
            SettingsCategory.APP_DRAWER,
            SettingsCategory.RECENTS,
            SettingsCategory.MISC,
    };

    private HomePreviewView mPreview;
    private View mBar;
    private View mTitle;
    private boolean mBarShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_expressive_home);

        View root = findViewById(R.id.settings_home_root);
        ScrollView scroll = findViewById(R.id.settings_home_scroll);
        View content = findViewById(R.id.settings_home_content);
        mBar = findViewById(R.id.settings_home_bar);
        mTitle = findViewById(R.id.settings_home_title);
        mPreview = findViewById(R.id.settings_home_preview);

        int titleTop = mTitle.getPaddingTop();
        int contentBottom = content.getPaddingBottom();
        int barHeight = dp(64);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout());
            mTitle.setPadding(mTitle.getPaddingLeft(), titleTop + bars.top,
                    mTitle.getPaddingRight(), mTitle.getPaddingBottom());
            content.setPadding(bars.left, content.getPaddingTop(), bars.right,
                    contentBottom + bars.bottom);
            ViewGroup.LayoutParams lp = mBar.getLayoutParams();
            lp.height = barHeight + bars.top;
            mBar.setLayoutParams(lp);
            return insets;
        });

        scroll.setOnScrollChangeListener((v, x, y, oldX, oldY) ->
                setBarShown(y > mTitle.getHeight() - mBar.getHeight()));

        mPreview.setOnClickListener(v -> openCategory(v, SettingsHomescreen.class));
        ExpressiveMotion.attachPress(mPreview, null, 0, 0, 0.97f, null, 0);

        View search = findViewById(R.id.settings_home_search);
        search.setOnClickListener(v -> startActivity(
                new Intent(this, SettingsSearchActivity.class)));
        ExpressiveMotion.attachPress(search, null, 0, 0, 0.97f, null, 0);

        for (int i = 0; i < TILE_IDS.length; i++) {
            bindTile(findViewById(TILE_IDS[i]), TILE_CATEGORIES[i], i == 0);
        }

        if (savedInstanceState == null) {
            ViewGroup group = (ViewGroup) content;
            for (int i = 0; i < group.getChildCount(); i++) {
                ExpressiveMotion.enter(group.getChildAt(i), dp(48), 40L * i);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPreview.refresh();
    }

    private void bindTile(View tile, SettingsCategory category, boolean tall) {
        int container = getColor(category.containerColorRes);
        int onContainer = getColor(category.onContainerColorRes);
        float radius = dp(28);
        GradientDrawable background = new GradientDrawable();
        background.setColor(container);
        background.setCornerRadius(radius);
        tile.setBackground(background);

        View shapeFrame = tile.findViewById(R.id.tile_shape_frame);
        if (tall) {
            ViewGroup.LayoutParams lp = shapeFrame.getLayoutParams();
            lp.width = dp(76);
            lp.height = dp(76);
            shapeFrame.setLayoutParams(lp);
            View glyph = tile.findViewById(R.id.tile_glyph);
            ViewGroup.LayoutParams glp = glyph.getLayoutParams();
            glp.width = dp(36);
            glp.height = dp(36);
            glyph.setLayoutParams(glp);
        }
        ExpressiveShapeView shape = tile.findViewById(R.id.tile_shape);
        shape.setShape(category.shape);
        shape.setColor(onContainer);
        ImageView glyph = tile.findViewById(R.id.tile_glyph);
        glyph.setImageResource(category.glyphRes);
        glyph.setImageTintList(ColorStateList.valueOf(container));
        TextView title = tile.findViewById(R.id.tile_title);
        title.setText(category.titleRes);
        title.setTextColor(onContainer);
        TextView summary = tile.findViewById(R.id.tile_summary);
        summary.setText(category.summaryRes);
        summary.setTextColor(onContainer);
        tile.setContentDescription(getString(category.titleRes));

        ExpressiveMotion.attachPress(tile, background, radius, dp(44), 0.95f, shapeFrame, 40f);
        tile.setOnClickListener(v -> openCategory(v, category.activity));
    }

    private void openCategory(View source, Class<? extends Activity> target) {
        ActivityOptions options = ActivityOptions.makeScaleUpAnimation(source, 0, 0,
                source.getWidth(), source.getHeight());
        startActivity(new Intent(this, target), options.toBundle());
    }

    private void setBarShown(boolean shown) {
        if (shown == mBarShown) {
            return;
        }
        mBarShown = shown;
        mBar.animate().alpha(shown ? 1f : 0f).setDuration(200).start();
        View barTitle = findViewById(R.id.settings_home_bar_title);
        barTitle.setTranslationY(shown ? dp(10) : 0);
        if (shown) {
            ExpressiveMotion.springTo(barTitle, DynamicAnimation.TRANSLATION_Y, 0f,
                    ExpressiveMotion.SPATIAL_STIFFNESS, ExpressiveMotion.SPATIAL_DAMPING);
        }
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }
}
