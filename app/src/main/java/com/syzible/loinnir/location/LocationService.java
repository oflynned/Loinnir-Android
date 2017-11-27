package com.syzible.loinnir.location;

import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.syzible.loinnir.utils.BroadcastFilters;
import com.yayandroid.locationmanager.base.LocationBaseService;
import com.yayandroid.locationmanager.configuration.LocationConfiguration;
import com.yayandroid.locationmanager.constants.FailType;
import com.yayandroid.locationmanager.constants.ProcessType;

/**
 * Created by ed on 27/11/2017.
 */

public class LocationService extends LocationBaseService {
    private boolean isLocationRequested = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public LocationConfiguration getLocationConfiguration() {
        return LocationConfig.silentConfiguration();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (!isLocationRequested) {
            isLocationRequested = true;
            getLocation();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onLocationChanged(Location location) {
        System.out.println(location.getLatitude() + ", " + location.getLongitude());
        LocationUtils.syncWithServer(location, getApplicationContext());

        Intent intent = new Intent(BroadcastFilters.updated_location.toString());
        intent.putExtra("lat", String.valueOf(location.getLatitude()));
        intent.putExtra("lng", String.valueOf(location.getLongitude()));
        sendBroadcast(intent);
    }

    @Override
    public void onLocationFailed(@FailType int type) {
        stopSelf();
    }

    @Override
    public void onProcessTypeChanged(@ProcessType int processType) {

    }
}
