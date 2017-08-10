package com.syzible.loinnir.network;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by ed on 16/12/2016
 */

abstract class Request<T> extends AsyncTask<Object, Void, T> {
    private NetworkCallback<T> networkCallback;
    private String url, verb;
    private HttpURLConnection connection;
    private T payload;

    Request(NetworkCallback<T> networkCallback, String url, String verb, boolean isExternal) {
        this(networkCallback, null, url, verb, isExternal);
    }

    Request(NetworkCallback<T> networkCallback, T payload, String url, String verb, boolean isExternal) {
        this.networkCallback = networkCallback;
        this.url = isExternal ? url : Endpoints.getApiURL(url);
        this.payload = payload;
        this.verb = verb;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Void... progress) {
        super.onProgressUpdate(progress);
    }

    @Override
    protected T doInBackground(Object... objects) {
        try {
            System.out.println(url);
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);
            connection.setRequestMethod(verb);
            connection.setInstanceFollowRedirects(true);
            if (verb.equals("POST")) setPostContent();
            connection.setUseCaches(false);
            connection.setAllowUserInteraction(false);
            connection.connect();

            if (verb.equals("POST")) return transferData(payload);

            return transferData();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(T o) {
        super.onPostExecute(o);
        assert networkCallback != null;
        if (o != null)
            networkCallback.onResponse(o);
        else
            networkCallback.onFailure();
    }

    HttpURLConnection getConnection() {
        return connection;
    }

    private void setPostContent() {
        getConnection().setRequestProperty("Content-Type", "application/json");
        getConnection().setRequestProperty("Accept", "application/json");
    }

    public abstract T transferData();

    public abstract T transferData(T payload);
}
