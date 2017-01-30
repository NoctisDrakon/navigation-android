package com.omnitracs.navigation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Utils {

    public static final String PREFS_NAME = "navigation_prefs";
    private static final String TAG = "Utils";

    public static void setDistance(Context context, float distance) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putFloat("distance", distance);
        editor.apply();
    }

    public static float getDistance(Context context) {
        return context.getSharedPreferences(PREFS_NAME, 0).getFloat("distance", 0f);
    }

    // string
    public static void setAlarmDistance(Context context, int distance) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putInt("alarm_distance", distance);
        editor.apply();
    }

    public static int getAlarmDistance(Context context) {
        return context.getSharedPreferences(PREFS_NAME, 0).getInt("alarm_distance", 100);
    }


}
