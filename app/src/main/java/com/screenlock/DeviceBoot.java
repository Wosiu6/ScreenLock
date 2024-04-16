package com.screenlock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

public class DeviceBoot extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        try{
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                Intent serviceIntent = new Intent(context, ScreenLockService.class);
                serviceIntent.putExtra("inputExtra", "Screen Lock");
                ContextCompat.startForegroundService(context, serviceIntent);
            }
        }
        catch (Exception e){
            Utils.DisplayShortToast("Failed to start " + Utils.getAppName(context), context);
        }
    }
}
