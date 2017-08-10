package com.syzible.loinnir.services;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.utils.LocalStorage;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 26/05/2017.
 */

public class TokenService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        sendRegistrationToOwnServer(refreshedToken);
    }

    private void sendRegistrationToOwnServer(String token) {
        if (LocalStorage.isLoggedIn(getApplicationContext())) {
            JSONObject payload = new JSONObject();
            try {
                payload.put("fb_id", LocalStorage.getID(getApplicationContext()));
                payload.put("fcm_token", token);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RestClient.post(getApplicationContext(), Endpoints.EDIT_USER, payload, new BaseJsonHttpResponseHandler<JSONObject>() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                    System.out.println(rawJsonResponse);
                    System.out.println("Token refreshed successfully");
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                    System.out.println(rawJsonData);
                    System.out.println("Token refresh failed?");
                }

                @Override
                protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                    return new JSONObject(rawJsonData);
                }
            });
        }
    }
}
