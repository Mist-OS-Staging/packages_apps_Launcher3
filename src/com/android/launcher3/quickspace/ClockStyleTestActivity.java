package com.android.launcher3.quickspace;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

public class ClockStyleTestActivity extends Activity {
    
    private LinearLayout mClockContainer;
    private ClockStyleManager mClockStyleManager;
    private int mCurrentTestStyle = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        mClockContainer = new LinearLayout(this);
        mClockContainer.setOrientation(LinearLayout.VERTICAL);
        
        Button testButton = new Button(this);
        testButton.setText("Test Next Clock Style");
        testButton.setOnClickListener(v -> testNextStyle());
        
        layout.addView(mClockContainer);
        layout.addView(testButton);
        setContentView(layout);
        
        mClockStyleManager = new ClockStyleManager(this);
        testNextStyle();
    }
    
    private void testNextStyle() {
        mClockContainer.removeAllViews();
        
        String[] styles = {"0", "1", "2", "3", "4", "5"};
        String[] styleNames = {"Default", "OneUI", "OxygenOS", "HyperOS", "iOS", "Modern"};
        
        getSharedPreferences("launcher.prefs", MODE_PRIVATE)
            .edit()
            .putString("pref_homescreen_clock_style", styles[mCurrentTestStyle])
            .apply();
        
        mClockStyleManager.updateStyle();
        
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("Style: " + styleNames[mCurrentTestStyle]);
        title.setTextSize(20);
        title.setPadding(0, 20, 0, 20);
        
        mClockContainer.addView(title);
        mClockContainer.addView(mClockStyleManager.createClockView());
        
        mCurrentTestStyle = (mCurrentTestStyle + 1) % styles.length;
    }
}
