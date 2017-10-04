package com.syzible.loinnir.objects;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.syzible.loinnir.R;

/**
 * Created by ed on 22/09/2017.
 */

public class ClusterRenderer extends DefaultClusterRenderer<MapCircle> {
    private Context context;
    private GoogleMap map;

    public ClusterRenderer(Context context, GoogleMap map, ClusterManager<MapCircle> clusterManager) {
        super(context, map, clusterManager);
        this.context = context;
        this.map = map;
    }

    @Override
    protected void onBeforeClusterItemRendered(MapCircle item, MarkerOptions markerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions);
        markerOptions.icon(bitmapDescriptorFromVector(context));

        /*CircleOptions circleOptions = new CircleOptions()
                .center(markerOptions.getPosition())
                .radius(LocationService.USER_LOCATION_RADIUS)
                .strokeColor(getMarkerColour(false))
                .fillColor(getMarkerColour(true));

        map.addCircle(circleOptions);*/
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context) {
        float zoom = map.getCameraPosition().zoom;

        Drawable vectorDrawable = ContextCompat.getDrawable(context, R.drawable.marker_circle);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Bitmap output = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth() / zoom), (int) (bitmap.getHeight() / zoom), false);

        return BitmapDescriptorFactory.fromBitmap(output);
    }

    private int getMarkerColour(boolean isAlpha) {
        int green = ContextCompat.getColor(context, R.color.green500);

        int r = (green) & 0xFF;
        int g = (green >> 8) & 0xFF;
        int b = (green >> 16) & 0xFF;
        int a = isAlpha ? 128 : 0;

        return Color.argb(a, r, g, b);
    }
}
