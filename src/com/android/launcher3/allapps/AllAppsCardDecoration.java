/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.allapps;

import static com.android.launcher3.LauncherPrefs.DRAWER_CARD_STYLE;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.util.Themes;

/** Draws the app list on a rounded card, matching the pinned apps card. */
public class AllAppsCardDecoration extends RecyclerView.ItemDecoration {

    private static final int CARD_RADIUS_DP = 28;

    private final Context mContext;
    private final Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF mRect = new RectF();
    private final float mRadius;

    public AllAppsCardDecoration(Context context) {
        mContext = context;
        mPaint.setColor(Themes.getAttrColor(context, R.attr.allappsHeaderProtectionColor));
        mRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, CARD_RADIUS_DP,
                context.getResources().getDisplayMetrics());
    }

    public static boolean isEnabled(Context context) {
        return LauncherPrefs.get(context).get(DRAWER_CARD_STYLE);
    }

    @Override
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {
        int childCount = parent.getChildCount();
        if (childCount == 0 || !isEnabled(mContext)) {
            return;
        }
        RecyclerView.Adapter<?> adapter = parent.getAdapter();
        int itemCount = adapter == null ? 0 : adapter.getItemCount();
        if (itemCount == 0) {
            return;
        }

        float top = -mRadius;
        float bottom = parent.getHeight() + mRadius;
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(child);
            if (position == 0) {
                top = Math.max(top, child.getTop() + child.getTranslationY());
            }
            if (position == itemCount - 1) {
                bottom = Math.min(bottom, child.getBottom() + child.getTranslationY());
            }
        }
        if (bottom <= top) {
            return;
        }
        mRect.set(0, top, parent.getWidth(), bottom);
        c.drawRoundRect(mRect, mRadius, mRadius, mPaint);
    }
}
