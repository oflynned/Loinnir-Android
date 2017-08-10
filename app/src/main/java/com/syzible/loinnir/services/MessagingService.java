package com.syzible.loinnir.services;

import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.utils.BroadcastFilters;
import com.syzible.loinnir.utils.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 26/05/2017.
 */

public class MessagingService extends FirebaseMessagingService {

    private enum NotificationTypes {
        new_partner_message, new_locality_update
    }

    @Override
    public void onMessageReceived(final RemoteMessage remoteMessage) {
        System.out.println("Received FCM update");

        if (remoteMessage.getData().size() > 0) {
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
            }
        }

        super.onMessageReceived(remoteMessage);
    }

    private void onLocalityInfoUpdate() {
        System.out.println("Dispatching onLocalityInfoUpdate()");
        // new locality update in chat, emit a broadcast to force an update if the locality fragment is active
        String newLocalityIntent = BroadcastFilters.new_locality_info_update.toString();
        Intent intent = new Intent(newLocalityIntent);
        getApplicationContext().sendBroadcast(intent);
    }

    private void onPartnerMessage(RemoteMessage remoteMessage) throws JSONException {
        System.out.println("Dispatching onPartnerMessage()");
        String notificationBody = remoteMessage.getData().get("message");
        User sender = new User(new JSONObject(remoteMessage.getData().get("from_details")));

        // on message received in the foreground
        System.out.println(notificationBody);
        JSONObject notificationData = new JSONObject(notificationBody);
        String _id = notificationData.getString("_id");
        notificationData.remove("_id");

        JSONObject idObj = new JSONObject();
        idObj.put("$oid", _id);
        notificationData.put("_id", idObj);

        Message message = new Message(sender, notificationData);

        // for updating UI or creating notifications on receiving a message
        String newMessageIntent = BroadcastFilters.new_partner_message.toString();
        Intent newDataIntent = new Intent(newMessageIntent);
        newDataIntent.putExtra("partner_id", sender.getId());
        getApplicationContext().sendBroadcast(newDataIntent);

        NotificationUtils.generateMessageNotification(getApplicationContext(), sender, message);
    }
}
