package com.syzible.loinnir.activities;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.persistence.LocalPrefs;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.FacebookUtils;
import com.syzible.loinnir.utils.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 08/05/2017.
 */

public class SplashActivity extends AppCompatActivity {

    private Handler handler;
    private Runnable runnable;
    private ImageView splashLogo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        splashLogo = (ImageView) findViewById(R.id.splash_app_logo);

        RestClient.post(this, Endpoints.GET_USER, JSONUtils.getIdPayload(this),
                new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                        if (response.has("success")) {
                            try {
                                if (!response.getBoolean("success")) {
                                    FacebookUtils.deleteToken(SplashActivity.this);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        handler = new Handler();
                        runnable = new Runnable() {
                            @Override
                            public void run() {
                                handler.removeCallbacks(this);
                                SplashActivity.this.finish();
                                startActivity(new Intent(SplashActivity.this, IntroductionActivity.class));
                            }
                        };

                        handler.postDelayed(runnable, 1500);

                        YoYo.with(Techniques.FadeIn).duration(300).playOn(splashLogo);
                        YoYo.with(Techniques.RubberBand).delay(300).duration(700).playOn(splashLogo);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                        DisplayUtils.generateSnackbar(SplashActivity.this, "Thit earráid amach ar an tseirbhís!");
                    }

                    @Override
                    protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONObject(rawJsonData);
                    }
                });
    }
}
