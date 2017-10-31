package com.syzible.loinnir.services;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v7.app.AlertDialog;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.persistence.LocalCacheDatabaseHelper;
import com.syzible.loinnir.utils.BroadcastFilters;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 04/09/2017.
 */

public class GPSAvailableService {
    public static boolean isGPSAvailable(Context context) {
        LocationManager locationManager = (LocationManager) context
                .getSystemService(Context.LOCATION_SERVICE);

        return (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }

    public static AlertDialog getGPSEnabledDialog(final Activity activity) {
        return new AlertDialog.Builder(activity)
                .setTitle("Easpa Sheirbhísí GPS")
                .setMessage("Níl an tseirbhís GPS ar siúl faoi láthair. " +
                        "Tá an tseirbhís seo riachtanach as ucht meaitseála agus seirbhísí Loinnir. " +
                        "Mura dteastaíonn uait do cheantar a roinnt d'úsáideoirí eile, bainistigh do roghanna sna socruithe.")
                .setPositiveButton("Cuir ar Siúl", (dialog, which) -> activity.startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setCancelable(false)
                .create();
    }
}
