package com.omnitracs.navigation;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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
    private static final int NOTIFICATION_ID = 3323342;
    private static final long[] pattern = {100, 200, 100, 200, 100, 200, 100, 200, 100, 100, 100, 100, 100, 200, 100, 200, 100, 200, 100, 200, 100, 100, 100, 100, 100, 200, 100, 200, 100, 200, 100, 200, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 50, 50, 100, 800};

    private GoogleApiClient client;
    private LocationRequest mLocationRequest;
    private Location currentLocation;
    private Toast infoToast;
    private NotificationCompat.Builder mBuilder;
    private boolean vibrating = false;
    private boolean actionAdded = false;
    private Ringtone r;
    private Vibrator v;

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

        r = RingtoneManager.getRingtone(getApplicationContext(), RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        showNotification();

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
        cancelNotification(this, NOTIFICATION_ID);
        nukeAlarm();


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

            float dist = getDistance(imHere, wannaGo); //distance in meters

            /*quick operations to show understandable info to user*/
            float showDist = dist > 999.9 ? dist / 1000 : dist;
            String unity = dist > 999.9 ? "Km." : "Mts.";
            String formattedOutput = String.format(dist > 999.9 ? "%.1f" : "%.0f", showDist);

            if (NavigationApplication.DEBUG) {
                Log.d(TAG, "onLocationEvent: Distance is: " + dist);
            }

            if (dist < Utils.getAlarmDistance(this)) {
                playAlarm();
            }

            EventBus.getDefault().post(new LocationEvent(location, formattedOutput, unity, dist));
            updateNotification(formattedOutput + " " + unity, dist < Utils.getAlarmDistance(this));
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
        public final String distance;
        public final String unity;
        public final float rawDistance;

        public LocationEvent(Location location, String distance, String unity, float rawDistance) {
            this.location = location;
            this.distance = distance;
            this.unity = unity;
            this.rawDistance = rawDistance;
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

    private void showNotification() {

        Intent resultIntent = new Intent(this, MainActivity.class);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                        .setContentTitle("Servicio de rastreo en ejecución")
                        .setContentText("Calculando distancia...")
                        .setContentIntent(resultPendingIntent)
                        .setOngoing(true);

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, mBuilder.build());

    }

    private void updateNotification(String distance, boolean isNear) {
        mBuilder.setContentText(String.format(getString(R.string.distance_format), distance));

        if (isNear && !actionAdded) {
            Intent intent = new Intent(this, MainActivity.class).putExtra("nuke", true);
            PendingIntent dismissIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.addAction(new NotificationCompat.Action(android.R.drawable.stat_notify_call_mute, "Desactivar", dismissIntent));
            actionAdded = true;
        }

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, mBuilder.build());
    }

    private void cancelNotification(Context ctx, int notifyId) {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) ctx.getSystemService(ns);
        nMgr.cancel(notifyId);
    }

    private void playAlarm() {
        if (!r.isPlaying()) {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            am.setStreamVolume(AudioManager.STREAM_RING, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            r.play();
        }


        if (!vibrating) {
            vibrating = true;
            v.vibrate(pattern, -1);
        }


    }

    private void nukeAlarm() {
        if (r.isPlaying()) {
            r.stop();
        }

        if (vibrating) {
            v.cancel();
            vibrating = false;
        }
    }

}
