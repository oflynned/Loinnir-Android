package com.syzible.loinnir.fragments.portal;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.ClusterRenderer;
import com.syzible.loinnir.objects.MapCircle;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.services.LocationService;
import com.syzible.loinnir.persistence.Constants;
import com.syzible.loinnir.utils.BroadcastFilters;
import com.syzible.loinnir.utils.JSONUtils;
import com.syzible.loinnir.persistence.LocalPrefs;
import com.syzible.loinnir.utils.MapCircleRenderer;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.regex.Matcher;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 07/05/2017.
 */

public class MapFrag extends Fragment implements OnMapReadyCallback {
    private GoogleMap googleMap;

    private BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BroadcastFilters.updated_location.toString()))
                getWebServerLocation();
        }
    };

    private LatLng lastKnownLocation;
    private boolean hasZoomed = false;
    private ArrayList<MapCircle> userCircles = new ArrayList<>();
    private int GREEN_500;

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(locationUpdateReceiver,
                new IntentFilter(BroadcastFilters.updated_location.toString()));

        hasZoomed = false;
        GREEN_500 = ContextCompat.getColor(getActivity(), R.color.green500);
        setMapPosition();

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
            actionBar.setSubtitle(null);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(locationUpdateReceiver);
    }

    private void setMapPosition() {
        if (googleMap != null) {
            if (LocalPrefs.getBooleanPref(LocalPrefs.Pref.should_share_location, getActivity())) {
                getWebServerLocation();
            } else {
                googleMap.clear();
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        LocationService.ATHLONE, LocationService.INITIAL_LOCATION_ZOOM));
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.map_frag, container, false);
        MapFragment mapFragment = (MapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        ClusterManager<MapCircle> clusterManager = new ClusterManager<>(getActivity(), this.googleMap);
        clusterManager.setRenderer(new MapCircleRenderer(getActivity(), this.googleMap, clusterManager));
        this.googleMap.setOnCameraIdleListener(clusterManager);

        if (Constants.DEV_MODE)
            this.googleMap.getUiSettings().setZoomControlsEnabled(true);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                LocationService.ATHLONE, LocationService.INITIAL_LOCATION_ZOOM));

        setMapPosition();
    }

    public static float coordinateDistanceToMeters(float lat1, float lng1, float lat2, float lng2) {
        double earthRadius = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        System.out.println((float) c);
        return (float) (earthRadius * c);
    }

    private boolean hasUserMovedPositions(User user) {
        Location userLocation = new Location("");
        userLocation.setLatitude(user.getLatitude());
        userLocation.setLongitude(user.getLongitude());

        Location oldLocation = new Location("");
        oldLocation.setLatitude(lastKnownLocation.latitude);
        oldLocation.setLongitude(lastKnownLocation.longitude);

        return userLocation.distanceTo(oldLocation) > 200;
    }

    private void getWebServerLocation() {
        RestClient.post(getActivity(), Endpoints.GET_ALL_USERS, JSONUtils.getIdPayload(getActivity().getBaseContext()),
                new BaseJsonHttpResponseHandler<JSONArray>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                        userCircles.clear();
                        googleMap.clear();
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                User user = new User(response.getJSONObject(i));
                                if (user.getId().equals(LocalPrefs.getID(getActivity()))) {
                                    userCircles.add(new MapCircle(user, true));
                                    if (lastKnownLocation == null)
                                        lastKnownLocation = user.getLocation();

                                    if (!hasZoomed || hasUserMovedPositions(user)) {
                                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(user.getLocation()));
                                        zoomToLocation(user.getLocation());
                                        hasZoomed = true;
                                        lastKnownLocation = user.getLocation();
                                    }

                                } else {
                                    userCircles.add(new MapCircle(user, false));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        googleMap.clear();
                        for (MapCircle circle : userCircles)
                            googleMap.addCircle(getUserCircle(circle.getPosition()));
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {

                    }

                    @Override
                    protected JSONArray parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONArray(rawJsonData);
                    }
                });
    }

    private void zoomToLocation(final LatLng location) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location,
                        LocationService.MY_LOCATION_ZOOM));
            }
        }, 1000);
    }

    private CircleOptions getUserCircle(LatLng position) {
        return new CircleOptions()
                .center(position)
                .radius(LocationService.USER_LOCATION_RADIUS)
                .strokeColor(getFillColour(true))
                .fillColor(getFillColour(false));
    }

    private int getFillColour(boolean isClear) {
        int r = (GREEN_500) & 0xFF;
        int g = (GREEN_500 >> 8) & 0xFF;
        int b = (GREEN_500 >> 16) & 0xFF;
        int a = isClear ? 0 : 128;

        return Color.argb(a, r, g, b);
    }
}
