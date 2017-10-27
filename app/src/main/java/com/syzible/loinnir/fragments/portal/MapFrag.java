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
import android.support.design.widget.FloatingActionButton;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.android.gms.maps.model.VisibleRegion;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.MapCircle;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.services.LocationService;
import com.syzible.loinnir.utils.BroadcastFilters;
import com.syzible.loinnir.utils.JSONUtils;
import com.syzible.loinnir.persistence.LocalPrefs;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 07/05/2017.
 */

public class MapFrag extends Fragment implements OnMapReadyCallback {
    private GoogleMap googleMap;
    private HeatmapTileProvider provider;

    private BroadcastReceiver locationUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), BroadcastFilters.updated_location.toString()))
                getWebServerLocation();
        }
    };

    private LatLng lastKnownLocation;
    private boolean hasZoomed = false;
    private ArrayList<MapCircle> userCircles = new ArrayList<>();
    private int GREEN_500, AMBER_500;

    private static final int MIN_GROUP_SIZE_TOLERANCE = 5;
    private static final float GROUP_DISTANCE_TOLERANCE = 500; //m
    private static final float MAP_TOLERANCE = 500; //m
    private static final int MAP_UPDATE_THRESHOLD = 500; //ms
    private long lastMapUpdateCall = Long.MIN_VALUE;


    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(locationUpdateReceiver,
                new IntentFilter(BroadcastFilters.updated_location.toString()));

        hasZoomed = false;
        GREEN_500 = ContextCompat.getColor(getActivity(), R.color.green500);
        AMBER_500 = ContextCompat.getColor(getActivity(), R.color.amber500);
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

    public float distanceTo(LatLng p1, LatLng p2) {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(p2.latitude - p1.latitude);
        double lngDiff = Math.toRadians(p2.longitude - p1.longitude);
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(p1.latitude)) * Math.cos(Math.toRadians(p2.latitude)) *
                        Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return (float) (distance * meterConversion);
    }

    private LatLng getAverageLocation(HashSet<MapCircle> list) {
        double aggregateLat = 0, aggregateLng = 0;
        for (MapCircle overlappingCircle : list) {
            aggregateLat += overlappingCircle.getPosition().latitude;
            aggregateLng += overlappingCircle.getPosition().longitude;
        }

        aggregateLat /= list.size();
        aggregateLng /= list.size();

        return new LatLng(aggregateLat, aggregateLng);
    }

    private void getCirclesWithinTolerance() {
        ArrayList<HashSet<MapCircle>> groomedCircles = new ArrayList<>();
        ArrayList<MapCircle> nonOverlappingUsers = new ArrayList<>();
        HashSet<MapCircle> items = new HashSet<>();

        boolean wasAdded = false;

        // restrict first to circles around dublin for the example to restrict loads
        for (int i = 0; i < userCircles.size(); i++) {
            if (userCircles.get(i).getUser().getCounty().equals("Áth Cliath")) {
                MapCircle focusedCircle = userCircles.get(i);
                for (int j = i + 1; j < userCircles.size(); j++) {
                    if (i == userCircles.size() - 1)
                        j = 0;

                    if (userCircles.get(j).getUser().getCounty().equals("Áth Cliath")) {
                        MapCircle testingCircle = userCircles.get(j);
                        double distance = focusedCircle.distanceTo(testingCircle.getPosition());
                        if (distance < GROUP_DISTANCE_TOLERANCE) {
                            wasAdded = true;
                            items.add(userCircles.get(j));
                        }
                    }
                }

                items.add(userCircles.get(i));
                groomedCircles.add(items);
                items = new HashSet<>();

                if (!wasAdded)
                    nonOverlappingUsers.add(userCircles.get(i));

                wasAdded = false;
            }
        }

        ArrayList<HashSet<MapCircle>> overlappingCirclesGroups = new ArrayList<>();

        // now reduce the sets into cardinal sets by reducing into set groups of people in a certain distance from each other
        for (int i = 0; i < groomedCircles.size(); i++) {
            HashSet<MapCircle> firstSet = groomedCircles.get(i);
            for (int j = 1; j < groomedCircles.size(); j++) {
                if (i == userCircles.size() - 1)
                    j = 0;

                HashSet<MapCircle> secondSet = groomedCircles.get(j);

                if (firstSet.size() > MIN_GROUP_SIZE_TOLERANCE && secondSet.size() > MIN_GROUP_SIZE_TOLERANCE) {
                    if (firstSet.containsAll(secondSet) && secondSet.containsAll(firstSet)) {
                        if (!overlappingCirclesGroups.contains(firstSet)) {
                            System.out.println("set: " + firstSet);
                            overlappingCirclesGroups.add(firstSet);
                        }
                    }
                }
            }
        }

        ArrayList<MapCircle> overlappingCircles = new ArrayList<>();

        for (HashSet<MapCircle> overlappingLocality : overlappingCirclesGroups) {
            LatLng location = getAverageLocation(overlappingLocality);
            //System.out.println(location);
            overlappingCircles.add(new MapCircle(location));
        }

        // now we have the population groups, we should aggregate over them to find the ones
        // within a tolerance to each to each other and calculate a new average for the centre
        // of the group's shape
        ArrayList<ArrayList<MapCircle>> output = new ArrayList<>();
        boolean wasGroupAdded = false;

        for (int i = 0; i < overlappingCircles.size(); i++) {
            ArrayList<MapCircle> groupsPertaining = new ArrayList<>();
            groupsPertaining.add(overlappingCircles.get(i));
            for (int j = 1; j < overlappingCircles.size(); j++) {
                if (j == overlappingCircles.size())
                    j = 0;

                if (distanceTo(overlappingCircles.get(i).getGroupLocation(), overlappingCircles.get(j).getGroupLocation()) < GROUP_DISTANCE_TOLERANCE) {
                    groupsPertaining.add(overlappingCircles.get(j));
                    System.out.println("Adding ");
                }
            }

            output.add(groupsPertaining);
        }

        for (ArrayList<MapCircle> item : output)
            for (MapCircle circle : item)
                System.out.println(circle.getGroupLocation());

        // groom the original group to those that aren't in
        googleMap.clear();
        for (MapCircle mapCircle : nonOverlappingUsers) {
            //googleMap.addCircle(getUserCircle(mapCircle.getPosition()));
        }

        // now re-add the groups reduced to a giant group cardinality
        //for (MapCircle group : output) {
        //    googleMap.addCircle(getUserCircle(group.getGroupLocation(), MAP_TOLERANCE));
        //}
    }

    private void drawVisibleCircles() {
        Runnable zoomToLocationRunnable = () -> {
            VisibleRegion region = googleMap.getProjection().getVisibleRegion();
            LatLng nw = getAdjustedCoord(region.latLngBounds.northeast, MAP_TOLERANCE, MAP_TOLERANCE);
            LatLng se = getAdjustedCoord(region.latLngBounds.southwest, -MAP_TOLERANCE, -MAP_TOLERANCE);

            LatLngBounds bounds = new LatLngBounds(se, nw);
            googleMap.clear();

            for (MapCircle circle : userCircles) {
                if (bounds.contains(circle.getUser().getLocation()))
                    googleMap.addCircle(getUserCircle(circle.getPosition()));
            }
        };

        new Handler().postDelayed(zoomToLocationRunnable, 0);
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

        FloatingActionButton focusLocation = (FloatingActionButton) view.findViewById(R.id.focus_gps_fab);
        focusLocation.setOnClickListener(v -> {
            if (googleMap != null) {
                if (LocalPrefs.getBooleanPref(LocalPrefs.Pref.should_share_location, getActivity())) {
                    for (MapCircle circle : userCircles) {
                        if (circle.isMe()) {
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(circle.getUser().getLocation(), LocationService.MY_LOCATION_ZOOM));
                            break;
                        }
                    }
                } else {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LocationService.ATHLONE, LocationService.INITIAL_LOCATION_ZOOM));
                }
            }
        });

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setOnCameraChangeListener((i) -> {
            final long snap = System.currentTimeMillis();
            if (lastMapUpdateCall + MAP_UPDATE_THRESHOLD > snap) {
                lastMapUpdateCall = snap;
                return;
            }

            // on map moved actions

            //getCirclesWithinTolerance();
            drawVisibleCircles();

            lastMapUpdateCall = snap;
        });

        if (Endpoints.isDebugMode())
            this.googleMap.getUiSettings().setZoomControlsEnabled(true);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                LocationService.ATHLONE, LocationService.INITIAL_LOCATION_ZOOM));

        setMapPosition();
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

    private static LatLng getAdjustedCoord(LatLng coord, float dx, float dy) {
        float earthRadius = 6378137;
        double newLatitude = coord.latitude + (dy / earthRadius) * (180 / Math.PI);
        double newLongitude = coord.longitude + (dx / earthRadius) * (180 / Math.PI) /
                Math.cos(coord.latitude * Math.PI / 180);

        return new LatLng(newLatitude, newLongitude);
    }

    private void getWebServerLocation() {
        RestClient.post(getActivity(), Endpoints.GET_ALL_USERS, JSONUtils.getIdPayload(getActivity().getBaseContext()),
                new BaseJsonHttpResponseHandler<JSONArray>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, final JSONArray response) {

                        final Context context = MapFrag.this.getActivity();
                        final Handler loadCirclesHandler = new Handler();
                        final Runnable loadCirclesRunnable = () -> {
                            userCircles.clear();
                            googleMap.clear();
                            for (int i = 0; i < response.length(); i++) {
                                try {
                                    User user = new User(response.getJSONObject(i));
                                    if (context != null)
                                        userCircles.add(new MapCircle(user, user.getId().equals(LocalPrefs.getID(context))));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }


                            //getCirclesWithinTolerance();
                            drawVisibleCircles();
                        };

                        loadCirclesHandler.postDelayed(loadCirclesRunnable, 2500);

                        final Handler zoomToLocationHandler = new Handler();
                        final Runnable zoomToLocationRunnable = () -> {
                            for (int i = 0; i < response.length(); i++) {
                                try {
                                    User user = new User(response.getJSONObject(i));
                                    if (context != null) {
                                        if (user.getId().equals(LocalPrefs.getID(context))) {
                                            if (lastKnownLocation == null)
                                                lastKnownLocation = user.getLocation();

                                            if (!hasZoomed || hasUserMovedPositions(user)) {
                                                googleMap.moveCamera(CameraUpdateFactory.newLatLng(user.getLocation()));
                                                zoomToLocation(user.getLocation());
                                                hasZoomed = true;
                                                lastKnownLocation = user.getLocation();
                                            }
                                            break;
                                        }
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        };

                        zoomToLocationHandler.postDelayed(zoomToLocationRunnable, 0);

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
        new Handler(Looper.getMainLooper()).postDelayed(() -> googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location,
                LocationService.MY_LOCATION_ZOOM)), 1000);
    }

    private CircleOptions getUserCircle(LatLng position) {
        return new CircleOptions()
                .center(position)
                .radius(LocationService.USER_LOCATION_RADIUS)
                .strokeColor(getFillColour(true))
                .fillColor(getFillColour(false));
    }

    private CircleOptions getUserCircle(LatLng position, float radius) {
        return new CircleOptions()
                .center(position)
                .radius(LocationService.USER_LOCATION_RADIUS)
                .strokeColor(getGroupedColour(true))
                .fillColor(getGroupedColour(false));
    }

    private int getFillColour(boolean isClear) {
        int r = (GREEN_500) & 0xFF;
        int g = (GREEN_500 >> 8) & 0xFF;
        int b = (GREEN_500 >> 16) & 0xFF;
        int a = isClear ? 0 : 128;

        return Color.argb(a, r, g, b);
    }

    private int getGroupedColour(boolean isClear) {
        int r = (AMBER_500) & 0xFF;
        int g = (AMBER_500 >> 8) & 0xFF;
        int b = (AMBER_500 >> 16) & 0xFF;
        int a = isClear ? 0 : 128;

        return Color.argb(a, r, g, b);
    }
}
