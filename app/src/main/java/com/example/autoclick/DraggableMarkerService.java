package com.example.autoclick;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class DraggableMarkerService extends Service {
    private WindowManager windowManager;
    private View markerView;
    private WindowManager.LayoutParams params;

    // 使用绝对屏幕坐标，不依赖任何UI状态
    private int absoluteClickX = 0;
    private int absoluteClickY = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        System.out.println("DraggableMarkerService 创建");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createDraggableMarker();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeMarker();
        System.out.println("DraggableMarkerService 销毁");
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createDraggableMarker() {
        if (!checkOverlayPermission()) {
            System.out.println("没有悬浮窗权限，无法显示拖拽标记");
            return;
        }

        try {
            removeMarker();

            markerView = LayoutInflater.from(this).inflate(R.layout.click_marker, null);

            // 获取真实屏幕尺寸（包含状态栏、导航栏等所有区域）
            Display display = windowManager.getDefaultDisplay();
            DisplayMetrics realMetrics = new DisplayMetrics();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                display.getRealMetrics(realMetrics);
            } else {
                display.getMetrics(realMetrics);
            }

            int screenWidth = realMetrics.widthPixels;
            int screenHeight = realMetrics.heightPixels;

            System.out.println("真实屏幕尺寸: " + screenWidth + "x" + screenHeight);

            // 悬浮窗参数：使用最宽松的布局标志，完全不受系统UI影响
            int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_FULLSCREEN;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        flags,
                        PixelFormat.TRANSLUCENT
                );
            } else {
                params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_PHONE,
                        flags,
                        PixelFormat.TRANSLUCENT
                );
            }

            params.gravity = Gravity.TOP | Gravity.START;
            params.x = screenWidth / 2;
            params.y = screenHeight / 2;

            markerView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX, initialY;
                private float downRawX, downRawY;
                private boolean dragging = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            downRawX = event.getRawX();
                            downRawY = event.getRawY();
                            dragging = false;

                            // 立即使用绝对屏幕坐标
                            absoluteClickX = Math.round(downRawX);
                            absoluteClickY = Math.round(downRawY);
                            broadcastAbsoluteCoordinate();
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getRawX() - downRawX;
                            float dy = event.getRawY() - downRawY;

                            if (!dragging && (Math.abs(dx) > 10 || Math.abs(dy) > 10)) {
                                dragging = true;
                            }

                            if (dragging) {
                                params.x = initialX + Math.round(dx);
                                params.y = initialY + Math.round(dy);
                                windowManager.updateViewLayout(markerView, params);

                                // 实时使用绝对屏幕坐标，完全不考虑标记位置
                                absoluteClickX = Math.round(event.getRawX());
                                absoluteClickY = Math.round(event.getRawY());
                                broadcastAbsoluteCoordinate();
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            // 最终确认绝对坐标
                            absoluteClickX = Math.round(event.getRawX());
                            absoluteClickY = Math.round(event.getRawY());
                            broadcastAbsoluteCoordinate();

                            System.out.println("最终绝对坐标: (" + absoluteClickX + ", " + absoluteClickY + ")");
                            dragging = false;
                            return true;
                    }
                    return false;
                }
            });

            windowManager.addView(markerView, params);

            // 初始位置的绝对坐标
            markerView.post(() -> {
                absoluteClickX = params.x + markerView.getWidth() / 2;
                absoluteClickY = params.y + markerView.getHeight() / 2;
                broadcastAbsoluteCoordinate();
                System.out.println("初始绝对坐标: (" + absoluteClickX + ", " + absoluteClickY + ")");
            });

            System.out.println("拖拽标记已显示");
        } catch (Exception e) {
            System.out.println("显示拖拽标记失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 广播绝对屏幕坐标，不受任何UI状态影响
     */
    private void broadcastAbsoluteCoordinate() {
        Intent intent = new Intent("coordinate_updated");
        intent.putExtra("x", absoluteClickX);
        intent.putExtra("y", absoluteClickY);
        sendBroadcast(intent);
        System.out.println("广播绝对坐标: (" + absoluteClickX + ", " + absoluteClickY + ")");
    }

    private void removeMarker() {
        if (markerView != null && windowManager != null) {
            try {
                windowManager.removeView(markerView);
                System.out.println("拖拽标记已移除");
            } catch (Exception e) {
                System.out.println("移除拖拽标记失败: " + e.getMessage());
            }
            markerView = null;
        }
    }

    private boolean checkOverlayPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this);
    }
}
