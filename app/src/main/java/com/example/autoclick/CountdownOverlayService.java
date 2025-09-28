package com.example.autoclick;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import java.util.Locale;

public class CountdownOverlayService extends Service {
    private WindowManager windowManager;
    private View overlayView;
    private TextView tvCountdown;
    private Handler handler;
    private Runnable countdownRunnable;
    private long targetTime;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        System.out.println("CountdownOverlayService 创建");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            targetTime = intent.getLongExtra("target_time", 0);
            if (targetTime > 0) {
                createOverlayView();
                startCountdown();
                System.out.println("倒计时开始，目标时间: " + targetTime);
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeOverlayView();
        if (handler != null && countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
        System.out.println("CountdownOverlayService 销毁");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createOverlayView() {
        if (!checkOverlayPermission()) {
            System.out.println("没有悬浮窗权限，无法显示倒计时");
            return;
        }

        try {
            // 移除旧的视图
            removeOverlayView();

            // 创建倒计时文本视图
            tvCountdown = new TextView(this);
            tvCountdown.setText("倒计时");
            tvCountdown.setTextColor(0xFFFFFFFF);
            tvCountdown.setTextSize(16);
            tvCountdown.setBackgroundColor(0x80000000);
            tvCountdown.setPadding(20, 10, 20, 10);

            // 设置窗口参数
            WindowManager.LayoutParams params;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        PixelFormat.TRANSLUCENT
                );
            } else {
                params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        PixelFormat.TRANSLUCENT
                );
            }

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 100;
            params.y = 100;

            overlayView = tvCountdown;
            windowManager.addView(overlayView, params);
            System.out.println("倒计时悬浮窗已创建");

        } catch (Exception e) {
            System.out.println("创建倒计时悬浮窗失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startCountdown() {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long remainingTime = targetTime - currentTime;

                if (remainingTime <= 0) {
                    // 倒计时结束
                    if (tvCountdown != null) {
                        tvCountdown.setText("执行中...");
                    }
                    System.out.println("倒计时结束");

                    // 2秒后自动关闭
                    handler.postDelayed(() -> {
                        stopSelf();
                    }, 2000);
                    return;
                }

                // 更新倒计时显示
                updateCountdownDisplay(remainingTime);

                // 安排下次更新（每100毫秒更新一次）
                handler.postDelayed(this, 100);
            }
        };

        handler.post(countdownRunnable);
    }

    private void updateCountdownDisplay(long remainingTime) {
        if (tvCountdown == null) return;

        long hours = remainingTime / (1000 * 60 * 60);
        long minutes = (remainingTime % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (remainingTime % (1000 * 60)) / 1000;
        long milliseconds = remainingTime % 1000;

        String countdownText;
        if (hours > 0) {
            countdownText = String.format(Locale.getDefault(),
                    "%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
        } else if (minutes > 0) {
            countdownText = String.format(Locale.getDefault(),
                    "%02d:%02d.%03d", minutes, seconds, milliseconds);
        } else {
            countdownText = String.format(Locale.getDefault(),
                    "%02d.%03d", seconds, milliseconds);
        }

        tvCountdown.setText("倒计时: " + countdownText);
    }

    private void removeOverlayView() {
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
                System.out.println("倒计时悬浮窗已移除");
            } catch (Exception e) {
                System.out.println("移除倒计时悬浮窗失败: " + e.getMessage());
            }
            overlayView = null;
            tvCountdown = null;
        }
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }
}