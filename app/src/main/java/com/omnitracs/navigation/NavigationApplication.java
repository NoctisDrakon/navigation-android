package com.omnitracs.navigation;

import android.app.Application;
import android.location.Location;

import com.orm.SugarApp;

/**
 * Created by Ghrutiaga on 16/01/2017.
 */

public class NavigationApplication extends SugarApp {
    public static boolean DEBUG;
    public static Location globalLocation;

    @Override
    public void onCreate() {
        DEBUG = true;
        super.onCreate();
    }
}
