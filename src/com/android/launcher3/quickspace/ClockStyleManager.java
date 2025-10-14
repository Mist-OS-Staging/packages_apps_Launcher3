package com.android.launcher3.quickspace;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.quickspace.views.AccentedTextClock;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ClockStyleManager {
    
    public static final int STYLE_DEFAULT = 0;
    public static final int STYLE_ONEUI = 1;
    public static final int STYLE_OXYGENOS = 2;
    public static final int STYLE_HYPEROS = 3;
    public static final int STYLE_IOS = 4;
    public static final int STYLE_MODERN = 5;
    
    private Context mContext;
    private int mCurrentStyle;
    
    public ClockStyleManager(Context context) {
        mContext = context;
        updateStyle();
    }
    
    public void updateStyle() {
        String styleValue = LauncherPrefs.getPrefs(mContext).getString(
            LauncherPrefs.HOMESCREEN_CLOCK_STYLE.getSharedPrefKey(), "0");
        mCurrentStyle = Integer.parseInt(styleValue);
    }
    
    public View createClockView() {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        
        switch (mCurrentStyle) {
            case STYLE_ONEUI:
                return inflater.inflate(R.layout.clock_style_oneui, null);
            case STYLE_OXYGENOS:
                return inflater.inflate(R.layout.clock_style_oxygenos, null);
            case STYLE_HYPEROS:
                return inflater.inflate(R.layout.clock_style_hyperos, null);
            case STYLE_IOS:
                return inflater.inflate(R.layout.clock_style_ios, null);
            case STYLE_MODERN:
                return inflater.inflate(R.layout.clock_style_modern, null);
            case STYLE_DEFAULT:
            default:
                return new AccentedTextClock(mContext);
        }
    }
    
    public boolean isCustomStyle() {
        return mCurrentStyle != STYLE_DEFAULT;
    }
    
    public int getCurrentStyle() {
        return mCurrentStyle;
    }
    
    public void updateClockTime(View clockView) {
        if (!isCustomStyle()) return;
        
        TextView dateView = clockView.findViewById(R.id.clock_date);
        if (dateView != null) {
            SimpleDateFormat dateFormat = getDateFormat();
            dateView.setText(dateFormat.format(Calendar.getInstance().getTime()));
        }
        
        // Update day of week for modern style
        if (mCurrentStyle == STYLE_MODERN) {
            TextView dayView = clockView.findViewById(R.id.quickspace_day_of_week);
            if (dayView != null) {
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                dayView.setText(dayFormat.format(Calendar.getInstance().getTime()));
            }
        }
    }
    
    public void updateContextualInfo(View clockView, boolean showPSA, String psaMessage, 
                                   boolean showNowPlaying, String nowPlayingText) {
        if (!isCustomStyle()) return;
        
        View contextualRow = clockView.findViewById(R.id.contextual_info_row);
        View psaView = clockView.findViewById(R.id.quickspace_psa_message);
        View nowPlayingView = clockView.findViewById(R.id.now_playing_content);
        TextView nowPlayingTextView = clockView.findViewById(R.id.now_playing_text);
        
        if (contextualRow == null) return;
        
        boolean hasContent = false;
        
        // Handle PSA
        if (showPSA && psaView != null) {
            ((TextView) psaView).setText(psaMessage);
            psaView.setVisibility(View.VISIBLE);
            hasContent = true;
        } else if (psaView != null) {
            psaView.setVisibility(View.GONE);
        }
        
        // Handle Now Playing
        if (showNowPlaying && nowPlayingView != null && nowPlayingTextView != null) {
            nowPlayingTextView.setText(nowPlayingText);
            nowPlayingView.setVisibility(View.VISIBLE);
            hasContent = true;
        } else if (nowPlayingView != null) {
            nowPlayingView.setVisibility(View.GONE);
        }
        
        // Show/hide contextual row based on content
        contextualRow.setVisibility(hasContent ? View.VISIBLE : View.GONE);
    }
    
    private SimpleDateFormat getDateFormat() {
        switch (mCurrentStyle) {
            case STYLE_ONEUI:
                return new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
            case STYLE_OXYGENOS:
                return new SimpleDateFormat("EEE, dd MMM", Locale.getDefault());
            case STYLE_HYPEROS:
                return new SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault());
            case STYLE_IOS:
                return new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
            case STYLE_MODERN:
                return new SimpleDateFormat("MMM dd", Locale.getDefault());
            default:
                return new SimpleDateFormat("EEE, MMM dd", Locale.getDefault());
        }
    }
}
