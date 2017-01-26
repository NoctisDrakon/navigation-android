package com.omnitracs.navigation;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.ui.PlacePicker;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION = 332;
    private static final int PLACES_REQUEST = 323;
    private int mSelectedItem;
    private GoogleApiClient client;
    private Toast infoToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onCreate: Extra in intent: " + getIntent().getBooleanExtra("nuke", false));
        }

        if (getIntent().getBooleanExtra("nuke", false)) {
            stopService(new Intent(getApplicationContext(), LocationService.class));
            Toast.makeText(MainActivity.this, "El servicio se ha detenido", Toast.LENGTH_SHORT).show();
        }

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);

        if (isMyServiceRunning(LocationService.class)) {
            fab.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_media_pause));
        } else {
            fab.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_media_play));
        }

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isMyServiceRunning(LocationService.class)) {
                    stopService(new Intent(getApplicationContext(), LocationService.class));
                    Toast.makeText(MainActivity.this, "El servicio se ha detenido", Toast.LENGTH_SHORT).show();
                    fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), android.R.drawable.ic_media_play));
                } else {


                    List<Place> placesList = new ArrayList<>();

                    try {
                        placesList = Place.listAll(Place.class);
                    } catch (Exception e) {
                        if (NavigationApplication.DEBUG) {
                            e.printStackTrace();
                        }
                    }

                    if (placesList.size() == 0) {
                        showToast(getString(R.string.no_selected_place));
                        return;
                    } else {
                        //get place and check current distance
                        Place p = placesList.get(0);

                        Location placeLocation = new Location(LocationManager.GPS_PROVIDER);
                        placeLocation.setLatitude(p.lat);
                        placeLocation.setLongitude(p.lng);

                        try {
                            if (NavigationApplication.globalLocation.distanceTo(placeLocation) < 100) {
                                showToast(getString(R.string.near_to_place_prompt));
                                return;
                            }
                        } catch (Exception ex) {
                            startService(new Intent(getApplicationContext(), LocationService.class));
                            Toast.makeText(MainActivity.this, "El servicio se ha iniciado", Toast.LENGTH_SHORT).show();
                            fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), android.R.drawable.ic_media_pause));
                        }
                    }

                    startService(new Intent(getApplicationContext(), LocationService.class));
                    Toast.makeText(MainActivity.this, "El servicio se ha iniciado", Toast.LENGTH_SHORT).show();
                    fab.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), android.R.drawable.ic_media_pause));
                }

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mSelectedItem = R.id.nav_camera;
        navigate(mSelectedItem);

        client = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        client.connect();

    }

    private void navigate(int mItemId) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        switch (mItemId) {
            case (R.id.nav_camera):
                transaction.replace(R.id.fragment_container, NavigationFragment.newInstance());
                transaction.commit();
                break;
        }
    }

    @Override
    protected void onStop() {
        //LocationServices.FusedLocationApi.removeLocationUpdates(client, this);

        // only stop if it's connected, otherwise we crash
        /*if (client != null) {
            client.disconnect();
        }*/
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            //startActivityForResult(new Intent(this, SearchPlacesActivity.class), PLACES_REQUEST);

            // Construct an intent for the place picker
            try {
                PlacePicker.IntentBuilder intentBuilder =
                        new PlacePicker.IntentBuilder();
                Intent intent = intentBuilder.build(this);
                // Start the intent by requesting a result,
                // identified by a request code.
                startActivityForResult(intent, PLACES_REQUEST);

            } catch (GooglePlayServicesRepairableException e) {
                // ...
            } catch (GooglePlayServicesNotAvailableException e) {
                // ...
            }


            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        mSelectedItem = id;
        navigate(id);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PLACES_REQUEST && resultCode == Activity.RESULT_OK) {

            final com.google.android.gms.location.places.Place place = PlacePicker.getPlace(this, data);
            EventBus.getDefault().post(new PlaceReceived(new Place(place.getName().toString(), place.getLatLng().latitude, place.getLatLng().longitude)));

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /*@Override
    public void onConnected(@Nullable Bundle bundle) {
        setLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }*/

   /* public void setLocationUpdates() {

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
            NavigationApplication.globalLocation = LocationServices.FusedLocationApi.getLastLocation(client);

            if (NavigationApplication.globalLocation != null) {
                if (NavigationApplication.DEBUG) {
                    Log.d(TAG, "setLocationUpdates: Last known location is:  " + NavigationApplication.globalLocation.getLongitude() + " <-> " + NavigationApplication.globalLocation.getLatitude());
                }
                EventBus.getDefault().post(new LocationEvent(NavigationApplication.globalLocation));
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

    @Override
    public void onLocationChanged(Location location) {
        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onLocationChanged: Location changed: " + location.getLatitude() + " " + location.getLongitude());
        }
        NavigationApplication.globalLocation = location;
        EventBus.getDefault().post(new LocationEvent(NavigationApplication.globalLocation));
    }

    public class LocationEvent {

        public final Location location;

        public LocationEvent(Location location) {
            this.location = location;
        }
    }*/

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onConnected: Main Activity Client connected");
        }

        getPermissions();
    }

    private void getPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation();
        } else {
            //ask for it
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION);

        }
    }

    private void getLastLocation() {
        try {
            EventBus.getDefault().post(new LastLocationReceived(LocationServices.FusedLocationApi.getLastLocation(client)));
            NavigationApplication.globalLocation = LocationServices.FusedLocationApi.getLastLocation(client);
            if (NavigationApplication.DEBUG) {
                Log.d(TAG, "getLastLocation: Got last location from Main Activity!");
            }
            EventBus.getDefault().post(new PermissionGranted());
            client.disconnect();
        } catch (SecurityException se) {
            if (NavigationApplication.DEBUG) {
                se.printStackTrace();
            }
        } catch (Exception e) {
            if (NavigationApplication.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    private void promtToClose() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                .setTitle(getString(R.string.required_permission_title))
                .setMessage(getString(R.string.required_permission_explanation))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
        builder.show();


    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onConnected: Main Activity: Connection failed.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLastLocation();
                } else {
                    promtToClose();
                }

                break;
        }


    }

    public class PermissionGranted {
        public boolean b;

        public PermissionGranted() {
            this.b = true;
        }

    }

    public class LastLocationReceived {
        public Location location;

        public LastLocationReceived(Location location) {
            this.location = location;
        }

    }

    public class PlaceReceived {
        public Place place;

        public PlaceReceived(Place place) {
            this.place = place;
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
