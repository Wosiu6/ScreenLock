package com.screenlock;

import android.content.Context;
import android.widget.Toast;

public class Utils {
    public static void DisplayShortToast(String text, Context context) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static void DisplayShortToast(int stringId, Context context) {
        String text = context.getResources().getString(stringId);

        DisplayShortToast(text, context);
    }

    private static String appName;

    public static String getAppName(Context ctx) {
        if (appName == null) {
            appName = ctx.getResources().getString(R.string.app_name);
        }

        return appName;
    }
}
