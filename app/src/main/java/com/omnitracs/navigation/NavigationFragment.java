
package com.omnitracs.navigation;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
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

import java.util.ArrayList;
import java.util.List;

public class NavigationFragment extends Fragment implements OnMapReadyCallback {

    private static final int MY_PERMISSION_LOCATION = 232;
    private static final int PLACES_REQUEST = 323;
    private static final String TAG = "NavigationFragment";
    private MapView mapView;
    private GoogleMap map;
    private Marker desiredPlace;
    private boolean firstTime = true;
    private TextView distance;
    private TextView textData;
    private Location currentLocation;
    private Button searchButton;

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
        searchButton = (Button) view.findViewById(R.id.search_button);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(getActivity(), PlacesActivity.class), PLACES_REQUEST);
            }
        });

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

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {
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
                markerLocation.setLatitude(point.latitude);
                markerLocation.setLongitude(point.longitude);

                Place place = new Place("Test place", markerLocation.getLatitude(), markerLocation.getLongitude());
                place.save();

                MarkerOptions marker = new MarkerOptions()
                        .position(new LatLng(point.latitude, point.longitude))
                        .title("New Marker");

                desiredPlace = map.addMarker(marker);

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(desiredPlace.getPosition());
                if (currentLocation != null) {
                    builder.include(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
                }

                LatLngBounds bounds = builder.build();

                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 180);
                map.animateCamera(cu);

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
            MarkerOptions marker = new MarkerOptions()
                    .position(new LatLng(placesList.get(0).lat, placesList.get(0).lng))
                    .title("New Marker");

            desiredPlace = map.addMarker(marker);

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(desiredPlace.getPosition());
            if (currentLocation != null) {
                builder.include(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
            }

            LatLngBounds bounds = builder.build();

            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, 180);
            map.animateCamera(cu);
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

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
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
        if (firstTime) {
            setCameraSimpleToLocation(event.location);
            firstTime = false;
        }

        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onLocationEvent: Location catched in fragment!");
            Log.d(TAG, "onLocationChanged: Location changed: " + event.location.getLatitude() + " " + event.location.getLongitude());
        }

        currentLocation = event.location;

        if (desiredPlace != null) {
            Location imHere = event.location;
            Location wannaGo = new Location(LocationManager.GPS_PROVIDER);
            wannaGo.setLatitude(desiredPlace.getPosition().latitude);
            wannaGo.setLongitude(desiredPlace.getPosition().longitude);

            float dist = getDistance(imHere, wannaGo);

            float showDist = dist > 999.9 ? dist / 1000 : dist;
            String unity = dist > 999.9 ? "kilómetros" : "metros";


            if (NavigationApplication.DEBUG) {
                Log.d(TAG, "onLocationEvent: Distance is: " + dist);
            }

            distance.setVisibility(View.VISIBLE);
            distance.setText("Estás a " + showDist + " " + unity + " de tu destino");

            if (dist < 100) {
                distance.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.holo_red_light));
            } else {
                distance.setBackgroundColor(ContextCompat.getColor(getActivity(), android.R.color.holo_blue_dark));
            }

        }
    }

    public float getDistance(Location loc1, Location loc2) {

        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "getDistance: calculations! ");
        }

        String data = "My coordinates: " + loc1.getLatitude() + " , " + loc1.getLongitude() + " \n Place coordinates: " + loc2.getLatitude() + " , " + loc2.getLongitude();

        textData.setVisibility(View.VISIBLE);
        textData.setText(data);

        float distanceInMeters = loc1.distanceTo(loc2);

        return distanceInMeters;
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
    public void onLastLocationReceived(MainActivity.LastLocationReceived lastLocationReceived) {

        if (NavigationApplication.DEBUG) {
            Log.d(TAG, "onLastLocationReceived: Location received from main activity");
        }

        currentLocation = lastLocationReceived.location;
        setCameraSimpleToLocation(lastLocationReceived.location);
    }


}

