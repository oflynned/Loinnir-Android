package com.syzible.loinnir.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.persistence.LocalPrefs;
import com.syzible.loinnir.services.LocationService;
import com.syzible.loinnir.services.TokenService;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EmojiUtils;
import com.syzible.loinnir.utils.EncodingUtils;
import com.syzible.loinnir.utils.FacebookUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Random;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 08/05/2017.
 */

public class AuthenticationActivity extends AppCompatActivity {

    private CallbackManager callbackManager;
    private LoginButton facebookLoginButton;
    private ImageView logo;

    final int[] flags = {
            R.drawable.an_cabhan,
            R.drawable.an_clar,
            R.drawable.an_dun,
            R.drawable.an_iarmhi,
            R.drawable.an_longfort,
            R.drawable.an_mhi,
            R.drawable.aontroim,
            R.drawable.ard_mhacha,
            R.drawable.ath_cliath,
            R.drawable.ceatharlach,
            R.drawable.ciarrai,
            R.drawable.cill_ceannaigh,
            R.drawable.cill_dara,
            R.drawable.cill_mhantain,
            R.drawable.corcaigh,
            R.drawable.doire,
            R.drawable.dun_na_ngall,
            R.drawable.eire,
            R.drawable.fear_manach,
            R.drawable.gaillimh,
            R.drawable.laois,
            R.drawable.liatroim,
            R.drawable.loch_garman,
            R.drawable.lu,
            R.drawable.luimneach,
            R.drawable.maigh_eo,
            R.drawable.muineachan,
            R.drawable.port_lairge,
            R.drawable.ros_comain,
            R.drawable.sligeach,
            R.drawable.tiobraid_arann,
            R.drawable.tir_eoghain,
            R.drawable.uibh_fhaili
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_frag);

        LocalPrefs.setBooleanPref(LocalPrefs.Pref.should_share_location, true, this);

        logo = (ImageView) findViewById(R.id.iv_login_app_logo);
        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YoYo.with(Techniques.RubberBand).duration(700).playOn(logo);
            }
        });

        callbackManager = CallbackManager.Factory.create();

        facebookLoginButton = (LoginButton) findViewById(R.id.login_fb_login_button);
        facebookLoginButton.setReadPermissions(Arrays.asList("public_profile"));

        registerFacebookCallback();
        animateFlag();

        if (FacebookUtils.hasExistingToken(this)) {
            this.finish();
            startActivity(new Intent(this, MainActivity.class));
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        AuthenticationActivity.this.finish();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    private void animateFlag() {
        final ImageView flagView = (ImageView) findViewById(R.id.iv_login_flag_generation);

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                setRandomFlag(flagView);
                YoYo.with(Techniques.FadeIn).duration(700).playOn(flagView);
                YoYo.with(Techniques.RubberBand).delay(700).duration(700).playOn(flagView);
                handler.postDelayed(this, 2000);
            }
        };

        setRandomFlag(flagView);
        YoYo.with(Techniques.FadeIn).duration(700).playOn(flagView);
        handler.postDelayed(runnable, 2000);
    }

    private void setRandomFlag(ImageView view) {
        int randomIndex = new Random().nextInt(flags.length);
        view.setImageResource(flags[randomIndex]);
    }

    private void startMain() {
        AuthenticationActivity.this.finish();
        startActivity(new Intent(AuthenticationActivity.this, MainActivity.class));
    }

    private void registerFacebookCallback() {
        facebookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                String accessToken = loginResult.getAccessToken().getToken();
                FacebookUtils.saveToken(accessToken, AuthenticationActivity.this);

                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject o, GraphResponse response) {
                                try {
                                    System.out.println("Received o: " + o.toString());
                                    String id = o.getString("id");
                                    String forename = o.getString("first_name");
                                    String surname = o.getString("last_name");
                                    String gender = o.getString("gender");
                                    String pic = "https://graph.facebook.com/" + id + "/picture?type=large";

                                    JSONObject postData = new JSONObject();
                                    postData.put("fb_id", id);
                                    postData.put("forename", EncodingUtils.encodeText(forename));
                                    postData.put("surname", EncodingUtils.encodeText(surname));
                                    postData.put("gender", gender);
                                    postData.put("profile_pic", pic);
                                    postData.put("show_location", true);

                                    // temp location until the phone updates
                                    postData.put("lat", LocationService.ATHLONE.latitude);
                                    postData.put("lng", LocationService.ATHLONE.longitude);

                                    LocalPrefs.setStringPref(LocalPrefs.Pref.id, id, AuthenticationActivity.this);
                                    LocalPrefs.setStringPref(LocalPrefs.Pref.forename, forename, AuthenticationActivity.this);
                                    LocalPrefs.setStringPref(LocalPrefs.Pref.surname, surname, AuthenticationActivity.this);
                                    LocalPrefs.setStringPref(LocalPrefs.Pref.profile_pic, pic, AuthenticationActivity.this);

                                    Intent startFCMTokenService = new Intent(AuthenticationActivity.this, TokenService.class);
                                    AuthenticationActivity.this.startService(startFCMTokenService);

                                    RestClient.post(AuthenticationActivity.this, Endpoints.CREATE_USER, postData, new BaseJsonHttpResponseHandler<JSONObject>() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                            DisplayUtils.generateToast(AuthenticationActivity.this, "Nuashonreofar do cheantar laistigh de chúpla nóiméad");
                                            startMain();
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                                            DisplayUtils.generateSnackbar(AuthenticationActivity.this, "Thit earáid amach (" + statusCode + ") " + EmojiUtils.getEmoji(EmojiUtils.SAD));
                                        }

                                        @Override
                                        protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                            return new JSONObject(rawJsonData);
                                        }
                                    });

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                        });

                Bundle parameters = new Bundle();
                parameters.putString("fields", "id, first_name, last_name, gender");
                request.setParameters(parameters);
                request.executeAsync();
            }

            @Override
            public void onCancel() {
                DisplayUtils.generateSnackbar(AuthenticationActivity.this, "Cuireadh an logáil isteach le Facebook ar ceal " + EmojiUtils.getEmoji(EmojiUtils.TONGUE));
            }

            @Override
            public void onError(FacebookException e) {
                DisplayUtils.generateSnackbar(AuthenticationActivity.this, "Thit earáid amach leis an logáil isteach " + EmojiUtils.getEmoji(EmojiUtils.SAD));
                FacebookUtils.deleteToken(AuthenticationActivity.this);
                e.printStackTrace();
            }
        });
    }
}
