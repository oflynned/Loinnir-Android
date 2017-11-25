package com.syzible.loinnir.location;

import android.support.annotation.NonNull;

import com.yayandroid.locationmanager.configuration.DefaultProviderConfiguration;
import com.yayandroid.locationmanager.configuration.GooglePlayServicesConfiguration;
import com.yayandroid.locationmanager.configuration.LocationConfiguration;
import com.yayandroid.locationmanager.configuration.PermissionConfiguration;

/**
 * Created by ed on 25/11/2017.
 */

public final class LocationConfig {
    private LocationConfig(){}

    public static LocationConfiguration silentConfiguration() {
        return silentConfiguration(true);
    }

    public static LocationConfiguration silentConfiguration(boolean keepTracking) {
        return new LocationConfiguration.Builder()
                .keepTracking(keepTracking)
                .useGooglePlayServices(new GooglePlayServicesConfiguration.Builder().askForSettingsApi(false).build())
                .useDefaultProviders(new DefaultProviderConfiguration.Builder().build())
                .build();
    }

    public static LocationConfiguration defaultConfiguration(@NonNull String rationalMessage, @NonNull String gpsMessage) {
        return new LocationConfiguration.Builder()
                .askForPermission(new PermissionConfiguration.Builder().rationaleMessage(rationalMessage).build())
                .useGooglePlayServices(new GooglePlayServicesConfiguration.Builder().build())
                .useDefaultProviders(new DefaultProviderConfiguration.Builder().gpsMessage(gpsMessage).build())
                .build();
    }

}
