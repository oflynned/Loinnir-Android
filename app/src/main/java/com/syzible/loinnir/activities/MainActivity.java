package com.syzible.loinnir.activities;

import android.animation.Animator;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.fragments.portal.ConversationsListFrag;
import com.syzible.loinnir.fragments.portal.LocalityConversationFrag;
import com.syzible.loinnir.fragments.portal.MapFrag;
import com.syzible.loinnir.fragments.portal.PartnerConversationFrag;
import com.syzible.loinnir.fragments.portal.RouletteFrag;
import com.syzible.loinnir.fragments.portal.RouletteOutcomeFrag;
import com.syzible.loinnir.location.LocationService;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.MetaDataUpdate;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.persistence.Constants;
import com.syzible.loinnir.persistence.LocalPrefs;
import com.syzible.loinnir.services.CachingUtil;
import com.syzible.loinnir.services.GPSAvailableService;
import com.syzible.loinnir.services.NetworkAvailableService;
import com.syzible.loinnir.services.NotificationUtils;
import com.syzible.loinnir.utils.BitmapUtils;
import com.syzible.loinnir.utils.BroadcastFilters;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EmojiUtils;
import com.syzible.loinnir.utils.EncodingUtils;
import com.syzible.loinnir.utils.JSONUtils;
import com.syzible.loinnir.utils.LanguageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

import static com.syzible.loinnir.persistence.Constants.getCountyFileName;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private NavigationView navigationView;

    private boolean shouldDisplayGreeting;
    private View headerView;
    private AlertDialog isGPSEnabledDialog;

    private Fragment currentFragment;

    private boolean isTopicBarShowing = true;
    private View topicBarView;

    private BroadcastReceiver finishMainActivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            assert finishMainActivityReceiver != null;
            if (Objects.equals(intent.getAction(), BroadcastFilters.finish_main_activity.name())) {
                finish();
            }
        }
    };

    private BroadcastReceiver changeGPSEnabledReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            assert changeGPSEnabledReceiver != null;
            if (Objects.equals(intent.getAction(), "android.location.PROVIDERS_CHANGED")) {
                if (!GPSAvailableService.isGPSAvailable(MainActivity.this)) {
                    isGPSEnabledDialog.show();
                } else {
                    isGPSEnabledDialog.cancel();
                }
            }
        }
    };

    private static boolean isAppVisible;

    public static boolean isActivityVisible() {
        return isAppVisible;
    }

    public static void setAppResumed() {
        isAppVisible = true;
    }

    public static void setAppPausedOrDead() {
        isAppVisible = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        isGPSEnabledDialog = GPSAvailableService.getGPSEnabledDialog(this);

        startService(new Intent(this, LocationService.class));

        String fcmToken = FirebaseInstanceId.getInstance().getToken();
        updateFcmToken(fcmToken);

        topicBarView = findViewById(R.id.topic_bar);
        topicBarView.setOnClickListener(v -> {
            if (!(currentFragment instanceof LocalityConversationFrag)) {
                Fragment fragment = new LocalityConversationFrag();
                currentFragment = fragment;
                setFragment(getFragmentManager(), fragment);
            }
        });

        if (!NetworkAvailableService.isInternetAvailable(this)) {
            topicBarView.setVisibility(View.GONE);
        }
        loadWeeklyTopic();

        MetaDataUpdate.updateLastActive(this);
        if (!GPSAvailableService.isGPSAvailable(this)) {
            isGPSEnabledDialog.show();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                setLocality();
                hideKeyboard();
            }
        };

        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.getMenu().getItem(0).setChecked(true);
        headerView = navigationView.getHeaderView(0);

        shouldDisplayGreeting = true;

        setUpDrawer();
        checkNotificationInvocation();

        if (shouldDisplayGreeting)
            greetUser();

        if (Constants.USER_AGREEMENT_VERSION != LocalPrefs.getUserAgreementsVersion(this)) {
            DisplayUtils.notifyChangeInTOS(this);
        }
    }

    private void updateFcmToken(String fcmToken) {
        if (fcmToken != null) {
            if (!fcmToken.equals("")) {
                JSONObject o = new JSONObject();
                try {
                    o.put("fb_id", LocalPrefs.getID(getApplicationContext()));
                    o.put("fcm_token", fcmToken);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                RestClient.post(this, Endpoints.EDIT_USER, o, new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                        System.out.println("Token update successful");
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                        System.out.println(rawJsonData);
                    }

                    @Override
                    protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONObject(rawJsonData);
                    }
                });
            }
        }
    }

    private void loadWeeklyTopic() {
        RestClient.get(Endpoints.GET_WEEKLY_TOPIC, new BaseJsonHttpResponseHandler<JSONObject>() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                try {
                    String topic = "TnS: " + EncodingUtils.decodeText(response.getString("topic"));
                    TextView topicView = findViewById(R.id.topic_text_view);
                    topicView.setText(topic);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

            }

            @Override
            protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                return new JSONObject(rawJsonData);
            }
        });
    }

    private void setGaLocale() {
        Locale locale = new Locale("ga");
        Locale.setDefault(locale);
        Configuration config = getBaseContext().getResources().getConfiguration();
        config.locale = locale;
        getApplicationContext().getResources().updateConfiguration(
                config, getApplicationContext().getResources().getDisplayMetrics());
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager)
                getApplicationContext().getSystemService(Activity.INPUT_METHOD_SERVICE);

        View currentView = MainActivity.this.getCurrentFocus();
        if (currentView != null)
            if (inputMethodManager != null)
                inputMethodManager.hideSoftInputFromWindow(currentView.getWindowToken(), 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(finishMainActivityReceiver,
                new IntentFilter(BroadcastFilters.finish_main_activity.toString()));
        registerReceiver(changeGPSEnabledReceiver,
                new IntentFilter("android.location.PROVIDERS_CHANGED"));

        setGaLocale();
        MainActivity.setAppResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainActivity.setAppPausedOrDead();
        unregisterReceiver(finishMainActivityReceiver);
        unregisterReceiver(changeGPSEnabledReceiver);
        MetaDataUpdate.updateLastActive(this);
    }

    private void greetUser() {
        String name = LocalPrefs.getStringPref(LocalPrefs.Pref.forename, this);
        DisplayUtils.generateSnackbar(this, "Fáilte romhat, a " + LanguageUtils.getVocative(name) + "! " +
                EmojiUtils.getEmoji(EmojiUtils.HAPPY));
    }

    private void checkNotificationInvocation() {
        String invocationType = getIntent().getStringExtra("invoker");
        if (invocationType != null) {
            switch (invocationType) {
                case "notification":
                    shouldDisplayGreeting = false;
                    navigationView.getMenu().getItem(1).setChecked(true);

                    String partnerId = getIntent().getStringExtra("user");
                    JSONObject chatPayload = new JSONObject();
                    try {
                        chatPayload.put("fb_id", partnerId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // get partner details and then open the chat fragment
                    RestClient.post(this, Endpoints.GET_USER, chatPayload, new BaseJsonHttpResponseHandler<JSONObject>() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                            try {
                                final User partner = new User(response);
                                final PartnerConversationFrag frag = new PartnerConversationFrag()
                                        .setPartner(partner);
                                currentFragment = frag;

                                RestClient.post(getApplicationContext(), Endpoints.GET_PAST_CONVERSATION_PREVIEWS, JSONUtils.getIdPayload(getApplicationContext()), new BaseJsonHttpResponseHandler<JSONArray>() {
                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                                        MainActivity.clearBackstack(getFragmentManager());
                                        MainActivity.setFragmentBackstack(getFragmentManager(), new ConversationsListFrag().setResponse(response));
                                        MainActivity.setFragmentBackstack(getFragmentManager(), frag);
                                        NotificationUtils.dismissNotification(getApplicationContext(), partner);
                                    }

                                    @Override
                                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {

                                    }

                                    @Override
                                    protected JSONArray parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                        return new JSONArray(rawJsonData);
                                    }
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                        }

                        @Override
                        protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                            return new JSONObject(rawJsonData);
                        }
                    });
                    break;
                case "push_notification":
                    shouldDisplayGreeting = false;

                    JSONObject payload = new JSONObject();
                    try {
                        payload.put("push_notification_id", getIntent().getStringExtra("push_notification_id"));
                        payload.put("event", "interaction");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    RestClient.post(getApplicationContext(),
                            Endpoints.PUSH_NOTIFICATION_INTERACTION,
                            payload,
                            new BaseJsonHttpResponseHandler<JSONObject>() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {

                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                                }

                                @Override
                                protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                    return new JSONObject(rawJsonData);
                                }
                            });
                    break;
                case "weekly_topic":
                    int id = Integer.valueOf(getIntent().getStringExtra("id"));
                    NotificationUtils.dismissNotification(getApplicationContext(), id);

                    shouldDisplayGreeting = false;
                    navigationView.getMenu().getItem(3).setChecked(true);
                    setFragment(getFragmentManager(), new LocalityConversationFrag());
                    break;
            }
        } else {
            setFragment(getFragmentManager(), new MapFrag());
        }
    }

    private void setLocality() {
        RestClient.post(getApplicationContext(), Endpoints.GET_USER, JSONUtils.getIdPayload(this), new BaseJsonHttpResponseHandler<JSONObject>() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                try {
                    String locality = response.getString("locality");
                    String county = response.getString("county");
                    LocalPrefs.setStringPref(LocalPrefs.Pref.last_known_location, locality, MainActivity.this);
                    LocalPrefs.setStringPref(LocalPrefs.Pref.last_known_county, county, MainActivity.this);

                    setDrawerFlagData(locality, county);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                Activity context = MainActivity.this;
                String lastLocation = LocalPrefs.getStringPref(LocalPrefs.Pref.last_known_location, context);
                String lastCounty = LocalPrefs.getStringPref(LocalPrefs.Pref.last_known_county, context);
                setDrawerFlagData(lastLocation, lastCounty);
                DisplayUtils.generateToast(context, "Easpa rochtain idirlín");
            }

            @Override
            protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                return new JSONObject(rawJsonData);
            }
        });
    }

    private void setDrawerFlagData(String locality, String county) {
        TextView localityName = headerView.findViewById(R.id.nav_header_locality);
        TextView countyName = headerView.findViewById(R.id.nav_header_county);

        if (locality.equals("abroad"))
            locality = "Thar Sáile";

        if (county.equals("abroad"))
            county = "Éire";

        localityName.setText(locality);
        countyName.setText(county);

        String countyFlagFile = getCountyFileName(county);
        int flagDrawable = getResources().getIdentifier(countyFlagFile, "drawable", getPackageName());
        ImageView countyFlag = findViewById(R.id.nav_header_county_flag);
        if (flagDrawable != 0) {
            countyFlag.setImageResource(flagDrawable);
        } else {
            countyFlag.setImageResource(R.mipmap.ic_launcher);
        }
    }

    private void setUpDrawer() {
        TextView userName = headerView.findViewById(R.id.nav_header_name);
        userName.setText(LocalPrefs.getFullName(this));

        setLocality();

        final ImageView profilePic = headerView.findViewById(R.id.nav_header_pic);
        final String myId = LocalPrefs.getID(getApplicationContext());

        if (!CachingUtil.doesImageExist(getApplicationContext(), myId)) {
            String picUrl = LocalPrefs.getStringPref(LocalPrefs.Pref.profile_pic, this);

            new GetImage(new NetworkCallback<Bitmap>() {
                @Override
                public void onResponse(Bitmap pic) {
                    Bitmap croppedImage = BitmapUtils.getCroppedCircle(pic);
                    Bitmap scaledAvatar = BitmapUtils.scaleBitmap(croppedImage, BitmapUtils.BITMAP_SIZE);
                    profilePic.setImageBitmap(scaledAvatar);
                    CachingUtil.cacheImage(getApplicationContext(), myId, scaledAvatar);
                }

                @Override
                public void onFailure() {

                }
            }, picUrl, true).execute();
        } else {
            Bitmap pic = CachingUtil.getCachedImage(getApplicationContext(), myId);
            profilePic.setImageBitmap(pic);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            // returning from the roulette fragment should show original roulette screen
            Fragment currentFragment = getFragmentManager().findFragmentById(R.id.portal_frame);
            if (currentFragment.getClass().getName().equals(RouletteOutcomeFrag.class.getName())) {
                MainActivity.clearBackstack(getFragmentManager());
                MainActivity.setFragment(getFragmentManager(), new RouletteFrag());
            } else {
                // if there's only one fragment on the stack we should prevent the default
                // popping to ask for the user's permission to close the app
                if (getFragmentManager().getBackStackEntryCount() < 1) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("An Aip a Dhúnadh?")
                            .setMessage("Má bhrúitear an chnaipe \"Dún\", dúnfar an aip. An bhfuil tú cinnte go bhfuil sé seo ag teastáil uait?")
                            .setPositiveButton("Dún", (dialog, which) -> MainActivity.this.finish())
                            .setNegativeButton("Ná dún", null)
                            .show();
                } else {
                    super.onBackPressed();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.action_suggestion_box) {
            startActivity(new Intent(this, SuggestionsActivity.class));
        } else if (id == R.id.action_topic) {
            if (NetworkAvailableService.isInternetAvailable(this)) {
                if (!isTopicBarShowing) {
                    Animator animator = ViewAnimationUtils.createCircularReveal(
                            topicBarView,
                            topicBarView.getWidth() - 130,
                            topicBarView.getHeight() - 130,
                            0,
                            (float) Math.hypot(topicBarView.getWidth(), topicBarView.getHeight()));
                    topicBarView.setVisibility(View.VISIBLE);
                    animator.setInterpolator(new AccelerateDecelerateInterpolator());
                    if (topicBarView.getVisibility() == View.VISIBLE) {
                        animator.setDuration(400);
                        animator.start();
                        topicBarView.setEnabled(true);
                    }
                } else {
                    topicBarView.setVisibility(View.GONE);
                    topicBarView.setEnabled(false);
                }

                isTopicBarShowing = !isTopicBarShowing;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_around_me) {
            clearBackstack(getFragmentManager());
            Fragment fragment = new MapFrag();
            currentFragment = fragment;
            setFragment(getFragmentManager(), fragment);
        } else if (id == R.id.nav_conversations) {
            clearBackstack(getFragmentManager());
            RestClient.post(this, Endpoints.GET_PAST_CONVERSATION_PREVIEWS,
                    JSONUtils.getIdPayload(this),
                    new BaseJsonHttpResponseHandler<JSONArray>() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                            Fragment fragment = new ConversationsListFrag().setResponse(response);
                            currentFragment = fragment;
                            setFragment(getFragmentManager(), fragment);
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {

                        }

                        @Override
                        protected JSONArray parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                            return new JSONArray(rawJsonData);
                        }
                    }
            );

        } else if (id == R.id.nav_roulette) {
            clearBackstack(getFragmentManager());
            Fragment fragment = new RouletteFrag();
            currentFragment = fragment;
            setFragment(getFragmentManager(), fragment);
        } else if (id == R.id.nav_nearby) {
            clearBackstack(getFragmentManager());
            Fragment fragment = new LocalityConversationFrag();
            currentFragment = fragment;
            setFragment(getFragmentManager(), fragment);
        } else if (id == R.id.nav_rate) {
            Intent intent;

            try {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
            } catch (android.content.ActivityNotFoundException e) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
            }
            startActivity(intent);

        } else if (id == R.id.nav_more_apps) {
            Intent intent;

            try {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:Syzible"));
            } catch (android.content.ActivityNotFoundException e) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/search?q:Syzible"));
            }

            startActivity(intent);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    public static void setFragment(FragmentManager fragmentManager, Fragment fragment) {
        if (fragmentManager != null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.portal_frame, fragment)
                    .commit();
        }
    }

    public static void setFragmentBackstack(FragmentManager fragmentManager, Fragment fragment) {
        if (fragmentManager != null)
            fragmentManager.beginTransaction()
                    .replace(R.id.portal_frame, fragment)
                    .addToBackStack(fragment.getClass().getName())
                    .commit();
    }

    public static void removeFragment(FragmentManager fragmentManager) {
        if (fragmentManager != null)
            fragmentManager.popBackStack();
    }

    public static void clearBackstack(FragmentManager fragmentManager) {
        if (fragmentManager != null)
            fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }
}
