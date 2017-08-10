package com.syzible.loinnir.network;

import org.json.JSONArray;
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

public class GetJSONArray extends GetRequest<JSONArray> {
    public GetJSONArray(NetworkCallback<JSONArray> networkCallback, String url, boolean isExternal) {
        super(networkCallback, url, isExternal);
    }

    @Override
    public JSONArray transferData() {
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

                    return new JSONArray(writer.toString());
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
    public JSONArray transferData(JSONArray payload) {
        return null;
    }
}
