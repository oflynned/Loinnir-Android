package com.syzible.loinnir.utils;

import android.content.Context;
import android.content.Intent;

import com.facebook.login.LoginManager;
import com.syzible.loinnir.services.TokenService;

/**
 * Created by ed on 12/05/2017.
 */

public class FacebookUtils {

    public static void saveToken(String token, Context context) {
        LocalStorage.setStringPref(LocalStorage.Pref.fb_access_token, token, context);
    }

    private static String getToken(Context context) {
        return LocalStorage.getStringPref(LocalStorage.Pref.fb_access_token, context);
    }

    public static boolean hasExistingToken(Context context) {
        return !getToken(context).equals("");
    }

    private static void clearToken(Context context) {
        LocalStorage.purgePref(LocalStorage.Pref.fb_access_token, context);
    }

    public static void getStoredPrefs(Context context) {
        for (LocalStorage.Pref pref : LocalStorage.Pref.values())
            System.out.println(pref.name() + ": " + LocalStorage.getStringPref(pref, context));
    }

    public static void deleteToken(Context context) {
        // stop updating the FCM token to the server
        Intent fcmTokenService = new Intent(context, TokenService.class);
        context.stopService(fcmTokenService);

        // now log out and clear tokens
        clearToken(context);
        LoginManager.getInstance().logOut();
    }
}
