/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings.expressive;

import android.graphics.Color;
import android.graphics.Insets;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.OvershootInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.R;

public abstract class ExpressiveSettingsActivity extends FragmentActivity {

    public static final int CONTENT_ID = R.id.settings_page_content;

    private static final int HEADER_HEIGHT_DP = 248;
    private static final int BAR_HEIGHT_DP = 64;

    private View mHeader;
    private ExpressiveShapeView mHeaderShape;
    private TextView mHeaderTitle;
    private View mBar;
    private View mBarBackground;
    private TextView mBarTitle;
    @Nullable
    private RecyclerView mList;
    private int mHeaderHeight;
    private int mBarHeight;
    private boolean mBarShown;

    protected abstract SettingsCategory getSettingsCategory();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_expressive_page);

        SettingsCategory category = getSettingsCategory();
        int container = getColor(category.containerColorRes);
        int onContainer = getColor(category.onContainerColorRes);

        View root = findViewById(R.id.settings_page_root);
        mHeader = findViewById(R.id.settings_page_header);
        mHeaderShape = findViewById(R.id.settings_page_header_shape);
        mHeaderTitle = findViewById(R.id.settings_page_header_title);
        mBar = findViewById(R.id.settings_page_bar);
        mBarBackground = findViewById(R.id.settings_page_bar_background);
        mBarTitle = findViewById(R.id.settings_page_bar_title);

        mHeader.setBackgroundColor(container);
        mHeaderShape.setShape(category.shape);
        mHeaderShape.setColor(onContainer);
        mHeaderTitle.setTextColor(onContainer);
        mHeaderTitle.setText(getTitle());
        mBarTitle.setText(getTitle());
        findViewById(R.id.settings_page_back).setOnClickListener(
                v -> getOnBackPressedDispatcher().onBackPressed());

        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            boolean lightHeader = Color.luminance(container) > 0.5f;
            controller.setSystemBarsAppearance(
                    lightHeader ? WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS : 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
        }

        mHeaderHeight = dp(HEADER_HEIGHT_DP);
        mBarHeight = dp(BAR_HEIGHT_DP);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                    | WindowInsets.Type.displayCutout());
            mHeaderHeight = dp(HEADER_HEIGHT_DP) + bars.top;
            mBarHeight = dp(BAR_HEIGHT_DP) + bars.top;
            setHeight(mHeader, mHeaderHeight);
            setHeight(mBar, mBarHeight);
            applyListPadding();
            return insets;
        });

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(@NonNull FragmentManager fm,
                            @NonNull Fragment f, @NonNull View v,
                            @Nullable Bundle state) {
                        if (f instanceof PreferenceFragmentCompat preferenceFragment
                                && preferenceFragment.getListView() != null) {
                            attachList(preferenceFragment.getListView(), state == null);
                        }
                    }
                }, false);

        if (savedInstanceState == null) {
            mHeaderShape.setRotation(-90f);
            mHeaderShape.setScaleX(0.4f);
            mHeaderShape.setScaleY(0.4f);
            ExpressiveMotion.springTo(mHeaderShape, DynamicAnimation.ROTATION, 0f, 200f, 0.6f);
            ExpressiveMotion.springTo(mHeaderShape, DynamicAnimation.SCALE_X, 1f, 260f, 0.55f);
            ExpressiveMotion.springTo(mHeaderShape, DynamicAnimation.SCALE_Y, 1f, 260f, 0.55f);
            ExpressiveMotion.enter(mHeaderTitle, dp(24), 60);
        }
    }

    @Override
    protected void onTitleChanged(CharSequence title, int color) {
        super.onTitleChanged(title, color);
        if (mHeaderTitle != null) {
            mHeaderTitle.setText(title);
            mBarTitle.setText(title);
        }
    }

    private void attachList(RecyclerView list, boolean animate) {
        mList = list;
        list.setClipToPadding(false);
        applyListPadding();
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                onListScrolled(recyclerView.computeVerticalScrollOffset());
            }
        });
        if (animate) {
            AnimationSet set = new AnimationSet(true);
            set.addAnimation(new TranslateAnimation(0, 0, dp(40), 0));
            set.addAnimation(new AlphaAnimation(0f, 1f));
            set.setDuration(420);
            set.setInterpolator(new OvershootInterpolator(1.1f));
            LayoutAnimationController controller = new LayoutAnimationController(set, 0.08f);
            list.setLayoutAnimation(controller);
        }
    }

    private void applyListPadding() {
        if (mList != null) {
            mList.setPadding(mList.getPaddingLeft(), mHeaderHeight, mList.getPaddingRight(),
                    mList.getPaddingBottom());
        }
    }

    private void onListScrolled(int offset) {
        mHeader.setTranslationY(-offset * 0.5f);
        mHeaderShape.setRotation(offset * 0.25f);
        float scale = 1f + offset / 1500f;
        mHeaderShape.setScaleX(scale);
        mHeaderShape.setScaleY(scale);
        float titleProgress = Math.min(1f, offset / (mHeaderHeight * 0.55f));
        mHeaderTitle.setAlpha(1f - titleProgress);
        float titleScale = 1f - 0.15f * titleProgress;
        mHeaderTitle.setPivotX(0);
        mHeaderTitle.setPivotY(mHeaderTitle.getHeight());
        mHeaderTitle.setScaleX(titleScale);
        mHeaderTitle.setScaleY(titleScale);
        setBarShown(offset > mHeaderHeight - mBarHeight - dp(16));
    }

    private void setBarShown(boolean shown) {
        if (shown == mBarShown) {
            return;
        }
        mBarShown = shown;
        mBarBackground.animate().alpha(shown ? 1f : 0f).setDuration(200).start();
        mBarTitle.animate().alpha(shown ? 1f : 0f).setDuration(200).start();
        if (shown) {
            mBarTitle.setTranslationY(dp(10));
            ExpressiveMotion.springTo(mBarTitle, DynamicAnimation.TRANSLATION_Y, 0f,
                    ExpressiveMotion.SPATIAL_STIFFNESS, ExpressiveMotion.SPATIAL_DAMPING);
        }
    }

    private static void setHeight(View view, int height) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp.height != height) {
            lp.height = height;
            view.setLayoutParams(lp);
        }
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }
}
