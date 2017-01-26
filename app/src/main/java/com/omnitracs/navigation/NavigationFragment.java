
package com.omnitracs.navigation;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NavigationFragment extends Fragment implements OnMapReadyCallback {

    private static final int MY_PERMISSION_LOCATION = 232;
    private static final String TAG = "NavigationFragment";
    private MapView mapView;
    private GoogleMap map;
    private Marker desiredPlace;
    private TextView distance;
    private TextView textData;
    private Location currentLocation;
    private Toast infoToast;
    private LinearLayout waitLayout;

    public NavigationFragment() {
        // Required empty public constructor
    }

    public static NavigationFragment newInstance() {
        NavigationFragment fragment = new NavigationFragment();
        Bundle args = new Bundle();
        //some args here
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //some params
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView = (MapView) view.findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        distance = (TextView) view.findViewById(R.id.distance);
        textData = (TextView) view.findViewById(R.id.text_data);
        waitLayout = (LinearLayout) view.findViewById(R.id.wait_layout);

        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onCreateView: Getting the map!");
        }

        mapView.getMapAsync(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_navigation, container, false);
        return view;
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        mapView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onMapReady: map is ready");
        }
        map.setBuildingsEnabled(false);

        setLocationEnabled();

        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                distance.setText("");

                new GetAddressTask().execute(latLng.latitude, latLng.longitude);
            }
        });

        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                currentLocation = NavigationApplication.globalLocation;
                checkForCurrentPlace();
            }
        });
    }

    private void checkForCurrentPlace() {

        List<Place> placesList = new ArrayList<>();

        try {
            placesList = Place.listAll(Place.class);
        } catch (Exception e) {
            if (NavigationApplication.DEBUG) {
                e.printStackTrace();
            }
        }

        if (placesList.size() > 0) {
            setMarker(placesList.get(0));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onRequestPermissionsResult: Launching On Request Permissions Result");
        }
        switch (requestCode) {
            case MY_PERMISSION_LOCATION: {

                if (NavigationApplication.DEBUG) {
                    Log.d(TAG, "onRequestPermissionsResult: Permission case!");
                }

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setLocationEnabled();
                    //callback.onPermissionGranted(true);
                } else {
                    //permission denied! too bad, don't do the thing and inform user
                    Toast.makeText(getActivity(), getString(R.string.location_needed), Toast.LENGTH_LONG).show();
                }
            }
            return;
        }

    }

    private void setLocationEnabled() {
        try {
            map.setMyLocationEnabled(true);
            //getActivity().startService(new Intent(getActivity(), LocationService.class));
        } catch (SecurityException e) {
            if (NavigationApplication.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    public void setCameraSimpleToLocation(Location location) {
        CameraPosition position = new CameraPosition.Builder()
                .target(new LatLng(location.getLatitude(), location.getLongitude())) // Sets the new camera position
                .zoom(150) // Sets the zoom
                //.bearing(180) // Rotate the camera
                //.tilt(90) // Set the camera tilt
                .build(); // Creates a CameraPosition from the builder

        map.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000, null);

        setLocationEnabled();
    }

    private void setMarker(Place place) {
        MarkerOptions marker = new MarkerOptions()
                .position(new LatLng(place.lat, place.lng))
                .title(place.name);

        desiredPlace = map.addMarker(marker);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(desiredPlace.getPosition());
        if (currentLocation != null) {
            builder.include(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        }

        LatLngBounds bounds = builder.build();


        Location markerLocation = new Location(LocationManager.GPS_PROVIDER);
        markerLocation.setLatitude(place.lat);
        markerLocation.setLongitude(place.lng);

        try {
            showToast(getSimpleDistanceString(NavigationApplication.globalLocation, markerLocation));
        } catch (Exception e) {
            //might be null
            if (NavigationApplication.DEBUG) {
                e.printStackTrace();
            }
        }

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 180);
        map.animateCamera(cu);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    private void showUIinfo(Location loc, String mDistance, String unity) {

        //first info frame
        String data = "My coordinates: " + loc.getLatitude() + " , " + loc.getLongitude();
        textData.setVisibility(View.VISIBLE);
        textData.setText(data);

        //second info frame
        distance.setVisibility(View.VISIBLE);
        distance.setText("Estás a " + mDistance + " " + unity + " de tu destino");

        if (Float.parseFloat(mDistance) < 100) {
            distance.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.holo_red_light));
        } else {
            distance.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.holo_blue_dark));
        }

    }

    void showToast(String text) {
        if (infoToast != null) {
            infoToast.cancel();
        }
        infoToast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
        infoToast.show();

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServiceStopped(LocationService.ServiceStopped stopped) {
        if (stopped.stopped) {
            textData.setVisibility(View.GONE);
            distance.setVisibility(View.GONE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLocationEvent(LocationService.LocationEvent event) {


        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onLocationEvent: Location catched in fragment!");
            Log.d(TAG, "onLocationChanged: Location changed: " + event.location.getLatitude() + " " + event.location.getLongitude());
            Log.d(TAG, "onLocationEvent: Distance saved: " + Utils.getDistance(getActivity()) + " Current Distance: " + Float.parseFloat(event.distance));
        }

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(desiredPlace.getPosition());
        if (currentLocation != null) {
            builder.include(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
        }
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(builder.build(), 180);
        map.animateCamera(cu);


        currentLocation = event.location;
        showUIinfo(event.location, event.distance, event.unity);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPermissionGranted(MainActivity.PermissionGranted permission) {

        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onPermissionGranted: Permission granted from main  activity!");
        }
        if (permission.b) {
            setLocationEnabled();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaceReceived(MainActivity.PlaceReceived placeReceived) {
        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onPlaceReceived: We have received a place: " + placeReceived.place.name);
        }

        distance.setText("");
        if (desiredPlace != null) {
            desiredPlace.remove();
        }

        try {
            Place.deleteAll(Place.class);
        } catch (Exception e) {
            if (NavigationApplication.DEBUG) {
                //might not exist
                e.printStackTrace();
            }
        }

        Location markerLocation = new Location(LocationManager.GPS_PROVIDER);
        markerLocation.setLatitude(placeReceived.place.lat);
        markerLocation.setLongitude(placeReceived.place.lng);

        Place place = new Place(placeReceived.place.name, markerLocation.getLatitude(), markerLocation.getLongitude());
        place.save();

        setMarker(place);

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLastLocationReceived(MainActivity.LastLocationReceived lastLocationReceived) {

        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onLastLocationReceived: Location received from main activity");
        }

        currentLocation = lastLocationReceived.location;
        setCameraSimpleToLocation(lastLocationReceived.location);
    }

    private String getSimpleDistanceString(Location l1, Location l2) {
        float rawDistance = l1.distanceTo(l2);
        float kmOrMtsDistance = rawDistance > 999.9 ? rawDistance / 1000 : rawDistance;
        String unity = rawDistance > 999.9 ? "kilómetros" : "metros";
        String formattedOutput = String.format(rawDistance > 999.9 ? "%.1f" : "%.0f", kmOrMtsDistance);
        return formattedOutput + " " + unity;
    }

    private void getAddress(double lat, double lng) {
    }

    private class GetAddressTask extends AsyncTask<Double, Void, Void> {

        Address obj;
        double providedLat;
        double providedLng;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            waitLayout.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            waitLayout.setVisibility(View.GONE);
            if (NavigationApplication.DEBUG) {
                Log.d(TAG, "onPostExecute: Task completed: ");
            }

            if (obj != null) {
                if (NavigationApplication.DEBUG) {
                    Log.d(TAG, "onPostExecute: " + obj.getAddressLine(0));
                    Log.d(TAG, "onPostExecute: " + obj.getLatitude());
                    Log.d(TAG, "onPostExecute: " + obj.getLongitude());
                    Log.d(TAG, "onPostExecute: " + providedLat);
                    Log.d(TAG, "onPostExecute: " + providedLng);
                }
                createMarkerWithName(obj.getAddressLine(0), providedLat, providedLng);
            } else {
                //likely an error fetching data. Show it as offline
                Toast.makeText(getActivity(), getString(R.string.offline_toast), Toast.LENGTH_SHORT).show();
                createMarkerWithName(getString(R.string.offline_tag), providedLat, providedLng);
            }

        }

        @Override
        protected Void doInBackground(Double... params) {

            if (NavigationApplication.DEBUG) {
                Log.d(TAG, "doInBackground: Executing geocoder in BG");
            }

            Geocoder geocoder = new Geocoder(getActivity().getApplicationContext(), Locale.getDefault());
            providedLat = params[0];
            providedLng = params[1];

            try {
                List<Address> addresses = geocoder.getFromLocation(providedLat, providedLng, 1);
                obj = addresses.get(0);
            } catch (IOException e) {
                //likely no internet connection
                if (NavigationApplication.DEBUG) {
                    e.printStackTrace();
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        waitLayout.setVisibility(View.GONE);
                        Toast.makeText(getActivity().getApplicationContext(), "any mesage", Toast.LENGTH_LONG).show();
                    }
                });
                Toast.makeText(getActivity(), getString(R.string.offline_toast), Toast.LENGTH_SHORT).show();
                createMarkerWithName(getString(R.string.offline_tag), providedLat, providedLng);
            }
            return null;
        }
    }

    private void createMarkerWithName(String name, double lat, double lng) {
        if (desiredPlace != null) {
            desiredPlace.remove();
        }

        try {
            Place.deleteAll(Place.class);
        } catch (Exception e) {
            if (NavigationApplication.DEBUG) {
                //might not exist
                e.printStackTrace();
            }
        }

        Location markerLocation = new Location(LocationManager.GPS_PROVIDER);
        markerLocation.setLatitude(lat);
        markerLocation.setLongitude(lat);

        Place place = new Place(name, lat, lng);
        place.save();

        setMarker(place);
    }


    private float getRawDistance(Location l1, Location l2) {
        return l1.distanceTo(l2);
    }


}

