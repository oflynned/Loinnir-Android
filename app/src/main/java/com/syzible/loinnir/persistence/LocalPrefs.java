package com.syzible.loinnir.persistence;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by ed on 08/05/2017.
 */

public class LocalPrefs {
    public enum Pref {
        id, fb_access_token, profile_pic, forename, surname, first_run_completed, should_share_location,
        location_update_frequency, lat, lng, last_known_location, last_known_county, user_agreements_version,
        facebook_permissions_version
    }

    public static String getID(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("id", "");
    }

    public static int getUserAgreementsVersion(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(Pref.user_agreements_version.name(), -1);
    }

    public static int getFacebookPermissionsVersion(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(Pref.facebook_permissions_version.name(), -1);
    }

    public static void setUserAgreementsVersion(Context context, int version) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt(Pref.user_agreements_version.name(), version).apply();
    }

    public static String getStringPref(Pref key, Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key.name(), "");
    }

    public static String getFullName(Context context) {
        return getStringPref(Pref.forename, context) + " " + getStringPref(Pref.surname, context);
    }

    public static boolean getBooleanPref(Pref key, Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key.name(), false);
    }

    public static void setStringPref(Pref key, String value, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(key.name(), value).apply();
    }

    public static void setIntPref(Pref key, int value, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putInt(key.name(), value).apply();
    }

    public static void setBooleanPref(Pref key, boolean value, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(key.name(), value).apply();
    }

    public static void purgePref(Pref key, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(key.name(), "").apply();
    }

    public static boolean isLoggedIn(Context context) {
        return !getID(context).equals("");
    }

    public static boolean isFirstRunCompleted(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Pref.first_run_completed.name(), false);
    }
}
