package com.soumen.internetspeedmeter;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final int NOTIFICATION_PERMISSION_REQUEST = 21;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 48, 36, 36);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("Internet Speed Meter");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView creator = new TextView(this);
        creator.setText("Created by Soumen Roy");
        creator.setTextSize(16);
        creator.setGravity(Gravity.CENTER);
        layout.addView(creator);

        TextView info = new TextView(this);
        info.setText("\nShows current download/upload traffic in a persistent notification.\n\nAndroid only permits a small status-bar icon for ordinary apps; numeric speed text is shown in the notification shade.");
        info.setTextSize(16);
        layout.addView(info);

        Button start = new Button(this);
        start.setText("Start speed monitoring");
        start.setOnClickListener(v -> requestPermissionAndStart());
        layout.addView(start);

        Button stop = new Button(this);
        stop.setText("Stop monitoring");
        stop.setOnClickListener(v -> stopService(new Intent(this, SpeedService.class)));
        layout.addView(stop);
        setContentView(layout);
    }

    private void requestPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST);
            return;
        }
        startMeter();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) startMeter();
    }

    private void startMeter() {
        Intent intent = new Intent(this, SpeedService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
        else startService(intent);
    }
}
