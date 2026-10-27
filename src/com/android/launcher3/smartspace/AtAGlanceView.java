/*
 * Copyright (C) 2021-2026 Lunaris & MistOS Projects
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
package com.android.launcher3.smartspace;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Workspace;
import com.android.launcher3.quickspace.QuickSpaceView;

public class AtAGlanceView extends FrameLayout
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Workspace.ScrollableContent {
    private QuickSpaceView mQuickspace;
    private SmartspaceView mSmartspace;
    private boolean mResumed;
    private boolean mVisible;
    private boolean mDestroyed;

    public AtAGlanceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mDestroyed) return;
        LauncherPrefs.getPrefs(getContext()).registerOnSharedPreferenceChangeListener(this);
        updateMode();
    }

    @Override
    protected void onDetachedFromWindow() {
        LauncherPrefs.getPrefs(getContext()).unregisterOnSharedPreferenceChangeListener(this);
        releaseContent();
        super.onDetachedFromWindow();
    }

    @Override
    public void onVisibilityAggregated(boolean isVisible) {
        super.onVisibilityAggregated(isVisible);
        mVisible = isVisible;
        updateListening();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key == null || LauncherPrefs.USE_SMARTSPACE.getSharedPrefKey().equals(key)) {
            updateMode();
        }
        if (key == null || key.startsWith("pref_smartspace_")) {
            if (mSmartspace != null) mSmartspace.refreshSettings();
        }
        if (key == null || key.startsWith("pref_quickspace_")) {
            if (mQuickspace != null) mQuickspace.onDataUpdated();
        }
    }

    private void updateMode() {
        if (mDestroyed) return;
        boolean smartspace = LauncherPrefs.USE_SMARTSPACE.get(getContext());
        if ((smartspace && mSmartspace != null) || (!smartspace && mQuickspace != null)) {
            return;
        }
        releaseContent();
        View content;
        if (smartspace) {
            mSmartspace = new SmartspaceView(getContext());
            content = mSmartspace;
        } else {
            mQuickspace = (QuickSpaceView) LayoutInflater.from(getContext())
                    .inflate(R.layout.quickspace_view, this, false);
            content = mQuickspace;
        }
        addView(content, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        updateListening();
    }

    private void updateListening() {
        boolean active = mResumed && mVisible && isAttachedToWindow() && !mDestroyed;
        if (mSmartspace != null) mSmartspace.setActive(active);
        if (mQuickspace != null) {
            if (active) mQuickspace.onResume();
            else mQuickspace.onPause();
        }
    }

    private void releaseContent() {
        if (mSmartspace != null) mSmartspace.setActive(false);
        if (mQuickspace != null) mQuickspace.onDestroy();
        removeAllViews();
        mSmartspace = null;
        mQuickspace = null;
    }

    @Override
    public boolean canScrollHorizontally() {
        return mSmartspace != null && mSmartspace.canScrollHorizontally();
    }

    public void onResume() {
        mResumed = true;
        updateListening();
    }

    public void onPause() {
        mResumed = false;
        updateListening();
    }

    public void onDestroy() {
        mDestroyed = true;
        LauncherPrefs.getPrefs(getContext()).unregisterOnSharedPreferenceChangeListener(this);
        releaseContent();
    }
}

