package com.example.autoclick;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class ScheduleKeepAliveService extends Service {
    private static final String CHANNEL_ID = "ScheduleKeepAliveChannel";
    private static final int NOTIFICATION_ID = 2001;
    
    private Handler handler;
    private Runnable scheduleRunnable;
    private long targetTime;
    private int clickX, clickY;
    private int clickCount;
    private int baseInterval;
    private int randomRange;
    private Random random;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());
        random = new Random();
        System.out.println("ScheduleKeepAliveService 已创建");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            targetTime = intent.getLongExtra("targetTime", 0);
            clickX = intent.getIntExtra("clickX", 0);
            clickY = intent.getIntExtra("clickY", 0);
            clickCount = intent.getIntExtra("clickCount", 1);
            baseInterval = intent.getIntExtra("baseInterval", 1000);
            randomRange = intent.getIntExtra("randomRange", 0);
            
            if (targetTime > 0) {
                startForegroundService();
                scheduleExecution();
            }
        }
        
        return START_STICKY;
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        }

        long remaining = targetTime - System.currentTimeMillis();
        String timeText = String.format(Locale.getDefault(), "剩余 %.1f 秒", remaining / 1000.0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("定时点击服务运行中")
                .setContentText(timeText + " | 坐标:(" + clickX + "," + clickY + ")")
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        System.out.println("前台服务已启动");
    }

    private void scheduleExecution() {
        long delay = targetTime - System.currentTimeMillis();
        
        if (delay <= 0) {
            executeClick();
            return;
        }
        
        scheduleRunnable = new Runnable() {
            @Override
            public void run() {
                executeClick();
            }
        };
        
        handler.postDelayed(scheduleRunnable, delay);
    }

    private void executeClick() {
        if (!AutoClickService.isServiceEnabled(this)) {
            stopSelf();
            return;
        }
        
        executeClickSequence(0);
    }

    private void executeClickSequence(int currentClick) {
        if (currentClick >= clickCount) {
            handler.postDelayed(() -> {
                Intent broadcastIntent = new Intent("com.example.autoclick.TASK_COMPLETED");
                sendBroadcast(broadcastIntent);
                stopSelf();
            }, 2000);
            return;
        }

        AutoClickService.performClickAt(clickX, clickY);

        if (currentClick + 1 < clickCount) {
            int nextDelay = baseInterval;
            if (randomRange > 0) {
                nextDelay += random.nextInt(randomRange);
            }
            handler.postDelayed(() -> executeClickSequence(currentClick + 1), nextDelay);
        } else {
            executeClickSequence(currentClick + 1);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "定时点击保活服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduleRunnable != null && handler != null) {
            handler.removeCallbacks(scheduleRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
