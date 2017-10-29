package com.syzible.loinnir.network;

import android.app.Activity;
import android.content.Context;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.utils.JSONUtils;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 29/10/2017.
 */

public class MetaDataUpdate {

    public static void updateLastActive(Context context) {
        RestClient.post(context, Endpoints.UPDATE_USER_META_DATA, JSONUtils.getIdPayload(context),
                new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {

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
