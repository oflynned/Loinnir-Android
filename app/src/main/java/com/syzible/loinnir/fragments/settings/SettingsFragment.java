package com.syzible.loinnir.fragments.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.support.annotation.Nullable;
import android.view.ContextThemeWrapper;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.AuthenticationActivity;
import com.syzible.loinnir.activities.SettingsActivity;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.services.CachingUtil;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EmojiUtils;
import com.syzible.loinnir.utils.FacebookUtils;
import com.syzible.loinnir.utils.JSONUtils;
import com.syzible.loinnir.utils.LanguageUtils;
import com.syzible.loinnir.utils.LocalStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 29/05/2017.
 */

public class SettingsFragment extends PreferenceFragment {

    SwitchPreference shouldShareLocation;
    Preference manageBlockedUsers, shareApp, aboutLoinnir, visitWebsite;
    Preference appVersion, licences, privacyPolicy, termsOfService;
    Preference logOut, deleteAccount;

    private Activity context;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings_preferences);

        context = getActivity();

        initialisePreferences();
        initialisePreferenceValues();
    }

    @Override
    public void onResume() {
        initialisePreferenceValues();
        super.onResume();
    }

    private void initialisePreferenceValues() {

        // account settings
        setListenerShareLocation();
        setListenerBlockedUsers();

        // about the app
        setListenerShareApp();
        setListenerAboutLoinnir();
        setListenerVisitWebsite();

        // legal affairs
        setListenerLicences();
        setListenerPrivacyPolicy();
        setListenerTOS();

        // danger area
        setListenerLogOut();
        setListenerDeleteAccount();
    }

    private void initialisePreferences() {
        shouldShareLocation = (SwitchPreference) findPreference("pref_should_share_location");
        manageBlockedUsers = findPreference("pref_manage_blocked_users");
        shareApp = findPreference("pref_share_app");
        aboutLoinnir = findPreference("pref_about_loinnir");
        visitWebsite = findPreference("pref_visit_website");
        appVersion = findPreference("pref_app_version");
        licences = findPreference("pref_licences");
        privacyPolicy = findPreference("pref_privacy_policy");
        termsOfService = findPreference("pref_tos");
        logOut = findPreference("pref_log_out");
        deleteAccount = findPreference("pref_delete_account");
    }

    private void setListenerShareLocation() {
        boolean isSharingLocation = LocalStorage.getBooleanPref(LocalStorage.Pref.should_share_location, context);
        shouldShareLocation.setChecked(isSharingLocation);
        shouldShareLocation.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                shouldShareLocation.setChecked(!LocalStorage.getBooleanPref(LocalStorage.Pref.should_share_location, getActivity()));
                LocalStorage.setBooleanPref(LocalStorage.Pref.should_share_location, (boolean) newValue, context);

                RestClient.post(context, Endpoints.EDIT_USER, JSONUtils.getLocationChangePayload(context, (boolean) newValue), new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                        DisplayUtils.generateSnackbar(context, "Nuashonraíodh an socrú ceantair go rathúil");
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                        DisplayUtils.generateSnackbar(context, "Thit earáid amach leis an an socrú ceantair a athrú (" + errorResponse + ")");
                    }

                    @Override
                    protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONObject(rawJsonData);
                    }
                });
                return false;
            }
        });
    }

    private void setListenerBlockedUsers() {
        final BlockedUsersFragment fragment = new BlockedUsersFragment();

        manageBlockedUsers.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                RestClient.post(context, Endpoints.GET_BLOCKED_USERS, JSONUtils.getIdPayload(context), new BaseJsonHttpResponseHandler<JSONArray>() {
                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String rawJsonResponse, JSONArray response) {
                        int count = response.length();

                        ArrayList<String> blockedUsers = new ArrayList<>();
                        for (int i=0; i<count; i++) {
                            try {
                                blockedUsers.add(response.getString(i));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        SettingsActivity.setFragmentBackstack(getFragmentManager(),
                                fragment.setCount(count).setBlockedUsers(blockedUsers));
                    }

                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {

                    }

                    @Override
                    protected JSONArray parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONArray(rawJsonData);
                    }
                });
                return false;
            }
        });

        RestClient.post(context, Endpoints.GET_BLOCKED_USERS, JSONUtils.getIdPayload(context), new BaseJsonHttpResponseHandler<JSONArray>() {
            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String rawJsonResponse, JSONArray response) {
                int count = response.length();
                fragment.setCount(count);
                String summary = "Tá " + count + " úsáideoir ann a bhfuil cosc curtha orthu";
                manageBlockedUsers.setSummary(summary);
            }

            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {

            }

            @Override
            protected JSONArray parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                return new JSONArray(rawJsonData);
            }
        });
    }

    private void setListenerShareApp() {
        shareApp.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                String shareSubText = "Loinnir - Ag Fionnadh Pobail";
                String shareBodyText = shareSubText + " https://play.google.com/store/apps/details?id=com.syzible.loinnir&hl=en";
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubText);
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareBodyText);
                startActivity(Intent.createChooser(shareIntent, "Roinn le"));

                return false;
            }
        });
    }

    private void setListenerAboutLoinnir() {
        aboutLoinnir.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingsActivity.setFragmentBackstack(getFragmentManager(), new AboutLoinnirFragment());
                return false;
            }
        });
    }

    private void setListenerVisitWebsite() {
        visitWebsite.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Endpoints.openLink(context, Endpoints.getFrontendURL(""));
                return false;
            }
        });
    }

    private void setListenerLicences() {
        licences.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Endpoints.openLink(context, Endpoints.getFrontendURL(Endpoints.LICENCES));
                return false;
            }
        });
    }

    private void setListenerPrivacyPolicy() {
        privacyPolicy.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Endpoints.openLink(context, Endpoints.getFrontendURL(Endpoints.PRIVACY_POLICIES));
                return false;
            }
        });
    }

    private void setListenerTOS() {
        termsOfService.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Endpoints.openLink(context, Endpoints.getFrontendURL(Endpoints.TERMS_OF_SERVICE));
                return false;
            }
        });
    }

    private void setListenerLogOut() {
        final String accountName = LocalStorage.getFullName(context);
        logOut.setSummary("Cúntas reatha: " + accountName);

        logOut.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(context)
                        .setTitle("Logáil Amach?")
                        .setMessage("Éireoidh tú logáilte amach de do chuid chúntais (" + accountName + "). Beidh tú in ann logáil isteach leis an gcúntas seo arís, nó le h-aon chúntas Facebook eile.")
                        .setPositiveButton("Logáil Amach", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                DisplayUtils.generateToast(context, "Logáil tú amach");

                                FacebookUtils.deleteToken(context);
                                getActivity().sendBroadcast(new Intent("finish_main_activity"));
                                getActivity().finish();
                                startActivity(new Intent(context, AuthenticationActivity.class));
                            }
                        })
                        .setNegativeButton("Ná Logáil Amach", null)
                        .create()
                        .show();
                return false;
            }
        });
    }

    private void setListenerDeleteAccount() {
        RestClient.post(getActivity(), Endpoints.GET_MATCHED_COUNT, JSONUtils.getIdPayload(getActivity()),
                new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                        try {
                            int count = response.getInt("count");
                            deleteAccount.setSummary("Rabhadh! Caillfidh tú " +
                                    count + " " + LanguageUtils.getCountForm(count, "nasc") +
                                    ". " + EmojiUtils.getEmoji(EmojiUtils.SAD));
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

        deleteAccount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.DangerAppTheme))
                        .setTitle("Do Chúntas Loinnir a Scriosadh?")
                        .setMessage("Tá brón orainn go dteastaíonn uait imeacht! Má ghlacann tú le do chúntas a scriosadh, bainfear do shonraí ar fad ónar bhfreastalaithe, agus ní bheidh tú in ann do chuid chúntais a rochtain gan cúntas eile a chruthú arís.")
                        .setPositiveButton("Deimhnigh an Scriosadh", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                RestClient.delete(getActivity(), Endpoints.DELETE_USER,
                                        JSONUtils.getIdPayload(getActivity()),
                                        new BaseJsonHttpResponseHandler<JSONObject>() {
                                            @Override
                                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                                DisplayUtils.generateToast(context, "Scriosadh do chúntas!");
                                                FacebookUtils.deleteToken(context);
                                                CachingUtil.clearCache(context);

                                                getActivity().sendBroadcast(new Intent("finish_main_activity"));
                                                getActivity().finish();
                                                startActivity(new Intent(context, AuthenticationActivity.class));
                                            }

                                            @Override
                                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                                                DisplayUtils.generateSnackbar(context, "Theip ar scriosadh do chúntais.");
                                            }

                                            @Override
                                            protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                                return new JSONObject(rawJsonData);
                                            }
                                        });
                            }
                        })
                        .setNegativeButton("Ná Scrios!", null)
                        .create()
                        .show();
                return false;
            }
        });
    }
}
