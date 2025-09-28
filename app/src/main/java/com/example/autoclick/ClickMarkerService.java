package com.example.autoclick;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class ClickMarkerService extends Service {
    private WindowManager windowManager;
    private View markerView;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        System.out.println("ClickMarkerService 创建");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int x = intent.getIntExtra("clickX", 0);
            int y = intent.getIntExtra("clickY", 0);
            if (x > 0 || y > 0) {
                showMarker(x, y);
                System.out.println("显示点击标记在: (" + x + ", " + y + ")");
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeMarker();
        System.out.println("ClickMarkerService 销毁");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showMarker(int x, int y) {
        if (!checkOverlayPermission()) {
            System.out.println("没有悬浮窗权限，无法显示点击标记");
            return;
        }

        try {
            // 移除旧的标记
            removeMarker();

            // 使用布局文件创建标记视图
            LayoutInflater inflater = LayoutInflater.from(this);
            markerView = inflater.inflate(R.layout.click_marker, null);

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
            params.x = x - 20; // 居中偏移
            params.y = y - 20; // 居中偏移

            windowManager.addView(markerView, params);
            System.out.println("点击标记已显示");

        } catch (Exception e) {
            System.out.println("显示点击标记失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void removeMarker() {
        if (markerView != null && windowManager != null) {
            try {
                windowManager.removeView(markerView);
                System.out.println("点击标记已移除");
            } catch (Exception e) {
                System.out.println("移除点击标记失败: " + e.getMessage());
            }
            markerView = null;
        }
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }
}
