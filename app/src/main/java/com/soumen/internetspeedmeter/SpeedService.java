package com.soumen.internetspeedmeter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.Locale;

public class SpeedService extends Service {
    private static final String CHANNEL_ID = "internet_speed_meter_live";
    private static final int NOTIFICATION_ID = 4107;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long previousRx = -1;
    private long previousTx = -1;
    private long previousTime = -1;

    private final Runnable sampler = new Runnable() {
        @Override public void run() {
            long now = System.currentTimeMillis();
            long rx = TrafficStats.getTotalRxBytes();
            long tx = TrafficStats.getTotalTxBytes();
            String down = "0 B/s", up = "0 B/s";

            if (previousTime > 0 && now > previousTime && rx >= 0 && tx >= 0 && previousRx >= 0 && previousTx >= 0) {
                double seconds = (now - previousTime) / 1000.0;
                down = formatRate(Math.max(0, rx - previousRx) / seconds);
                up = formatRate(Math.max(0, tx - previousTx) / seconds);
            }
            previousRx = rx;
            previousTx = tx;
            previousTime = now;
            updateNotification(down, up);
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        Notification initial = buildNotification("Measuring…", "Measuring…");
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, initial);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        handler.removeCallbacks(sampler);
        handler.post(sampler);
        return START_STICKY;
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(sampler);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void createChannel() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26 && manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Live internet speed", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Current download and upload traffic rates");
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }
    }

    private void updateNotification(String down, String up) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, buildNotification(down, up));
    }

    private Notification buildNotification(String down, String up) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
            PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, CHANNEL_ID)
            : new Notification.Builder(this);

        return builder
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Internet Speed Meter")
            .setContentText("↓ " + down + "   ↑ " + up)
            .setStyle(new Notification.BigTextStyle()
                .bigText("Download: " + down + "\nUpload: " + up + "\nCreated by Soumen Roy"))
            .setContentIntent(pending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build();
    }

    private String formatRate(double bytesPerSecond) {
        if (bytesPerSecond < 1024) return String.format(Locale.US, "%.0f B/s", bytesPerSecond);
        if (bytesPerSecond < 1024 * 1024) return String.format(Locale.US, "%.1f KB/s", bytesPerSecond / 1024.0);
        return String.format(Locale.US, "%.2f MB/s", bytesPerSecond / (1024.0 * 1024.0));
    }
}
