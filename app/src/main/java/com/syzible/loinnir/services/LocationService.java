package com.syzible.loinnir.services;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.persistence.Constants;

/**
 * Created by ed on 30/05/2017.
 */

public class LocationService extends Service {

    private static final String PACKAGE_NAME = "com.syzible.loinnir.locationservice";
    private static final String TAG = LocationService.class.getSimpleName();
    public static final String BROADCAST_ACTION = PACKAGE_NAME + ".broadcast";
    public static final String BROADCAST_LOCATION = PACKAGE_NAME + ".location";

    private final IBinder binder = new LocalBinder();

    public static final LatLng ATHLONE = new LatLng(53.4232575, -7.9402598);
    public static final float INITIAL_LOCATION_ZOOM = 6.0f;
    public static final float MY_LOCATION_ZOOM = 14.0f;
    public static final int USER_LOCATION_RADIUS = 500;

    private static final int LOCATION_FOREGROUND_INTERVAL = Endpoints.isDebugMode() ? Constants.ONE_SECOND : Constants.ONE_MINUTE;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = LOCATION_FOREGROUND_INTERVAL / 2;

    private LocationRequest locationRequest;
    private FusedLocationProviderClient mFusedLocationClient;

    private LocationCallback mLocationCallback;
    private Handler mServiceHandler;

    private boolean mChangingConfiguration = false;

    private Location mLocation;
    public LocationService() {
    }

    @Override
    public void onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!MainActivity.isActivityVisible()) {
            removeLocationUpdates();
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration = false;
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (!mChangingConfiguration && LocationUtils.requestingLocationUpdates(this)) {
            Log.i(TAG, "Starting foreground service");
            /*
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
                mNotificationManager.startServiceInForeground(new Intent(this,
                        LocationUpdatesService.class), NOTIFICATION_ID, getNotification());
            } else {
                startForeground(NOTIFICATION_ID, getNotification());
            }*/
        }
        return true;
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    public void requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates");
        LocationUtils.setRequestingLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), LocationService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(locationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            LocationUtils.setRequestingLocationUpdates(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            LocationUtils.setRequestingLocationUpdates(this, false);
            stopSelf();
        } catch (SecurityException unlikely) {
            LocationUtils.setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            mLocation = task.getResult();
                        } else {
                            Log.w(TAG, "Failed to get location.");
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);

        mLocation = location;

        Intent intent = new Intent(BROADCAST_ACTION);
        intent.putExtra(BROADCAST_LOCATION, location);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        // Update notification content if running as a foreground service.
        if (serviceIsRunningInForeground(this)) {
            // TODO should broadcast new location and update server location value

        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(LOCATION_FOREGROUND_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public class LocalBinder extends Binder {
        public LocationService getService() {
            return LocationService.this;
        }
    }

    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }
}