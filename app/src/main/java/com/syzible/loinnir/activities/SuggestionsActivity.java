package com.syzible.loinnir.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EmojiUtils;
import com.syzible.loinnir.utils.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import mehdi.sakout.fancybuttons.FancyButton;

/**
 * Created by ed on 27/10/2017.
 */

public class SuggestionsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_suggestions);

        TextView emojiTextView = findViewById(R.id.suggestion_text_4_emojis);
        String emojiString = EmojiUtils.getEmoji(EmojiUtils.HEART_EYES) +
                " " + EmojiUtils.getEmoji(EmojiUtils.HAPPY) +
                " " + EmojiUtils.getEmoji(EmojiUtils.TONGUE);
        emojiTextView.setText(emojiString);

        EditText suggestionBox = findViewById(R.id.suggestion_box_content);

        FancyButton sendSuggestion = findViewById(R.id.send_suggestion_button);
        sendSuggestion.setOnClickListener(v -> {
            String suggestion = suggestionBox.getText().toString().trim();

            if (!suggestion.equals("")) {
                JSONObject o = JSONUtils.getIdPayload(this);
                try {
                    o.put("suggestion", suggestion);
                    o.put("version", this.getResources().getString(R.string.app_version));
                    o.put("time", System.currentTimeMillis());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Toast.makeText(getApplicationContext(), "Á seoladh...", Toast.LENGTH_SHORT).show();

                RestClient.post(this, Endpoints.SEND_SUGGESTION, o, new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                        DisplayUtils.generateToast(SuggestionsActivity.this, "Go raibh maith agat! " + EmojiUtils.getEmoji(EmojiUtils.HAPPY));
                        suggestionBox.setText("");
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                        DisplayUtils.generateToast(SuggestionsActivity.this, "Thit earráid amach! " + EmojiUtils.getEmoji(EmojiUtils.SAD));
                    }

                    @Override
                    protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONObject(rawJsonData);
                    }
                });
            }
        });
    }
}
