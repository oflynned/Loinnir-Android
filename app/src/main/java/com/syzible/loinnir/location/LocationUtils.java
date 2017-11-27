/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.syzible.loinnir.location;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.maps.model.LatLng;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.persistence.LocalPrefs;
import com.syzible.loinnir.utils.BroadcastFilters;
import com.yayandroid.locationmanager.LocationManager;
import com.yayandroid.locationmanager.listener.LocationListener;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class LocationUtils {
    public static final LatLng ATHLONE = new LatLng(53.4232575, -7.9402598);
    public static final float INITIAL_LOCATION_ZOOM = 6.0f;
    public static final float MY_LOCATION_ZOOM = 14.0f;
    public static final int USER_LOCATION_RADIUS = 500;

    public LocationManager initialiseLocationManager(Context context, Activity activity) {
        return new LocationManager.Builder(context)
                .activity(activity)
                .configuration(LocationConfig.silentConfiguration())
                .notify(new LocationListener() {
                    @Override
                    public void onProcessTypeChanged(int processType) {

                    }

                    @Override
                    public void onLocationChanged(Location location) {
                        Intent intent = new Intent(BroadcastFilters.updated_location.toString());
                        intent.putExtra("lat", String.valueOf(location.getLatitude()));
                        intent.putExtra("lng", String.valueOf(location.getLongitude()));
                        activity.sendBroadcast(intent);

                        syncWithServer(location, activity);
                    }

                    @Override
                    public void onLocationFailed(int type) {

                    }

                    @Override
                    public void onPermissionGranted(boolean alreadyHadPermission) {

                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                })
                .build();
    }

    private void syncWithServer(Location location, Context context) {
        if (!LocalPrefs.getID(context).equals("")) {
            JSONObject payload = new JSONObject();

            try {
                payload.put("fb_id", LocalPrefs.getID(context));
                payload.put("lng", location.getLongitude());
                payload.put("lat", location.getLatitude());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // don't care about response on user side
            RestClient.post(context, Endpoints.UPDATE_USER_LOCATION, payload, new BaseJsonHttpResponseHandler<JSONObject>() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                    try {
                        String currentLocality = response.getJSONObject("user").getString("locality");
                        if (!LocalPrefs.getStringPref(LocalPrefs.Pref.last_known_location, context).equals(currentLocality))
                            context.sendBroadcast(new Intent(BroadcastFilters.changed_locality.toString()));

                        LocalPrefs.setStringPref(LocalPrefs.Pref.last_known_location, currentLocality, context);

                        if (LocalPrefs.getBooleanPref(LocalPrefs.Pref.should_share_location, context))
                            context.sendBroadcast(new Intent(BroadcastFilters.updated_location.toString()));
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
}
