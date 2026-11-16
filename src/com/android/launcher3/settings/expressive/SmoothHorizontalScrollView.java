/*
 * Copyright (C) 2026 MistOS
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.launcher3.settings.expressive;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.HorizontalScrollView;

import androidx.annotation.Nullable;

public class SmoothHorizontalScrollView extends HorizontalScrollView {

    private final int mTouchSlop;
    private float mDownX;
    private float mDownY;
    private boolean mDirectionDecided;

    public SmoothHorizontalScrollView(Context context) {
        this(context, null);
    }

    public SmoothHorizontalScrollView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setSmoothScrollingEnabled(true);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = ev.getX();
                mDownY = ev.getY();
                mDirectionDecided = false;
                disallowParentIntercept(canScrollHorizontally(1) || canScrollHorizontally(-1));
                break;
            case MotionEvent.ACTION_MOVE:
                if (!mDirectionDecided) {
                    float dx = Math.abs(ev.getX() - mDownX);
                    float dy = Math.abs(ev.getY() - mDownY);
                    if (dx > mTouchSlop || dy > mTouchSlop) {
                        mDirectionDecided = true;
                        disallowParentIntercept(dx > dy);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                disallowParentIntercept(false);
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void disallowParentIntercept(boolean disallow) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }
}
