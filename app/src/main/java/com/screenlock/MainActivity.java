package com.screenlock;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private Button btnStartService, btnStopService, btnGetAdmin, btnGetDisplayOverAppsPermission;
    private TextView txtServiceStatus, txtAdminStatus, txtDrawStatus;
    private ImageView imgQuit;
    private ComponentName adminComponentName;
    private static final int TIMER_DELAY = 1000;
    private Timer timer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        btnStartService = findViewById(R.id.btn_startService);
        btnStopService = findViewById(R.id.btn_stopService);
        btnGetAdmin = findViewById(R.id.btn_grantAdmin);

        txtAdminStatus = findViewById(R.id.txt_adminPermissionFeedback);
        txtDrawStatus = findViewById(R.id.txt_drawOverPermissionFeedback);
        txtServiceStatus = findViewById(R.id.txt_serviceStatus);

        imgQuit = findViewById(R.id.img_quit);
        imgQuit.setOnClickListener(v -> {
            finish();
            System.exit(0);
        });

        btnGetDisplayOverAppsPermission = findViewById(R.id.btn_grantSpecial);
        btnStartService.setOnClickListener(v -> startService());
        btnStopService.setOnClickListener(v -> stopService());
        btnGetAdmin.setOnClickListener(v -> getAdmin());
        btnGetDisplayOverAppsPermission.setOnClickListener(v -> getDisplayOverAppsPermission());

        adminComponentName = new ComponentName(getApplicationContext(), DeviceAdmin.class);

        timer = new Timer();
        timer.schedule(getUiUpdateTimerTask(), TIMER_DELAY);
    }

    public void startService() {
        if (!hasAdmin(getApplicationContext())) {
            Utils.DisplayShortToast(R.string.needs_administrator_rights, getApplicationContext());
        }
        else if (!canDrawOverApps(getApplicationContext())) {
            Utils.DisplayShortToast(R.string.needs_draw_over_apps, getApplicationContext());
        }
        else {
            Intent serviceIntent = new Intent(this, ScreenLockService.class);
            serviceIntent.putExtra("inputExtra", R.string.app_name);
            ContextCompat.startForegroundService(this, serviceIntent);
        }
    }

    public void stopService() {
        try{
            Intent serviceIntent = new Intent(getApplicationContext(), ScreenLockService.class);
            stopService(serviceIntent);
        }
        catch (Exception e){
            Utils.DisplayShortToast(R.string.stopping_service_failed, getApplicationContext());
        }
    }

    public void getDisplayOverAppsPermission(){
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void getAdmin(){
        if (hasAdmin(getApplicationContext())) {
            startActivity(new Intent().setComponent(new ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings")));
        }
        else {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponentName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, R.string.needs_administrator_rights);
            startActivity(intent);
        }
    }

    public boolean hasAdmin(Context context){
        DevicePolicyManager deviceManger = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        return deviceManger.isAdminActive(adminComponentName);
    }

    public static boolean canDrawOverApps(Context context) {
        return Settings.canDrawOverlays(context);
    }

    private boolean isServiceRunning() {
        return SingletonServiceManager.isScreenLockServiceRunning;
    }

    private TimerTask updateUiTimerTask;
    private TimerTask getUiUpdateTimerTask() {
        updateUiTimerTask = new TimerTask() {
            @Override
            public void run() {
                boolean hasAdmin = hasAdmin(getApplicationContext());
                boolean hasSpecialPermissions = canDrawOverApps(getApplicationContext());
                boolean isServiceRunning = isServiceRunning();

                txtAdminStatus.setText(getString(hasAdmin ? R.string.permission_granted_feedback : R.string.permission_denied_feedback));
                txtAdminStatus.setTextColor(getColor(hasAdmin ? R.color.permission_granted : R.color.permission_denied));

                txtDrawStatus.setText(getString(hasSpecialPermissions ? R.string.permission_granted_feedback : R.string.permission_denied_feedback));
                txtDrawStatus.setTextColor(getColor(hasSpecialPermissions ? R.color.permission_granted : R.color.permission_denied));

                txtServiceStatus.setText(getString(isServiceRunning ? R.string.running_feedback : R.string.stopped_feedback));
                txtServiceStatus.setTextColor(getColor(isServiceRunning ? R.color.permission_granted : R.color.permission_denied));

                Runnable adjustButtons;
                if (SingletonServiceManager.isScreenLockServiceRunning) {
                    adjustButtons = () -> {
                        btnStartService.setVisibility(View.GONE);
                        btnStopService.setVisibility(View.VISIBLE);
                    };
                } else{
                    adjustButtons = () -> {
                        btnStartService.setVisibility(View.VISIBLE);
                        btnStopService.setVisibility(View.GONE);
                    };
                }
                runOnUiThread(adjustButtons);

                timer.schedule(getUiUpdateTimerTask(), TIMER_DELAY);
            }
        };

        return updateUiTimerTask;
    }
}