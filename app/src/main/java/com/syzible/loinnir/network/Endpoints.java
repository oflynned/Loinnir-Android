package com.syzible.loinnir.network;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

/**
 * Created by ed on 17/05/2017.
 */

public class Endpoints {
    private static final int API_VERSION = 1;

    private static final String LOCAL_ENDPOINT = "http://10.0.2.2:3000";
    private static final String APP_ENDPOINT = "https://loinnir.herokuapp.com";
    public static final String DOMAIN_ENDPOINT = "http://www.loinnir.ie";
    public static final String FACEBOOK_PAGE = "https://www.facebook.com/LoinnirApp";

    private static final String STEM_URL = isDebugMode() ? LOCAL_ENDPOINT : APP_ENDPOINT;
    private static final String API_URL = STEM_URL + "/api/v" + API_VERSION;

    public static boolean isDebugMode() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }

    public static final String CREATE_USER = "/users/create";
    public static final String EDIT_USER = "/users/edit";
    public static final String DELETE_USER = "/users/delete";

    public static final String UPDATE_USER_LOCATION = "/users/update-location";
    public static final String GET_NEARBY_COUNT = "/users/get-nearby-count";
    public static final String PUSH_NOTIFICATION_INTERACTION = "/users/push-notification-interaction";

    public static final String GET_USER = "/users/get";
    public static final String GET_ALL_USERS = "/users/get-all";
    public static final String GET_RANDOM_USER = "/users/get-random";
    public static final String UPDATE_USER_META_DATA = "/users/update-user-meta-data";

    public static final String GET_MATCHED_COUNT = "/users/get-matched-count";
    public static final String GET_UNMATCHED_COUNT = "/users/get-unmatched-count";
    public static final String GET_BLOCKED_USERS = "/users/get-blocked-users";

    public static final String BLOCK_USER = "/users/block-user";
    public static final String UNBLOCK_USER = "/users/unblock-user";

    public static final String SEND_PARTNER_MESSAGE = "/messages/send-partner-message";
    public static final String SEND_LOCALITY_MESSAGE = "/messages/send-locality-message";
    public static final String GET_PAST_CONVERSATION_PREVIEWS = "/messages/get-past-conversation-previews";
    public static final String GET_PARTNER_MESSAGES_COUNT = "/messages/get-partner-messages-count";

    public static final String GET_PARTNER_MESSAGES = "/messages/get-partner-messages";
    public static final String GET_PARTNER_MESSAGES_PAGINATION = "/messages/get-paginated-partner-messages";
    public static final String GET_LOCALITY_MESSAGES = "/messages/get-locality-messages";
    public static final String GET_LOCALITY_MESSAGES_PAGINATION = "/messages/get-paginated-locality-messages";

    public static final String MARK_PARTNER_MESSAGES_SEEN = "/messages/mark-seen";
    public static final String SUBSCRIBE_TO_PARTNER = "/messages/subscribe-partner";

    public static final String PRIVACY_POLICIES = "/priobhaideacht";
    public static final String TERMS_OF_SERVICE = "/tos";

    public static final String SEND_SUGGESTION = "/services/send-suggestion";

    public static String getApiURL(String endpoint) {
        return API_URL + endpoint;
    }

    public static String getFrontendURL(String endpoint) {
        return STEM_URL + endpoint;
    }

    public static void openLink(Context context, String url) {
        Intent openLink = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(openLink);
    }
}
