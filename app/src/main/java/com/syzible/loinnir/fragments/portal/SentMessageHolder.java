package com.syzible.loinnir.fragments.portal;

import android.view.View;
import android.widget.TextView;

import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.syzible.loinnir.R;
import com.syzible.loinnir.objects.Message;

/**
 * Created by ed on 04/10/2017.
 */

public class SentMessageHolder
        extends MessageHolders.OutcomingTextMessageViewHolder<Message> {

    private TextView wasSeenStatusView;

    public SentMessageHolder(View itemView) {
        super(itemView);
        wasSeenStatusView = (TextView) itemView.findViewById(R.id.was_seen_sent_holder_view);
    }

    @Override
    public void onBind(Message message) {
        super.onBind(message);

        if (message.isWasSeen())
            wasSeenStatusView.setVisibility(View.GONE);
    }
}
