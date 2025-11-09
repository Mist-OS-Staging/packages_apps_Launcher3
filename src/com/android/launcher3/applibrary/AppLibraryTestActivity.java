package com.android.launcher3.applibrary;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import com.android.launcher3.Launcher;

public class AppLibraryTestActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 50, 50, 50);
        
        Button testButton = new Button(this);
        testButton.setText("Test App Library");
        testButton.setOnClickListener(v -> {
            try {
                Launcher launcher = Launcher.getLauncher(this);
                AppLibraryOverlay.show(launcher);
            } catch (Exception e) {
                // Fallback - show directly
                AppLibraryOverlay overlay = new AppLibraryOverlay(this);
                setContentView(overlay);
            }
        });
        
        layout.addView(testButton);
        setContentView(layout);
    }
}
