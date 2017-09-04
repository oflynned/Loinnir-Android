package com.syzible.loinnir.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.persistence.LocalCacheDatabaseHelper;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 04/09/2017.
 */

public class NetworkAvailableService extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                // query and sync cached messages with the server
                syncWithServer(context);
            }
        }
    }

    private void syncWithServer(final Context context) {
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                LocalCacheDatabaseHelper.printCachedItemsContents(context);

                if (LocalCacheDatabaseHelper.getCachedMessagesSize(context) > 0) {
                    JSONObject o = LocalCacheDatabaseHelper.getCachedItem(context);
                    try {
                        boolean isLocality = o.getBoolean("is_locality");
                        final String id = o.getString("local_id");

                        JSONObject data = o.getJSONObject("data");
                        String endpoint = isLocality ? Endpoints.SEND_LOCALITY_MESSAGE : Endpoints.SEND_PARTNER_MESSAGE;
                        RestClient.post(context, endpoint, data, new BaseJsonHttpResponseHandler<JSONObject>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                LocalCacheDatabaseHelper.removeCachedItem(context, id);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                            }

                            @Override
                            protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                return new JSONObject(rawJsonData);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    handler.postDelayed(this, 1000);
                } else {
                    handler.removeCallbacks(this);
                }
            }
        };

        handler.postDelayed(runnable, 1000);
    }
}
