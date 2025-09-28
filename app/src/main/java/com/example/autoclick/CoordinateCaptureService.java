package com.example.autoclick;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class CoordinateCaptureService extends Service {

    private WindowManager windowManager;
    private View overlayView;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlayView();
        System.out.println("CoordinateCaptureService 创建");
    }

    private void createOverlayView() {
        if (!checkOverlayPermission()) {
            System.out.println("没有悬浮窗权限，无法显示坐标捕获界面");
            stopSelf();
            return;
        }

        try {
            overlayView = new TextView(this);
            ((TextView) overlayView).setText("点击屏幕任意位置获取坐标\n点击后将自动返回应用");
            ((TextView) overlayView).setTextColor(0xFFFFFFFF);
            ((TextView) overlayView).setBackgroundColor(0x80000000);
            ((TextView) overlayView).setGravity(Gravity.CENTER);
            ((TextView) overlayView).setPadding(50, 50, 50, 50);

            overlayView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        int x = (int) event.getRawX();
                        int y = (int) event.getRawY();
                        
                        // 通过广播发送坐标
                        Intent broadcastIntent = new Intent("coordinate_captured");
                        broadcastIntent.putExtra("x", x);
                        broadcastIntent.putExtra("y", y);
                        sendBroadcast(broadcastIntent);
                        
                        System.out.println("坐标已捕获并发送: (" + x + ", " + y + ")");

                        // 返回应用
                        Intent backIntent = new Intent(getApplicationContext(), MainActivity.class);
                        backIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(backIntent);
                        stopSelf();
                        return true;
                    }
                    return false;
                }
            });

            WindowManager.LayoutParams params;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
            } else {
                params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                        PixelFormat.TRANSLUCENT);
            }
            
            params.gravity = Gravity.TOP | Gravity.LEFT;
            windowManager.addView(overlayView, params);
            System.out.println("坐标捕获界面已显示");
            
        } catch (Exception e) {
            System.out.println("创建坐标捕获界面失败: " + e.getMessage());
            e.printStackTrace();
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
                System.out.println("坐标捕获界面已移除");
            } catch (Exception e) {
                System.out.println("移除坐标捕获界面失败: " + e.getMessage());
            }
        }
        System.out.println("CoordinateCaptureService 销毁");
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }
}
