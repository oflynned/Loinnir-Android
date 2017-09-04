package com.syzible.loinnir.persistence;

import android.content.Context;
import android.provider.BaseColumns;

import com.syzible.loinnir.utils.EncodingUtils;

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
        private String message, sender, recipient;
        private boolean isLocalityMessage;
        private Context context;

        // to a locality
        public CachedItem(String message, Context context) {
            this.message = message;
            this.sender = LocalPrefs.getID(context);
            this.recipient = LocalPrefs.getStringPref(LocalPrefs.Pref.last_known_location, context);
            this.isLocalityMessage = true;
            this.context = context;
        }

        // to a user
        public CachedItem(String message, String recipient, Context context) {
            this.message = message;
            this.sender = LocalPrefs.getID(context);
            this.recipient = recipient;
            this.isLocalityMessage = false;
            this.context = context;
        }

        public String getMessage() {
            return EncodingUtils.encodeText(message);
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

        public Context getContext() {
            return context;
        }
    }
}
