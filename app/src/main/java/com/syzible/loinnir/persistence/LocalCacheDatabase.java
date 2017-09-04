package com.syzible.loinnir.persistence;

import android.content.Context;
import android.provider.BaseColumns;

import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.utils.EncodingUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ed on 02/09/2017.
 */

public class LocalCacheDatabase {
    public static final String DATABASE_NAME = "loinnir_local_cache_db";

    private LocalCacheDatabase() {
    }

    public static abstract class Columns implements BaseColumns {
        public static final String TABLE_NAME = "local_message_cache";
        public static final String TIME_SUBMITTED = "time";     // long
        public static final String SENDER = "sender";           // string -- me, but needs to be tracked if a user changes accounts
        public static final String RECIPIENT = "recipient";     // string -- can be a locality name or a user id
        public static final String IS_LOCALITY = "room_type";   // string -- locality/user
        public static final String MESSAGE_CONTENT = "message"; // string -- message being sent
    }

    public static class CachedItem {
        private Message message;
        private String sender, recipient;
        private boolean isLocalityMessage;
        private Context context;

        public CachedItem(Message message, String sender, String recipient, boolean isLocalityMessage, Context context) {
            this.message = message;
            this.sender = sender;
            this.recipient = recipient;
            this.isLocalityMessage = isLocalityMessage;
            this.context = context;
        }

        public String getMessage() {
            return EncodingUtils.encodeText(message.getText());
        }

        public boolean isLocalityMessage() {
            return isLocalityMessage;
        }

        public String getSender() {
            return sender;
        }

        public String getRecipient() {
            return recipient;
        }

        public JSONObject getCachedUserConversationPayload() {
            JSONObject o = new JSONObject();
            try {
                o.put("message", getMessage());
                o.put("to_id", recipient);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return o;
        }

        public JSONObject getCachedLocalityPayload() {
            JSONObject o = new JSONObject();
            try {
                o.put("message", getMessage());
                o.put("locality", LocalPrefs.getStringPref(LocalPrefs.Pref.last_known_location, context));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return o;
        }
    }
}
