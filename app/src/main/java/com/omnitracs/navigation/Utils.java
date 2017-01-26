package com.omnitracs.navigation;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Ghrutiaga on 26/01/2017.
 */

public class Utils {

    public static final String PREFS_NAME = "navigation_prefs";

    // string
    public static void setDistance(Context context, float distance) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, 0).edit();
        editor.putFloat("distance", distance);
        editor.apply();
    }

    public static float getDistance(Context context) {
        return context.getSharedPreferences(PREFS_NAME, 0).getFloat("distance", 0f);
    }


}
