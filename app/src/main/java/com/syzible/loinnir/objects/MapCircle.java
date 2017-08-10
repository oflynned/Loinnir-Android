package com.syzible.loinnir.objects;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

/**
 * Created by ed on 15/07/2017.
 */

public class MapCircle implements ClusterItem {

    private User user;
    private boolean isMe;

    public MapCircle(User user, boolean isMe) {
        this.user = user;
        this.isMe = isMe;
    }

    public User getUser() {
        return user;
    }

    public boolean isMe() {
        return isMe;
    }

    public void render() {

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
}
