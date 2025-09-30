package com.example.autoclick;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import java.util.Locale;
import java.util.Random;

public class ScheduleKeepAliveService extends Service {
    private static final String CHANNEL_ID = "ScheduleKeepAliveChannel";
    private static final int NOTIFICATION_ID = 2001;

    private Handler handler;
    private Runnable scheduleRunnable;
    private long targetTime;
    private int clickX, clickY; // 实时更新的坐标
    private double clickDurationSeconds;  // 改为持续时间（秒）
    private int baseInterval;
    private int randomRange;
    private Random random;
    private long clickingStartTime;  // 点击开始时间
    private long clickingEndTime;    // 点击结束时间

    // 坐标更新接收器
    private BroadcastReceiver coordinateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("coordinate_updated".equals(intent.getAction())) {
                clickX = intent.getIntExtra("x", clickX);
                clickY = intent.getIntExtra("y", clickY);
                
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());
        random = new Random();

        // 注册坐标更新广播接收器
        IntentFilter filter = new IntentFilter("coordinate_updated");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(coordinateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(coordinateReceiver, filter);
        }

        
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            targetTime = intent.getLongExtra("targetTime", 0);
            clickX = intent.getIntExtra("clickX", 0);
            clickY = intent.getIntExtra("clickY", 0);
            clickDurationSeconds = intent.getDoubleExtra("clickDurationSeconds", 5.0);  // 改为持续时间
            baseInterval = intent.getIntExtra("baseInterval", 300);
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

        // 记录开始时间
        clickingStartTime = System.currentTimeMillis();
        clickingEndTime = clickingStartTime + (long)(clickDurationSeconds * 1000);  // 转换为毫秒
        
        System.out.println("开始持续点击: " + clickDurationSeconds + "秒, 结束时间: " + clickingEndTime);
        
        // 开始点击循环
        executeClickLoop();
    }

    private void executeClickLoop() {
        long currentTime = System.currentTimeMillis();
        
        // 执行一次点击
        boolean success = AutoClickService.performClickAt(clickX, clickY);
        System.out.println("执行点击在 (" + clickX + ", " + clickY + "), 结果: " + success);
        
        // 检查是否已经超过持续时间
        if (currentTime >= clickingEndTime) {
            // 持续时间结束，关闭悬浮窗和服务
            System.out.println("点击完成, 实际持续: " + (currentTime - clickingStartTime) + "ms");
            
            // 发送关闭悬浮窗的指令
            Intent closeCountdownIntent = new Intent(this, CountdownOverlayService.class);
            stopService(closeCountdownIntent);

            Intent closeMarkerIntent = new Intent(this, ClickMarkerService.class);
            stopService(closeMarkerIntent);

            Intent closeDragMarkerIntent = new Intent(this, DraggableMarkerService.class);
            stopService(closeDragMarkerIntent);

            // 发送完成广播
            Intent broadcastIntent = new Intent("com.example.autoclick.TASK_COMPLETED");
            sendBroadcast(broadcastIntent);

            // 延迟一下再停止服务，确保悬浮窗关闭完成
            handler.postDelayed(() -> {
                stopSelf();
            }, 500);
            return;
        }

        // 计算下次点击的延迟时间
        int nextDelay = baseInterval;
        if (randomRange > 0) {
            // 在基础间隔基础上增加随机时间
            nextDelay += random.nextInt(randomRange + 1);
        }

        System.out.println("下次点击延迟: " + nextDelay + "ms");
        
        // 延迟后继续下一次点击
        handler.postDelayed(this::executeClickLoop, nextDelay);
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

        // 取消注册广播接收器
        try {
            unregisterReceiver(coordinateReceiver);
        } catch (Exception e) {
            // 忽略
        }

        
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
