package com.syzible.loinnir.objects;

import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.syzible.loinnir.utils.EmojiUtils;
import com.syzible.loinnir.utils.EncodingUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * Created by ed on 07/05/2017.
 */

public class Message implements IMessage {
    private String id;
    private User sender;
    private long time;
    private String contents;

    public Message(User sender, JSONObject messageObject) {
        try {
            this.id = messageObject.getJSONObject("_id").getString("$oid");
            this.sender = sender;
            this.time = messageObject.getLong("time");
            this.contents = messageObject.getString("message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Message(String id, User sender, long time, String contents) {
        this.id = id;
        this.sender = sender;
        this.time = time;
        this.contents = contents;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getText() {
        return EncodingUtils.decodeText(contents);
    }

    @Override
    public IUser getUser() {
        return sender;
    }

    @Override
    public Date getCreatedAt() {
        return new Date(time);
    }
}
