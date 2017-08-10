package com.syzible.loinnir.network;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by ed on 16/12/2016
 */

public class PostJSONArray extends PostRequest<JSONArray> {
    public PostJSONArray(NetworkCallback<JSONArray> networkCallback, JSONArray payload, String url, boolean isExternal) {
        super(networkCallback, payload, url, isExternal);
    }

    @Override
    public JSONArray transferData() {
        return null;
    }

    @Override
    public JSONArray transferData(JSONArray payload) {
        try {

            switch (getConnection().getResponseCode()) {
                case 200:
                case 304:
                    Writer writer = new BufferedWriter(new OutputStreamWriter(getConnection().getOutputStream()));
                    writer.write(payload.toString());
                    writer.close();

                    InputStream inputStream = getConnection().getInputStream();
                    StringBuilder buffer = new StringBuilder();
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String inputLine;
                    while ((inputLine = bufferedReader.readLine()) != null) {
                        buffer.append(inputLine).append("\n");
                    }

                    return new JSONArray(buffer.toString());
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
}
