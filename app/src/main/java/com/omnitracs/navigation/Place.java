package com.omnitracs.navigation;

import android.location.Location;

import com.orm.SugarRecord;

/**
 * Created by Ghrutiaga on 19/01/2017.
 */

public class Place extends SugarRecord {

    public String name;
    public double lat;
    public double lng;

    public Place() {
    }

    public Place(String name, double lat, double lng) {
        this.name = name;
        this.lat = lat;
        this.lng = lng;
    }

}
