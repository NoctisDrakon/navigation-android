package com.omnitracs.navigation;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = "LocationService";
    private GoogleApiClient client;
    private LocationRequest mLocationRequest;
    private Location currentLocation;
    private Toast infoToast;

    public LocationService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        super.onStartCommand(intent, flags, startId);
        client = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        client.connect();

        return START_STICKY;
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        setLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onDestroy() {
        LocationServices.FusedLocationApi.removeLocationUpdates(client, this);

        // only stop if it's connected, otherwise we crash
        if (client != null) {
            client.disconnect();
        }

        EventBus.getDefault().post(new ServiceStopped(true));


        super.onDestroy();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onLocationChanged: Location changed: " + location.getLatitude() + " " + location.getLongitude());
        }
        currentLocation = location;
        NavigationApplication.globalLocation = location;
        EventBus.getDefault().post(new LocationEvent(location));

        List<Place> placesList = new ArrayList<>();

        try {
            placesList = Place.listAll(Place.class);
        } catch (Exception e) {
            if (NavigationApplication.DEBUG) {
                e.printStackTrace();
            }
        }

        if (placesList.size() == 0) {
            //no data found
            if (NavigationApplication.DEBUG) {
                Log.d(TAG, "onLocationChanged: No data stored found");
            }
            return;
        }

        Location desiredPlace = new Location(LocationManager.GPS_PROVIDER);
        desiredPlace.setLatitude(placesList.get(0).lat);
        desiredPlace.setLongitude(placesList.get(0).lng);

        if (desiredPlace != null && currentLocation != null) {
            Location imHere = currentLocation;
            Location wannaGo = desiredPlace;

            float dist = getDistance(imHere, wannaGo);

            float showDist = dist > 999.9 ? dist / 1000 : dist;
            String unity = dist > 999.9 ? "kilómetros" : "metros";


            if (NavigationApplication.DEBUG) {
                Log.d(TAG, "onLocationEvent: Distance is: " + dist);
            }

            showToast("Estás a " + showDist + " " + unity + " de tu destino");


            //post here an event to fragment

        }
    }

    public void setLocationUpdates() {

        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "setLocationUpdates: Setting location updates");
        }

        try {
            if (client == null || !client.isConnected()) {
                if (NavigationApplication.DEBUG) {
                    Log.d(TAG, "setLocationUpdates: Something went wrong with connection");
                }
                return;
            }

            //get last location first
            try {
                EventBus.getDefault().post(LocationServices.FusedLocationApi.getLastLocation(client));
                currentLocation = LocationServices.FusedLocationApi.getLastLocation(client);
            } catch (Exception e) {
                //might be null
                if (NavigationApplication.DEBUG) {
                    e.printStackTrace();
                }
            }
            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(2000)
                    .setFastestInterval(1000);
            // Request location updates
            LocationServices.FusedLocationApi.requestLocationUpdates(client, mLocationRequest, this);

        } catch (SecurityException se) {
            if (NavigationApplication.DEBUG) {
                se.printStackTrace();
            }
        }

    }

    public float getDistance(Location loc1, Location loc2) {

        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "getDistance: calculations! ");
        }

        float distanceInMeters = loc1.distanceTo(loc2);

        return distanceInMeters;
    }

    public class LocationEvent {

        public final Location location;

        public LocationEvent(Location location) {
            this.location = location;
        }
    }

    public class ServiceStopped {

        public final boolean stopped;

        public ServiceStopped(boolean stopped) {
            this.stopped = stopped;
        }
    }

    void showToast(String text) {
        if (infoToast != null) {
            infoToast.cancel();
        }
        infoToast = Toast.makeText(this, text, Toast.LENGTH_LONG);
        infoToast.show();

    }

}
