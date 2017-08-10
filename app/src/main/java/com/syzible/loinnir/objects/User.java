package com.syzible.loinnir.objects;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.stfalcon.chatkit.commons.models.IUser;
import com.syzible.loinnir.utils.Constants;
import com.syzible.loinnir.utils.EncodingUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.CharsetDecoder;

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

    public User(JSONObject data) throws JSONException {
        this.fb_id = data.getString("fb_id");
        this.longitude = (float) data.getDouble("lng");
        this.latitude = (float) data.getDouble("lat");
        this.location = new LatLng(latitude, longitude);
        this.forename = EncodingUtils.decodeText(data.getString("forename"));
        this.surname = EncodingUtils.decodeText(data.getString("surname"));
        this.avatar = data.getString("profile_pic");
        this.locality = data.getString("locality");
        this.county = data.getString("county");
        this.isFemale = data.getString("gender").equals("female");
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
}
