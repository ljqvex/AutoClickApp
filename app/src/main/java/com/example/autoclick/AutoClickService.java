package com.example.autoclick;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Path;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.app.NotificationCompat;

public class AutoClickService extends AccessibilityService {
    private static AutoClickService instance;
    private static final String CHANNEL_ID = "AutoClickChannel";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        createNotificationChannel();
        showServiceNotification();
        
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 不需要特殊处理
    }

    @Override
    public void onInterrupt() {
        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        
    }

    public static boolean performClickAt(int x, int y) {
        if (instance == null) {
            
            return false;
        }

        

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                Path path = new Path();
                path.moveTo(x, y);

                GestureDescription.Builder builder = new GestureDescription.Builder();
                GestureDescription.StrokeDescription stroke =
                        new GestureDescription.StrokeDescription(path, 0, 100);
                builder.addStroke(stroke);

                boolean result = instance.dispatchGesture(builder.build(), new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        super.onCompleted(gestureDescription);
                        
                    }

                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        super.onCancelled(gestureDescription);
                        
                    }
                }, null);

                
                return result;
            } catch (Exception e) {
                
                e.printStackTrace();
                return false;
            }
        } else {
            
            return false;
        }
    }

    public static boolean isServiceEnabled(Context context) {
        if (context == null) {
            return false;
        }

        try {
            String enabledServices = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            if (TextUtils.isEmpty(enabledServices)) {
                return false;
            }

            String serviceName = context.getPackageName() + "/" + AutoClickService.class.getName();
            boolean enabled = enabledServices.contains(serviceName);
            
            return enabled;
        } catch (Exception e) {
            
            return false;
        }
    }

    public static AutoClickService getInstance() {
        return instance;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "自动点击服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("自动点击功能运行通知");
            channel.setShowBadge(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showServiceNotification() {
        try {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle("自动点击服务")
                    .setContentText("服务运行中，可执行自动点击")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true)
                    .setShowWhen(false);

            startForeground(NOTIFICATION_ID, builder.build());
        } catch (Exception e) {
            
        }
    }
}
