package com.syzible.loinnir.objects;

import com.google.android.gms.maps.model.LatLng;
import com.stfalcon.chatkit.commons.models.IUser;
import com.syzible.loinnir.utils.EncodingUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ed on 07/05/2017.
 */

public class User implements IUser {

    private String fb_id;
    private float longitude, latitude;
    private LatLng location;
    private String forename;
    private String surname;
    private String avatar;
    private String locality, county;
    private boolean isFemale;
    private long lastActive;

    public User(String fb_id) {
        this.fb_id = fb_id;
    }

    public User(JSONObject data) throws JSONException {
        this.fb_id = data.getString("fb_id");
        this.longitude = (float) data.getDouble("lng");
        this.latitude = (float) data.getDouble("lat");
        this.location = new LatLng(latitude, longitude);
        this.forename = EncodingUtils.decodeText(data.getString("forename"));
        this.surname = EncodingUtils.decodeText(data.getString("surname"));
        this.isFemale = data.getString("gender").equals("female");
        this.avatar = data.getString("profile_pic");
        this.lastActive = data.getLong("last_active");

        if (data.getString("locality").equals("abroad"))
            this.locality = "Thar Sáile";
        else
            this.locality = data.getString("locality");

        if (data.getString("county").equals("abroad"))
            this.county = "Éire";
        else
            this.county = data.getString("county");
    }

    @Override
    public String getId() {
        return fb_id;
    }

    @Override
    public String getName() {
        return forename + " " + surname;
    }

    @Override
    public String getAvatar() {
        return avatar;
    }

    public float getLongitude() {
        return longitude;
    }

    public float getLatitude() {
        return latitude;
    }

    public LatLng getLocation() {
        return location;
    }

    public String getLocality() {
        return locality;
    }

    public String getCounty() {
        return county;
    }

    public String getForename() {
        return forename;
    }

    public String getSurname() {
        return surname;
    }

    public boolean isFemale() {
        return isFemale;
    }

    public long getLastActive() {
        return lastActive;
    }

    public void setLastActive(long lastActive) {
        this.lastActive = lastActive;
    }
}
