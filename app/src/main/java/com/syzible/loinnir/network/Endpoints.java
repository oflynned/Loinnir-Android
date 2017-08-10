package com.syzible.loinnir.network;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Created by ed on 17/05/2017.
 */

public class Endpoints {
    private static final int API_VERSION = 1;

    private static final String LOCAL_ENDPOINT = "http://10.0.2.2:3000";
    private static final String REMOTE_ENDPOINT = "http://ec2-54-194-22-138.eu-west-1.compute.amazonaws.com";
    private static final String STEM_URL = LOCAL_ENDPOINT;
    private static final String API_URL = STEM_URL + "/api/v" + API_VERSION;

    public static final String CREATE_USER = "/users/create";
    public static final String EDIT_USER = "/users/edit";
    public static final String DELETE_USER = "/users/delete";

    public static final String UPDATE_USER_LOCATION = "/users/update-location";
    public static final String GET_NEARBY_COUNT = "/users/get-nearby-count";

    public static final String GET_USER = "/users/get";
    public static final String GET_OTHER_USERS = "/users/get-others";
    public static final String GET_RANDOM_USER = "/users/get-random";

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

    public static final String LICENCES = "/ceadunais";
    public static final String PRIVACY_POLICIES = "/priobhaideacht";
    public static final String TERMS_OF_SERVICE = "/tos";

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
