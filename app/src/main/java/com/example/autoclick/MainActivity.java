package com.example.autoclick;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private EditText etHour, etMinute, etSecond, etMillisecond;
    private EditText etClickCount, etClickInterval, etRandomRange;
    private TextView tvCoordinates, tvStatus;
    private Button btnGetCoordinates, btnScheduleClick, btnOpenSettings, btnStopClick, btnClearMarker;
    private LinearLayout layoutInterval;

    private int clickX = 0, clickY = 0;
    private long targetTimeMillis = 0;
    private boolean isTaskRunning = false;

    // 点击参数
    private double clickDurationSeconds = 5.0;  // 改为持续时间（秒）
    private int baseInterval = 200;
    private int randomRange = 100;
    private Random random = new Random();

    // Handler 用于定时任务
    private Handler mainHandler;
    private Runnable scheduleRunnable;

    // 广播接收器，用于接收坐标
    private BroadcastReceiver coordinateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("coordinate_captured".equals(intent.getAction())) {
                clickX = intent.getIntExtra("x", 0);
                clickY = intent.getIntExtra("y", 0);
                tvCoordinates.setText("坐标: (" + clickX + ", " + clickY + ")");
                updateStatus("坐标已获取");

                // 停止坐标捕获服务
                stopService(new Intent(MainActivity.this, CoordinateCaptureService.class));
                
                // 显示点击标记
                Intent markerIntent = new Intent(MainActivity.this, ClickMarkerService.class);
                markerIntent.putExtra("clickX", clickX);
                markerIntent.putExtra("clickY", clickY);
                startService(markerIntent);
            }
        }
    };

    // 任务完成广播接收器
    private BroadcastReceiver taskCompletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.autoclick.TASK_COMPLETED".equals(intent.getAction())) {
                updateStatus("所有点击已完成");
                btnStopClick.setVisibility(View.GONE);
                btnScheduleClick.setEnabled(true);
                btnGetCoordinates.setEnabled(true);
                isTaskRunning = false;
                
                // 自动清除标记和倒计时
                stopService(new Intent(MainActivity.this, ClickMarkerService.class));
                stopService(new Intent(MainActivity.this, CountdownOverlayService.class));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initHandlers();
        registerReceivers();
        setupListeners();
        checkServiceStatus();

        // 初始化默认值
        setDefaultValues();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceivers();
        stopAllOperations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkServiceStatus();
    }

    private void initViews() {
        // 时间输入
        etHour = findViewById(R.id.et_hour);
        etMinute = findViewById(R.id.et_minute);
        etSecond = findViewById(R.id.et_second);
        etMillisecond = findViewById(R.id.et_millisecond);

        // 点击参数
        etClickCount = findViewById(R.id.et_click_count);
        etClickInterval = findViewById(R.id.et_click_interval);
        etRandomRange = findViewById(R.id.et_random_range);
        layoutInterval = findViewById(R.id.layout_interval);

        // 状态显示
        tvCoordinates = findViewById(R.id.tv_coordinates);
        tvStatus = findViewById(R.id.tv_status);

        // 按钮
        btnGetCoordinates = findViewById(R.id.btn_get_coordinates);
        btnScheduleClick = findViewById(R.id.btn_schedule_click);
        btnOpenSettings = findViewById(R.id.btn_open_settings);
        btnStopClick = findViewById(R.id.btn_stop_click);
        btnClearMarker = findViewById(R.id.btn_clear_marker);
    }

    private void initHandlers() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void registerReceivers() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(coordinateReceiver, new IntentFilter("coordinate_captured"), Context.RECEIVER_NOT_EXPORTED);
            registerReceiver(taskCompletedReceiver, new IntentFilter("com.example.autoclick.TASK_COMPLETED"), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(coordinateReceiver, new IntentFilter("coordinate_captured"));
            registerReceiver(taskCompletedReceiver, new IntentFilter("com.example.autoclick.TASK_COMPLETED"));
        }
    }

    private void unregisterReceivers() {
        try {
            if (coordinateReceiver != null) {
                unregisterReceiver(coordinateReceiver);
            }
            if (taskCompletedReceiver != null) {
                unregisterReceiver(taskCompletedReceiver);
            }
        } catch (Exception e) {
            System.out.println("注销广播接收器失败: " + e.getMessage());
        }
    }

    private void setupListeners() {
        btnGetCoordinates.setOnClickListener(this::onGetCoordinatesClick);
        btnScheduleClick.setOnClickListener(this::onScheduleClickClick);
        btnOpenSettings.setOnClickListener(this::onOpenSettingsClick);
        btnStopClick.setOnClickListener(this::onStopClickClick);
        btnClearMarker.setOnClickListener(this::onClearMarkerClick);

        // 持续时间变化监听
        etClickCount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateIntervalVisibility();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setDefaultValues() {
        // 设置当前时间加10秒作为默认时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, 10);
        
        etHour.setText(String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.HOUR_OF_DAY)));
        etMinute.setText(String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.MINUTE)));
        etSecond.setText(String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.SECOND)));
        etMillisecond.setText("0");
        etClickCount.setText("5");
        etClickInterval.setText("180");
        etRandomRange.setText("100");
        updateIntervalVisibility();
    }

    private void updateIntervalVisibility() {
        try {
            String durationText = etClickCount.getText().toString();
            if (durationText.isEmpty()) {
                layoutInterval.setVisibility(View.GONE);
                return;
            }
            
            double duration = Double.parseDouble(durationText);
            // 持续时间大于0就显示间隔设置
            layoutInterval.setVisibility(duration > 0 ? View.VISIBLE : View.GONE);
            System.out.println("持续时间: " + duration + "秒, 间隔设置可见性: " + (duration > 0));
        } catch (NumberFormatException e) {
            layoutInterval.setVisibility(View.GONE);
        }
    }

    private void checkServiceStatus() {
        boolean accessibilityEnabled = AutoClickService.isServiceEnabled(this);
        boolean overlayPermissionGranted = checkOverlayPermission();

        if (!accessibilityEnabled) {
            updateStatus("需要开启无障碍服务");
            btnOpenSettings.setVisibility(View.VISIBLE);
        } else if (!overlayPermissionGranted) {
            updateStatus("需要悬浮窗权限");
            btnOpenSettings.setVisibility(View.GONE);
        } else {
            updateStatus("服务已就绪");
            btnOpenSettings.setVisibility(View.GONE);
        }
    }

    private void onGetCoordinatesClick(View view) {
        if (isTaskRunning) {
            showToast("任务进行中，无法更改坐标");
            return;
        }
        
        if (!checkOverlayPermission()) {
            requestOverlayPermission();
            return;
        }

        // 清除之前的标记
        stopService(new Intent(this, ClickMarkerService.class));

        // 先最小化应用
        moveTaskToBack(true);
        
        // 延迟启动坐标捕获，确保应用已最小化
        mainHandler.postDelayed(() -> {
            startService(new Intent(this, CoordinateCaptureService.class));
        }, 500);
        
        updateStatus("请点击屏幕选择坐标");
    }

    private void onScheduleClickClick(View view) {
        if (!validateInputs()) return;
        if (!checkAccessibilityService()) {
            showAccessibilityDialog();
            return;
        }
        if (!checkOverlayPermission()) {
            requestOverlayPermission();
            return;
        }

        parseInputs();
        calculateTargetTime();

        if (targetTimeMillis <= System.currentTimeMillis()) {
            showToast("设定时间已过，请重新设置");
            return;
        }

        startScheduledClick();
    }

    private void onOpenSettingsClick(View view) {
        showAccessibilityDialog();
    }

    private void onStopClickClick(View view) {
        stopAllOperations();
        updateStatus("已停止所有操作");
        btnStopClick.setVisibility(View.GONE);
        btnScheduleClick.setEnabled(true);
        btnGetCoordinates.setEnabled(true);
        isTaskRunning = false;
        
        // 清除标记和倒计时
        stopService(new Intent(this, ClickMarkerService.class));
        stopService(new Intent(this, CountdownOverlayService.class));
    }

    private void onClearMarkerClick(View view) {
        if (isTaskRunning) {
            showToast("任务进行中，无法清除标记");
            return;
        }
        
        // 清除所有标记和服务
        stopService(new Intent(this, ClickMarkerService.class));
        stopService(new Intent(this, CoordinateCaptureService.class));
        stopService(new Intent(this, CountdownOverlayService.class));
        clickX = 0;
        clickY = 0;
        tvCoordinates.setText("坐标: 未设置");
        updateStatus("已清除所有标记");
    }

    private boolean validateInputs() {
        try {
            int hour = Integer.parseInt(etHour.getText().toString());
            int minute = Integer.parseInt(etMinute.getText().toString());
            int second = Integer.parseInt(etSecond.getText().toString());
            int millisecond = Integer.parseInt(etMillisecond.getText().toString());

            if (hour < 0 || hour > 23 || minute < 0 || minute > 59 ||
                    second < 0 || second > 59 || millisecond < 0 || millisecond > 999) {
                showToast("时间格式错误");
                return false;
            }

            double duration = Double.parseDouble(etClickCount.getText().toString());
            if (duration <= 0) {
                showToast("持续时间必须大于0");
                return false;
            }

            if (duration > 0) {
                int interval = Integer.parseInt(etClickInterval.getText().toString());
                int range = Integer.parseInt(etRandomRange.getText().toString());
                if (interval <= 0 || range < 0) {
                    showToast("点击间隔参数错误");
                    return false;
                }
            }

            if (clickX == 0 && clickY == 0) {
                showToast("请先获取坐标");
                return false;
            }

            return true;
        } catch (NumberFormatException e) {
            showToast("输入格式错误");
            return false;
        }
    }

    private void parseInputs() {
        clickDurationSeconds = Double.parseDouble(etClickCount.getText().toString());
        if (clickDurationSeconds > 0) {
            baseInterval = Integer.parseInt(etClickInterval.getText().toString());
            randomRange = Integer.parseInt(etRandomRange.getText().toString());
        } else {
            baseInterval = 300;
            randomRange = 0;
        }
    }

    private void calculateTargetTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(etHour.getText().toString()));
        calendar.set(Calendar.MINUTE, Integer.parseInt(etMinute.getText().toString()));
        calendar.set(Calendar.SECOND, Integer.parseInt(etSecond.getText().toString()));
        calendar.set(Calendar.MILLISECOND, Integer.parseInt(etMillisecond.getText().toString()));

        targetTimeMillis = calendar.getTimeInMillis();

        // 如果时间已过，设置为明天同一时间
        if (targetTimeMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            targetTimeMillis = calendar.getTimeInMillis();
        }
    }

    private void startScheduledClick() {
        // 停止之前的任务
        stopAllOperations();
        
        isTaskRunning = true;
        btnGetCoordinates.setEnabled(false);

        long delay = targetTimeMillis - System.currentTimeMillis();

        // 启动倒计时服务
        Intent countdownIntent = new Intent(this, CountdownOverlayService.class);
        countdownIntent.putExtra("target_time", targetTimeMillis);
        startService(countdownIntent);

        // 确保显示点击标记
        Intent markerIntent = new Intent(this, ClickMarkerService.class);
        markerIntent.putExtra("clickX", clickX);
        markerIntent.putExtra("clickY", clickY);
        startService(markerIntent);

        // 启动保活服务
        Intent keepAliveIntent = new Intent(this, ScheduleKeepAliveService.class);
        keepAliveIntent.putExtra("targetTime", targetTimeMillis);
        keepAliveIntent.putExtra("clickX", clickX);
        keepAliveIntent.putExtra("clickY", clickY);
        keepAliveIntent.putExtra("clickDurationSeconds", clickDurationSeconds);  // 改为持续时间
        keepAliveIntent.putExtra("baseInterval", baseInterval);
        keepAliveIntent.putExtra("randomRange", randomRange);
        startService(keepAliveIntent);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
        updateStatus("已安排 " + sdf.format(new Date(targetTimeMillis)) + " 执行点击");
        
        btnStopClick.setVisibility(View.VISIBLE);
        btnScheduleClick.setEnabled(false);

        // 最小化应用
        moveTaskToBack(true);

        System.out.println("安排点击任务，延迟: " + delay + "ms，目标时间: " + targetTimeMillis);
    }

    private void stopAllOperations() {
        if (scheduleRunnable != null) {
            mainHandler.removeCallbacks(scheduleRunnable);
            scheduleRunnable = null;
        }

        // 停止所有服务
        stopService(new Intent(this, CountdownOverlayService.class));
        stopService(new Intent(this, CoordinateCaptureService.class));
        stopService(new Intent(this, ScheduleKeepAliveService.class));

        System.out.println("所有操作已停止");
    }

    private boolean checkAccessibilityService() {
        return AutoClickService.isServiceEnabled(this);
    }

    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this)
                .setTitle("开启无障碍服务")
                .setMessage("需要开启无障碍服务来执行自动点击")
                .setPositiveButton("去设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void updateStatus(String status) {
        runOnUiThread(() -> {
            tvStatus.setText("状态: " + status);
            System.out.println("状态更新: " + status);
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
