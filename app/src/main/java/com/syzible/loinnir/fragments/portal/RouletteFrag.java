package com.syzible.loinnir.fragments.portal;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.utils.AnimationUtils;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EmojiUtils;
import com.syzible.loinnir.utils.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 07/05/2017.
 */

public class RouletteFrag extends Fragment {

    private ImageView rouletteButton;
    private TextView unmatchedUserCountTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.roulette_frag, container, false);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setTitle(R.string.app_name);
            actionBar.setSubtitle(null);
        }

        unmatchedUserCountTextView = view.findViewById(R.id.unmatched_count_roulette);

        RestClient.post(getActivity(), Endpoints.GET_UNMATCHED_COUNT,
                JSONUtils.getIdPayload(getActivity()),
                new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                        try {
                            int count = response.getInt("count");
                            String usersLeftMessage;

                            if (count == 0)
                                usersLeftMessage = "Níl aon daoine nua ann le nasc a chruthú! " + EmojiUtils.getEmoji(EmojiUtils.SAD);
                            else if (count == 1)
                                usersLeftMessage = "Tá " + count + " úsáideoir eile ag baint úsáide as an aip seo nár bhuail tú leis/léi go fóill " + EmojiUtils.getEmoji(EmojiUtils.COOL);
                            else
                                usersLeftMessage = "Tá " + count + " úsáideoir eile ag baint úsáide as an aip seo nár bhuail tú leo go fóill " + EmojiUtils.getEmoji(EmojiUtils.COOL);

                            unmatchedUserCountTextView.setText(usersLeftMessage);

                            if (getView() != null) {
                                ProgressBar progressBar = getView().findViewById(R.id.roulette_progress_bar);
                                progressBar.setVisibility(View.GONE);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                        String usersLeftMessage = "Thit earáid amach ar an tseibhís " + EmojiUtils.getEmoji(EmojiUtils.SAD);
                        unmatchedUserCountTextView.setText(usersLeftMessage);
                    }

                    @Override
                    protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONObject(rawJsonData);
                    }
                });

        rouletteButton = view.findViewById(R.id.roulette_spinner_button);
        rouletteButton.setOnClickListener(v -> {
            AnimationUtils.rotateView(rouletteButton, false);

            RestClient.post(getActivity(), Endpoints.GET_RANDOM_USER,
                    JSONUtils.getIdPayload(getActivity()),
                    new BaseJsonHttpResponseHandler<JSONObject>() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                            try {
                                if (response.has("success")) {
                                    DisplayUtils.generateSnackbar(getActivity(),
                                            "Níl aon úsáideoirí nua ann le nasc a dhéanamh " +
                                                    EmojiUtils.getEmoji(EmojiUtils.SAD));
                                } else {
                                    User partner = new User(response);
                                    RouletteLoadingFrag loadingFrag = new RouletteLoadingFrag()
                                            .setPartner(partner);
                                    MainActivity.setFragmentBackstack(getFragmentManager(), loadingFrag);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                        }

                        @Override
                        protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                            return new JSONObject(rawJsonData);
                        }
                    });
        });

        return view;
    }

}
