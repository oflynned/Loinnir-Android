package com.syzible.loinnir.services;

import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.persistence.LocalPrefs;
import com.syzible.loinnir.utils.BroadcastFilters;
import com.syzible.loinnir.utils.EncodingUtils;
import com.syzible.loinnir.utils.FacebookUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 26/05/2017.
 */

public class ServerBroadcastService extends FirebaseMessagingService {

    private enum NotificationTypes {
        new_partner_message, new_locality_update, block_enacted, push_notification, message_seen
    }

    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (remoteMessage.getData().size() > 0) {
            if (FacebookUtils.hasExistingToken(getApplicationContext())) {
                System.out.println("Data in packet: " + remoteMessage.getData());
                String message_type = remoteMessage.getData().get("notification_type");

                if (message_type.equals(NotificationTypes.new_locality_update.name())) {
                    onLocalityInfoUpdate();
                } else if (message_type.equals(NotificationTypes.new_partner_message.name())) {
                    try {
                        onPartnerMessage(remoteMessage);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (message_type.equals(NotificationTypes.block_enacted.name())) {
                    onBlockEnacted(remoteMessage.getData().get("block_enacter_id"));
                } else if (message_type.equals(NotificationTypes.push_notification.name())) {
                    onPushNotification(remoteMessage.getData());
                } else if (message_type.equals(NotificationTypes.message_seen.name())) {
                    // TODO disabled for 1.0.2
                    // TODO add as a feature for 1.1.0
                    // onPartnerMessageSeen(remoteMessage.getData().get("partner_id"));
                }
            }
        }
    }

    private void onPushNotification(Map<String, String> data) {
        String title = data.get("push_notification_title");
        String content = data.get("push_notification_content");
        String url = data.get("push_notification_link");
        String notificationId = data.get("push_notification_id");

        JSONObject payload = new JSONObject();
        try {
            payload.put("push_notification_id", notificationId);
            payload.put("event", "delivery");
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

        NotificationUtils.generatePushNotification(getApplicationContext(), title, content, url, notificationId);
    }

    /**
     * A broadcast should notify the app if the user has been blocked, and update/disable/remove
     * parts of the app that may or may not be showing to prevent contact and overriding blocks
     *
     * @param userBlockingMeId id of the user who has enacted the block
     */
    private void onBlockEnacted(String userBlockingMeId) {
        Intent handleUiOnBlockBroadcast = new Intent(BroadcastFilters.block_enacted.toString());
        handleUiOnBlockBroadcast.putExtra("block_enacter_id", userBlockingMeId);
        getApplicationContext().sendBroadcast(handleUiOnBlockBroadcast);
    }

    private void onLocalityInfoUpdate() {
        // new locality update in chat
        // emit a broadcast to force an update if the locality fragment is active
        String newLocalityIntent = BroadcastFilters.new_locality_info_update.toString();
        Intent intent = new Intent(newLocalityIntent);
        getApplicationContext().sendBroadcast(intent);
    }

    private void onPartnerMessageSeen(String partnerId) {
        String seenZonedIntent = BroadcastFilters.message_seen.toString();
        Intent intent = new Intent(seenZonedIntent);
        intent.putExtra("partner_id", partnerId);
        getApplicationContext().sendBroadcast(intent);
    }

    private void onPartnerMessage(RemoteMessage remoteMessage) throws JSONException {
        String notificationBody = remoteMessage.getData().get("message");
        User sender = new User(new JSONObject(remoteMessage.getData().get("from_details")));

        // on message received in the foreground
        JSONObject notificationData = new JSONObject(notificationBody);
        String _id = notificationData.getString("_id");
        notificationData.remove("_id");

        JSONObject idObj = new JSONObject();
        idObj.put("$oid", _id);
        notificationData.put("_id", idObj);

        String messageData = EncodingUtils.encodeText(notificationData.getString("message"));
        notificationData.remove("message");
        notificationData.put("message", messageData);

        Message message = new Message(sender, notificationData);

        // for updating UI or creating notifications on receiving a message
        String newMessageIntent = BroadcastFilters.new_partner_message.toString();
        Intent newDataIntent = new Intent(newMessageIntent);
        newDataIntent.putExtra("partner_id", sender.getId());
        getApplicationContext().sendBroadcast(newDataIntent);

        NotificationUtils.generateMessageNotification(getApplicationContext(), sender, message);
    }
}
