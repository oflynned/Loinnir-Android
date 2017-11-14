package com.syzible.loinnir.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import com.google.android.gms.maps.model.LatLng;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.persistence.Constants;
import com.syzible.loinnir.persistence.LocalPrefs;
import com.syzible.loinnir.utils.BroadcastFilters;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 30/05/2017.
 */

public class LocationService extends Service {

    public static final LatLng ATHLONE = new LatLng(53.4232575, -7.9402598);
    public static final float INITIAL_LOCATION_ZOOM = 6.0f;
    public static final float MY_LOCATION_ZOOM = 14.0f;
    public static final int USER_LOCATION_RADIUS = 500;

    private static final int LOCATION_FOREGROUND_INTERVAL = Endpoints.isDebugMode() ? Constants.ONE_SECOND : Constants.FIVE_MINUTES;
    private static final float LOCATION_DISTANCE = 250f;

    private LocationManager locationManager = null;

    private class LocationListener implements android.location.LocationListener {

        private Location lastLocation;

        LocationListener(String provider) {
            this.lastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            if (LocalPrefs.getBooleanPref(LocalPrefs.Pref.should_share_location, getApplicationContext()))
                lastLocation.set(location);
            syncWithServer(location);
        }

        private void syncWithServer(Location location) {
            if (!LocalPrefs.getID(getApplicationContext()).equals("")) {
                JSONObject payload = new JSONObject();

                try {
                    payload.put("fb_id", LocalPrefs.getID(getApplicationContext()));
                    payload.put("lng", location.getLongitude());
                    payload.put("lat", location.getLatitude());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // don't care about response on user side
                RestClient.post(getApplicationContext(), Endpoints.UPDATE_USER_LOCATION, payload, new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                        try {
                            String currentLocality = response.getJSONObject("user").getString("locality");
                            if (!LocalPrefs.getStringPref(LocalPrefs.Pref.last_known_location, getApplicationContext()).equals(currentLocality))
                                getApplicationContext().sendBroadcast(new Intent(BroadcastFilters.changed_locality.toString()));

                            LocalPrefs.setStringPref(LocalPrefs.Pref.last_known_location, currentLocality, getApplicationContext());

                            if (LocalPrefs.getBooleanPref(LocalPrefs.Pref.should_share_location, getApplicationContext()))
                                getApplicationContext().sendBroadcast(new Intent(BroadcastFilters.updated_location.toString()));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                    }

                    @Override
                    protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONObject(rawJsonData);
                    }
                });
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            System.out.println("onStatusChanged: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            System.out.println("onProviderEnabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            System.out.println("onProviderDisabled: " + provider);
        }
    }

    LocationListener[] locationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //stopPollingLocation();
        startPollingLocation(LOCATION_FOREGROUND_INTERVAL);
        return START_STICKY;
    }

    private void startPollingLocation(int frequency) {
        initializeLocationManager();
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, frequency, LOCATION_DISTANCE,
                    locationListeners[1]);
        } catch (java.lang.SecurityException | IllegalArgumentException e) {
            e.printStackTrace();
        }

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, frequency, LOCATION_DISTANCE,
                    locationListeners[0]);
        } catch (java.lang.SecurityException | IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void stopPollingLocation() {
        if (locationManager != null) {
            for (LocationListener locationListener : locationListeners) {
                try {
                    locationManager.removeUpdates(locationListener);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPollingLocation();
        //startPollingLocation(LOCATION_BACKGROUND_INTERVAL);
    }

    private void initializeLocationManager() {
        if (locationManager == null) {
            locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }
}