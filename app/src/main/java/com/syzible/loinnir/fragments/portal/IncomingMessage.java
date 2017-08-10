package com.syzible.loinnir.fragments.portal;

import android.view.View;
import android.widget.TextView;

import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.syzible.loinnir.R;
import com.syzible.loinnir.objects.Message;

/**
 * Created by ed on 11/06/2017.
 */

public class IncomingMessage extends MessagesListAdapter.IncomingMessageViewHolder<Message> {
    private TextView senderName;

    public IncomingMessage(View itemView) {
        super(itemView);
        senderName = (TextView) itemView.findViewById(R.id.message_sender);
    }

    @Override
    public void onBind(Message message) {
        super.onBind(message);
        senderName.setText(message.getUser().getName());
    }
}
