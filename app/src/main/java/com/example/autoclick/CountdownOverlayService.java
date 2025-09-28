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

            // 使用布局文件创建倒计时视图
            LayoutInflater inflater = LayoutInflater.from(this);
            overlayView = inflater.inflate(R.layout.overlay_countdown, null);
            tvCountdown = overlayView.findViewById(R.id.tv_overlay_countdown);

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

            // 设置为顶部正中
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.y = 50; // 距离顶部50像素

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
                    // 倒计时结束，立即关闭
                    System.out.println("倒计时结束，立即关闭");
                    stopSelf();
                    return;
                }

                // 更新倒计时显示
                updateCountdownDisplay(remainingTime);

                // 安排下次更新（每50毫秒更新一次，减少延迟）
                handler.postDelayed(this, 50);
            }
        };

        handler.post(countdownRunnable);
    }

    private void updateCountdownDisplay(long remainingTime) {
        if (tvCountdown == null) return;

        // 根据剩余时间设置颜色
        int textColor;
        if (remainingTime > 60000) { // 大于1分钟 - 绿色
            textColor = 0xFF00FF00;
        } else if (remainingTime > 10000) { // 大于10秒小于1分钟 - 黄色
            textColor = 0xFFFFFF00;
        } else { // 10秒内 - 红色
            textColor = 0xFFFF0000;
        }
        tvCountdown.setTextColor(textColor);

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

        tvCountdown.setText(countdownText);
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
