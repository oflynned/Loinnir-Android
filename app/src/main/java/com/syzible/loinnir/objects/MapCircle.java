package com.syzible.loinnir.objects;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by ed on 15/07/2017.
 */

public class MapCircle implements ClusterItem {

    private User user;
    private boolean isMe;
    private LatLng location;

    public MapCircle(User user, boolean isMe) {
        this.user = user;
        this.isMe = isMe;
    }

    public MapCircle(LatLng location) {
        this.location = location;
    }

    public LatLng getLocation() {
        return location;
    }

    public User getUser() {
        return user;
    }

    public boolean isMe() {
        return isMe;
    }

    @Override
    public LatLng getPosition() {
        return new LatLng(user.getLatitude(), user.getLongitude());
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getSnippet() {
        return null;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public float distanceTo(LatLng point) {
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(point.latitude - user.getLatitude());
        double lngDiff = Math.toRadians(point.longitude - user.getLongitude());
        double a = Math.sin(latDiff / 2) * Math.sin(latDiff / 2) +
                Math.cos(Math.toRadians(user.getLatitude())) * Math.cos(Math.toRadians(point.latitude)) *
                        Math.sin(lngDiff / 2) * Math.sin(lngDiff / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;

        int meterConversion = 1609;

        return (float) (distance * meterConversion);
    }

    @Override
    public String toString() {
        return user.getForename() + ": (" + user.getLatitude() + ", " + user.getLongitude() + ")";
    }
}
