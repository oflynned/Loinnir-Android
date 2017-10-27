package com.syzible.loinnir.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.syzible.loinnir.R;
import com.syzible.loinnir.utils.EmojiUtils;

/**
 * Created by ed on 27/10/2017.
 */

public class SuggestionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_suggestions);

        TextView emojiTextView = (TextView) findViewById(R.id.suggestion_text_4_emojis);
        String emojiString = EmojiUtils.getEmoji(EmojiUtils.HEART_EYES) +
                " " + EmojiUtils.getEmoji(EmojiUtils.HAPPY) +
                " " + EmojiUtils.getEmoji(EmojiUtils.TONGUE);
        emojiTextView.setText(emojiString);
    }
}
