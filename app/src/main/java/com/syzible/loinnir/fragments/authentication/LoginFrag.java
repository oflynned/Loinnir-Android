package com.syzible.loinnir.fragments.authentication;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.services.LocationService;
import com.syzible.loinnir.services.TokenService;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EmojiUtils;
import com.syzible.loinnir.utils.EncodingUtils;
import com.syzible.loinnir.utils.FacebookUtils;
import com.syzible.loinnir.utils.LocalStorage;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 08/05/2017.
 */

public class LoginFrag extends Fragment {

    private CallbackManager callbackManager;
    private LoginButton facebookLoginButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // default to on, has to be set at some point, may as well be here instead of overriding on each start
        LocalStorage.setBooleanPref(LocalStorage.Pref.should_share_location, true, getActivity());
        callbackManager = CallbackManager.Factory.create();
    }

    private void startMain() {
        getActivity().finish();
        startActivity(new Intent(getActivity(), MainActivity.class));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_frag, container, false);
        facebookLoginButton = (LoginButton) view.findViewById(R.id.login_fb_login_button);
        facebookLoginButton.setFragment(this);
        facebookLoginButton.setReadPermissions("public_profile");
        registerFacebookCallback();

        return view;
    }

    private void registerFacebookCallback() {
        facebookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                String accessToken = loginResult.getAccessToken().getToken();
                FacebookUtils.saveToken(accessToken, getActivity());

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

                                    LocalStorage.setStringPref(LocalStorage.Pref.id, id, getActivity());
                                    LocalStorage.setStringPref(LocalStorage.Pref.forename, forename, getActivity());
                                    LocalStorage.setStringPref(LocalStorage.Pref.surname, surname, getActivity());
                                    LocalStorage.setStringPref(LocalStorage.Pref.profile_pic, pic, getActivity());

                                    Intent startFCMTokenService = new Intent(getActivity(), TokenService.class);
                                    getActivity().startService(startFCMTokenService);

                                    RestClient.post(getActivity(), Endpoints.CREATE_USER, postData, new BaseJsonHttpResponseHandler<JSONObject>() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                            DisplayUtils.generateToast(getActivity(), "Nuashonreofar do cheantar laistigh de chúpla nóiméad");
                                            startMain();
                                        }

                                        @Override
                                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                                            DisplayUtils.generateSnackbar(getActivity(), "Thit earáid amach (" + statusCode + ") " + EmojiUtils.getEmoji(EmojiUtils.SAD));
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
                DisplayUtils.generateSnackbar(getActivity(), "Cuireadh an logáil isteach le Facebook ar ceal " + EmojiUtils.getEmoji(EmojiUtils.TONGUE));
            }

            @Override
            public void onError(FacebookException e) {
                DisplayUtils.generateSnackbar(getActivity(), "Thit earáid amach leis an logáil isteach " + EmojiUtils.getEmoji(EmojiUtils.SAD));
                FacebookUtils.deleteToken(getActivity());
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
