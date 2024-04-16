package com.screenlock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class ScreenLockService extends Service implements View.OnTouchListener {
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int SERVICE_FOREGROUND_NOTIFICATION_ID = 565;
    private static final int NOTIFICATION_REQUEST_CODE = 0;
    private static final long NOTIFICATION_UPDATE_INTERVAL = 5000;

    private DevicePolicyManager deviceManger;
    private DisplayManager displayManager;
    private ContentResolver contentResolver;
    private Display[] externalDisplays;
    private int displayTimeout;
    private Timer timer;
    private TimerTask timerTask;
    private LinearLayout touchLayout;
    private static boolean isScreenOff;

    private static Context thisAppContext;

    private final BroadcastReceiver screenOnOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_ON)) {
                if (isScreenOff) {
                    isScreenOff = false;
                    resetTimer();
                }
            } else if (Objects.equals(intent.getAction(), Intent.ACTION_SCREEN_OFF)) {
                isScreenOff = true;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            initialiseServices();
            initialiseTimer();
            initialiseTouchListener();
            setupScreenReceiver();
        } catch (Exception e) {
            Utils.DisplayShortToast(R.string.error_when_starting_the_service, thisAppContext);
            System.exit(0);
        }
    }

    private void initialiseTimer() {
        displayTimeout = getDisplayTimeout();
        timer = new Timer();
        timerTask = getServiceTimerTask();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            thisAppContext = getApplicationContext();
            final NotificationCompat.Builder notifications = createNotificationBuilder();
            refreshExternalDisplayCount();
            StartBackgroundWorker(notifications);

            startForeground(SERVICE_FOREGROUND_NOTIFICATION_ID, notifications.build());
        } catch (Exception e) {
            Utils.DisplayShortToast(getString(R.string.something_went_wrong), thisAppContext);
        }

        Utils.DisplayShortToast(getString(R.string.service_started), thisAppContext);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        cleanTimer();

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (windowManager != null && touchLayout != null) {
            windowManager.removeView(touchLayout);
        }

        SingletonServiceManager.isScreenLockServiceRunning = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private TimerTask getServiceTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                // lock only if connected to external screen
                refreshExternalDisplayCount();
                if (externalDisplays.length > 0) deviceManger.lockNow();
            }
        };
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        resetTimer();
        return false;
    }

    private void setupScreenReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOnOffReceiver, filter);
    }

    private void StartBackgroundWorker(NotificationCompat.Builder notification) {
        // Init timer task
        // notification required for a foreground service to be running
        Thread notificationThread = new Thread(() -> {
            resetTimer();

            while (true) {
                try {
                    Thread.sleep(NOTIFICATION_UPDATE_INTERVAL);

                    int oldDisplayTimeout = displayTimeout;
                    displayTimeout = getDisplayTimeout();

                    if (oldDisplayTimeout != displayTimeout) {
                        resetTimer();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                refreshExternalDisplayCount();
                // notification required for a foreground service to be running
                notification.setContentText(Utils.getAppName(getApplicationContext()) + getString(R.string.service_is_running)
                        + (externalDisplays.length > 0 ? getString(R.string.number_of_external_displays_connected) + externalDisplays.length : getString(R.string.no_external_displays_connected) + "."));
                startForeground(SERVICE_FOREGROUND_NOTIFICATION_ID, notification.build());
            }
        });

        notificationThread.start();
    }

    private void refreshExternalDisplayCount() {
        externalDisplays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
    }

    private void initialiseTouchListener() {
        touchLayout = new LinearLayout(this);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                0);
        touchLayout.setLayoutParams(lp);

        // set on touch listener
        touchLayout.setOnTouchListener(this);

        // fetch window manager object
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // set layout parameter of window manager
        WindowManager.LayoutParams mParams = new WindowManager.LayoutParams(
                0,
                0,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                , PixelFormat.TRANSLUCENT
        );

        mParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowManager.addView(touchLayout, mParams);
    }

    private void initialiseServices() {
        // Manager init
        deviceManger = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);

        // Resolver Init
        contentResolver = getContentResolver();
    }

    private NotificationCompat.Builder createNotificationBuilder() {
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, NOTIFICATION_REQUEST_CODE, notificationIntent, PendingIntent.FLAG_MUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(Utils.getAppName(getApplicationContext()))
                .setContentText(getString(R.string.service_running))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setVibrate(new long[1]) // Passing null here silently fails
                .setContentIntent(pendingIntent);
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                Utils.getAppName(getApplicationContext()) + " Channel",
                NotificationManager.IMPORTANCE_LOW
        );
        serviceChannel.enableVibration(false);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);

        SingletonServiceManager.isScreenLockServiceRunning = true;
    }

    private void resetTimer() {
        cleanTimer();

        timer = new Timer();

        timerTask = getServiceTimerTask();

        scheduleTimerTask();
    }

    private void cleanTimer() {
        if (timerTask != null) {
            timerTask.cancel();
        }

        if (timer != null) {
            timer.cancel();
            timer.purge();
        }
    }

    private void scheduleTimerTask() {
        displayTimeout = getDisplayTimeout();
        timer.schedule(timerTask, displayTimeout);
    }

    private int getDisplayTimeout() {
        try {
            contentResolver = getContentResolver();
            return Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Settings.SettingNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}