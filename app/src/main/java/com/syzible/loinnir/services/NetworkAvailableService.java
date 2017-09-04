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
import com.syzible.loinnir.utils.BroadcastFilters;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 04/09/2017.
 */

public class NetworkAvailableService {
    public static boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager
                .getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());
    }

    public static void syncCachedData(final Context context) {
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                System.out.println(LocalCacheDatabaseHelper.getCachedMessagesSize(context));

                if (LocalCacheDatabaseHelper.getCachedMessagesSize(context) > 0) {
                    JSONObject o = LocalCacheDatabaseHelper.getCachedItem(context);
                    System.out.println(o);
                    try {
                        boolean isLocality = o.getBoolean("is_locality");
                        final String id = o.getString("local_id");
                        JSONObject data = o.getJSONObject("data");

                        String endpoint = isLocality ? Endpoints.SEND_LOCALITY_MESSAGE : Endpoints.SEND_PARTNER_MESSAGE;
                        RestClient.post(context, endpoint, data, new BaseJsonHttpResponseHandler<JSONObject>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                // force the locality page to update if cached items get sent
                                LocalCacheDatabaseHelper.removeCachedItem(context, id);
                                context.sendBroadcast(new Intent(BroadcastFilters.changed_locality.toString()));
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                            }

                            @Override
                            protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                System.out.println(rawJsonData);
                                return new JSONObject(rawJsonData);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    handler.postDelayed(this, 1000);
                }

                if (!isInternetAvailable(context))
                    handler.removeCallbacks(this);
            }
        };

        handler.postDelayed(runnable, 1000);
    }
}
