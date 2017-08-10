package com.syzible.loinnir.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by ed on 16/12/2016
 */

public class GetJSONObject extends GetRequest<JSONObject> {
    public GetJSONObject(NetworkCallback<JSONObject> networkCallback, String url, boolean isExternal) {
        super(networkCallback, url, isExternal);
    }

    @Override
    public JSONObject transferData() {
        try {
            Writer writer = new StringWriter();

            switch (getConnection().getResponseCode()) {
                case 200:
                case 304:
                    char[] buffer = new char[1024];
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(getConnection().getInputStream()));
                    int n;
                    while ((n = br.read(buffer)) != -1) writer.write(buffer, 0, n);
                    br.close();

                    return new JSONObject(writer.toString());
                case 404:
                case 500:
                    break;
            }
            return null;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public JSONObject transferData(JSONObject payload) {
        return null;
    }
}
