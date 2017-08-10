package com.syzible.loinnir.network;

import android.content.Context;
import android.os.Looper;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.SyncHttpClient;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by ed on 18/05/2017.
 */

public class RestClient {
    private static AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    private static SyncHttpClient syncHttpClient = new SyncHttpClient();

    private RestClient() {
    }

    public static void get(String url, AsyncHttpResponseHandler responseHandler) {
        getClient().get(Endpoints.getApiURL(url), null, responseHandler);
    }

    public static void getExternal(String url, AsyncHttpResponseHandler responseHandler) {
        getClient().setEnableRedirects(true);
        getClient().get(url, null, responseHandler);
    }

    public static void post(Context context, String url, JSONObject data, AsyncHttpResponseHandler responseHandler) {
        try {
            getClient().post(context, Endpoints.getApiURL(url), new StringEntity(data.toString()), "application/json", responseHandler);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public static void delete(Context context, String url, JSONObject data, AsyncHttpResponseHandler responseHandler) {
        try {
            getClient().delete(context, Endpoints.getApiURL(url), new StringEntity(data.toString()), "application/json", responseHandler);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    // prevents throwing synchronous looper exception in service thread
    private static AsyncHttpClient getClient() {
        if (Looper.myLooper() == null)
            return syncHttpClient;
        return asyncHttpClient;
    }
}
