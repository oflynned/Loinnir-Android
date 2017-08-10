package com.syzible.loinnir.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by ed on 08/05/2017.
 */

public class LocalStorage {
    public enum Pref {
        id, fb_access_token, profile_pic, forename, surname, first_run, should_share_location,
        location_update_frequency, lat, lng
    }

    public static String getID(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("id", "");
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

    public static float getFloatPref(Pref key, Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getFloat(key.name(), 0);
    }

    public static void setStringPref(Pref key, String value, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(key.name(), value).apply();
    }

    public static void setBooleanPref(Pref key, boolean value, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean(key.name(), value).apply();
    }

    public static void setFloatPref(Pref key, float value, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putFloat(key.name(), value).apply();
    }

    public static void purgePref(Pref key, Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putString(key.name(), "").apply();
    }

    public static boolean isLoggedIn(Context context) {
        return !getID(context).equals("");
    }

    public static boolean isFirstRun(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Pref.first_run.name(), true);
    }
}
