package com.example.autoclick;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

public class ClickMarkerService extends Service {
    private WindowManager windowManager;
    private View markerView;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int x = intent.getIntExtra("clickX", 0);
            int y = intent.getIntExtra("clickY", 0);
            if (x > 0 || y > 0) {
                showMarker(x, y);
                
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        removeMarker();
        
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showMarker(int x, int y) {
        if (!checkOverlayPermission()) {
            
            return;
        }

        try {
            removeMarker();

            // 使用布局文件创建标记视图
            LayoutInflater inflater = LayoutInflater.from(this);
            markerView = inflater.inflate(R.layout.click_marker, null);

            // 测量标记视图的实际大小
            markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int markerWidth = markerView.getMeasuredWidth();
            int markerHeight = markerView.getMeasuredHeight();

            // 如果测量结果为0，使用默认值
            if (markerWidth <= 0) markerWidth = dpToPx(40);
            if (markerHeight <= 0) markerHeight = dpToPx(40);

            // 设置窗口参数 - 添加不受其他窗口影响的标志
            WindowManager.LayoutParams params;
            int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

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
            // 精确居中：使用实际测量尺寸
            params.x = x - markerWidth / 2;
            params.y = y - markerHeight / 2;

            windowManager.addView(markerView, params);
            

        } catch (Exception e) {
            
            e.printStackTrace();
        }
    }

    private void removeMarker() {
        if (markerView != null && windowManager != null) {
            try {
                windowManager.removeView(markerView);
                
            } catch (Exception e) {
                
            }
            markerView = null;
        }
    }

    /**
     * dp转px
     */
    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        return Math.round(dp * displayMetrics.density);
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }
}
