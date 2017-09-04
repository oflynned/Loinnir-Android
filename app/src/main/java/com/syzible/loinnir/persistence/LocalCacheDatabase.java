package com.syzible.loinnir.persistence;

import android.provider.BaseColumns;

import com.syzible.loinnir.objects.Message;

/**
 * Created by ed on 02/09/2017.
 */

public class LocalCacheDatabase {
    public static final String DATABASE_NAME = "loinnir_local_cache_db";

    private LocalCacheDatabase() {}

    public static abstract class Columns implements BaseColumns {
        public static final String TABLE_NAME = "local_message_cache";
        public static final String TIME_SUBMITTED = "time";     // long
        public static final String RECIPIENT = "recipient";     // string -- can be a locality name or a user id
        public static final String IS_LOCALITY = "room_type";   // boolean -- locality/user
        public static final String MESSAGE_CONTENT = "message"; // string -- message being sent
    }

    public static class CachedItem {
        private Message message;
        private String recipient;
        private boolean isLocalityMessage;

        public CachedItem(Message message, String recipient, boolean isLocalityMessage) {
            this.message = message;
            this.recipient = recipient;
            this.isLocalityMessage = isLocalityMessage;
        }

        public Message getMessage() {
            return message;
        }

        public String getRecipient() {
            return recipient;
        }

        public boolean isLocalityMessage() {
            return isLocalityMessage;
        }
    }
}
