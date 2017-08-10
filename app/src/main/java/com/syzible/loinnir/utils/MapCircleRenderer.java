package com.syzible.loinnir.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.syzible.loinnir.R;
import com.syzible.loinnir.objects.MapCircle;
import com.syzible.loinnir.services.LocationService;

/**
 * Created by ed on 15/07/2017.
 */

public class MapCircleRenderer extends DefaultClusterRenderer<MapCircle> {

    private int GREEN_500;
    private Drawable markerCircle;

    public MapCircleRenderer(Context context, GoogleMap map, ClusterManager<MapCircle> clusterManager) {
        super(context, map, clusterManager);

        GREEN_500 = ContextCompat.getColor(context, R.color.green500);
        markerCircle = ContextCompat.getDrawable(context, R.drawable.marker_circle);
    }

    @Override
    protected void onBeforeClusterItemRendered(MapCircle circle, MarkerOptions markerOptions) {
        super.onBeforeClusterItemRendered(circle, markerOptions);

        CircleOptions options = new CircleOptions()
                .center(circle.getPosition())
                .radius(LocationService.USER_LOCATION_RADIUS)
                .strokeColor(GREEN_500)
                .fillColor(getFillColour());
    }

    private int getFillColour() {
        int r = (GREEN_500) & 0xFF;
        int g = (GREEN_500 >> 8) & 0xFF;
        int b = (GREEN_500 >> 16) & 0xFF;
        int a = 128;

        return Color.argb(a, r, g, b);
    }
}
