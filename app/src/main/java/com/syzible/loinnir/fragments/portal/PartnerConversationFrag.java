package com.syzible.loinnir.fragments.portal;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.MetaDataUpdate;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.persistence.LocalCacheDatabase;
import com.syzible.loinnir.persistence.LocalCacheDatabaseHelper;
import com.syzible.loinnir.persistence.LocalPrefs;
import com.syzible.loinnir.services.CachingUtil;
import com.syzible.loinnir.services.NetworkAvailableService;
import com.syzible.loinnir.services.NotificationUtils;
import com.syzible.loinnir.utils.BitmapUtils;
import com.syzible.loinnir.utils.BroadcastFilters;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EmojiUtils;
import com.syzible.loinnir.utils.EncodingUtils;
import com.syzible.loinnir.utils.JSONUtils;
import com.syzible.loinnir.utils.LanguageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

import static com.syzible.loinnir.utils.EncodingUtils.copyText;

/**
 * Created by ed on 07/05/2017.
 */

public class PartnerConversationFrag extends Fragment {
    private User partner;
    private View view;

    private Handler handler;
    private Runnable changeSubtitleText;

    private ProgressBar progressBar;

    private ArrayList<Message> messages = new ArrayList<>();
    private ArrayList<Message> paginatedMessages = new ArrayList<>();
    private Context context;
    private boolean isShowingLastActive = false;

    private static final int SUBTITLE_DURATION = 7500; // ms

    private MessagesListAdapter<Message> adapter;
    private BroadcastReceiver newPartnerMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), BroadcastFilters.new_partner_message.toString())) {
                String partnerId = intent.getStringExtra("partner_id");

                // new messages should only be added if you're currently in the correct conversation
                if (partnerId.equals(partner.getId())) {
                    JSONObject payload = JSONUtils.getPartnerInteractionPayload(partnerId, getActivity());
                    RestClient.post(getActivity(), Endpoints.GET_PARTNER_MESSAGES, payload,
                            new BaseJsonHttpResponseHandler<JSONArray>() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                                    try {
                                        JSONObject latestPayload = response.getJSONObject(response.length() - 1);
                                        User sender = new User(latestPayload.getJSONObject("user"));
                                        Message message = new Message(sender, latestPayload.getJSONObject("message"));
                                        adapter.addToStart(message, true);
                                        markSeen();
                                        NotificationUtils.dismissNotification(getActivity(), partner);
                                        partner.setLastActive(sender.getLastActive());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {

                                }

                                @Override
                                protected JSONArray parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                    return new JSONArray(rawJsonData);
                                }
                            });
                }
            }
        }
    };

    private BroadcastReceiver internetAvailableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), "android.net.conn.CONNECTIVITY_CHANGE"))
                if (NetworkAvailableService.isInternetAvailable(getActivity()))
                    NetworkAvailableService.syncCachedData(getActivity());
        }
    };

    private BroadcastReceiver onBlockEnactedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), BroadcastFilters.block_enacted.toString())) {
                String blockEnacterId = intent.getStringExtra("block_enacter_id");
                if (partner.getId().equals(blockEnacterId)) {
                    // you've been blocked -- set the conversations fragment after reloading it
                    // blocked user should now not be able to be visible in the previous chats
                    DisplayUtils.generateToast(getActivity(), "Chuir " + partner.getForename() + " cosc ort! " + EmojiUtils.getEmoji(EmojiUtils.ANXIOUS));

                    RestClient.post(getActivity(), Endpoints.GET_PAST_CONVERSATION_PREVIEWS, JSONUtils.getIdPayload(getActivity()), new BaseJsonHttpResponseHandler<JSONArray>() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                            MainActivity.clearBackstack(getFragmentManager());
                            MainActivity.setFragment(getFragmentManager(), new ConversationsListFrag().setResponse(response));
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {

                        }

                        @Override
                        protected JSONArray parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                            return new JSONArray(rawJsonData);
                        }
                    });
                }
            }
        }
    };

    private BroadcastReceiver onSeenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), BroadcastFilters.message_seen.toString())) {
                String partnerId = intent.getStringExtra("partner_id");
                if (partner.getId().equals(partnerId)) {
                    JSONObject payload = JSONUtils.getPartnerInteractionPayload(partnerId, getActivity());
                    RestClient.post(getActivity(), Endpoints.GET_PARTNER_MESSAGES, payload,
                            new BaseJsonHttpResponseHandler<JSONArray>() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                                    try {
                                        JSONObject latestPayload = response.getJSONObject(response.length() - 1);
                                        User sender = new User(latestPayload.getJSONObject("user"));
                                        Message message = new Message(sender, latestPayload.getJSONObject("message"));
                                        adapter.addToStart(message, true);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {

                                }

                                @Override
                                protected JSONArray parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                    return new JSONArray(rawJsonData);
                                }
                            });
                }
            }
        }
    };

    private void animateSubtitle(TextView subtitleView) {
        if (subtitleView != null) {
            AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
            fadeOut.setDuration(250);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    subtitleView.setText(formatSubtitle());
                    AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                    fadeIn.setDuration(250);
                    subtitleView.startAnimation(fadeIn);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            subtitleView.startAnimation(fadeOut);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.conversation_frag, container, false);
        context = PartnerConversationFrag.this.getActivity();
        progressBar = view.findViewById(R.id.conversations_progress_bar);
        setupAdapter(view);

        return view;
    }

    private String formatSubtitle() {
        isShowingLastActive = !isShowingLastActive;

        if (!isShowingLastActive) {
            return partner.getLocality() + ", " + partner.getCounty();
        } else {
            Date date = new Date(partner.getLastActive());
            DateFormat dayMonthFormatter = new SimpleDateFormat("dd/MM", Locale.ENGLISH);

            String reportedDayMonth = dayMonthFormatter.format(date);
            String givenDayMonth = dayMonthFormatter.format(new Date(System.currentTimeMillis()));

            boolean wasLastActiveToday = reportedDayMonth.equals(givenDayMonth);
            String subtitleFormat = wasLastActiveToday ? "HH:mm" : "HH:mm dd/MM";

            DateFormat formatter = new SimpleDateFormat(subtitleFormat, Locale.ENGLISH);
            return "Ar líne ag " + formatter.format(date) + (wasLastActiveToday ? " inniu" : "");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        progressBar.setVisibility(View.VISIBLE);

        context.registerReceiver(newPartnerMessageReceiver,
                new IntentFilter(BroadcastFilters.new_partner_message.toString()));
        context.registerReceiver(onBlockEnactedReceiver,
                new IntentFilter(BroadcastFilters.block_enacted.toString()));
        context.registerReceiver(onSeenReceiver,
                new IntentFilter(BroadcastFilters.message_seen.toString()));
        context.registerReceiver(internetAvailableReceiver,
                new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        loadMessages();
        NotificationUtils.dismissNotification(context, partner);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(null);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);

            LayoutInflater inflater = LayoutInflater.from(getActivity());
            View toolbarView = inflater.inflate(R.layout.toolbar_partner, null);

            TextView partnerTitleTV = toolbarView.findViewById(R.id.partner_convo_toolbar_title);
            TextView partnerSubtitleTV = toolbarView.findViewById(R.id.partner_convo_toolbar_subtitle);
            partnerTitleTV.setText(partner.getName());
            partnerSubtitleTV.setText(formatSubtitle());

            actionBar.setCustomView(toolbarView);
            actionBar.setDisplayShowCustomEnabled(true);

            partnerTitleTV.setOnClickListener(v -> {
                final Bitmap cachedImage = CachingUtil.getCachedImage(context, partner.getId());
                ProfileFrag profileFrag = new ProfileFrag().setPartner(partner).setBitmap(cachedImage);
                MainActivity.setFragmentBackstack(getFragmentManager(), profileFrag);
            });

            partnerSubtitleTV.setOnClickListener(v -> {
                final Bitmap cachedImage = CachingUtil.getCachedImage(context, partner.getId());
                ProfileFrag profileFrag = new ProfileFrag().setPartner(partner).setBitmap(cachedImage);
                MainActivity.setFragmentBackstack(getFragmentManager(), profileFrag);
            });

            handler = new Handler();
            changeSubtitleText = new Runnable() {
                @Override
                public void run() {
                    animateSubtitle(partnerSubtitleTV);
                    handler.postDelayed(this, SUBTITLE_DURATION);
                }
            };

            handler.postDelayed(changeSubtitleText, SUBTITLE_DURATION);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        context.unregisterReceiver(newPartnerMessageReceiver);
        context.unregisterReceiver(onBlockEnactedReceiver);
        context.unregisterReceiver(onSeenReceiver);
        context.unregisterReceiver(internetAvailableReceiver);

        handler.removeCallbacks(changeSubtitleText);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        }
    }

    private void setMessageInputListener(final MessagesListAdapter<Message> adapter) {
        MessageInput messageInput = view.findViewById(R.id.message_input);
        messageInput.setInputListener(input -> {
            final String messageContent = input.toString().trim();
            if (NetworkAvailableService.isInternetAvailable(context)) {
                RestClient.post(context, Endpoints.GET_USER, JSONUtils.getIdPayload(context),
                        new BaseJsonHttpResponseHandler<JSONObject>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                try {
                                    final User me = new User(response);

                                    JSONObject payload = new JSONObject();
                                    payload.put("my_id", me.getId());
                                    payload.put("partner_id", partner.getId());

                                    RestClient.post(context, Endpoints.GET_PARTNER_MESSAGES_COUNT,
                                            JSONUtils.getPartnerInteractionPayload(partner.getId(), context),
                                            new BaseJsonHttpResponseHandler<JSONObject>() {
                                                @Override
                                                public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                                    try {
                                                        if (response.getInt("count") == 0)
                                                            matchPartner(partner);

                                                        Message message = new Message(LocalPrefs.getID(context),
                                                                me, System.currentTimeMillis(), messageContent);
                                                        adapter.addToStart(message, true);

                                                        // send to server
                                                        JSONObject messagePayload = new JSONObject();
                                                        messagePayload.put("from_id", LocalPrefs.getID(context));
                                                        messagePayload.put("to_id", partner.getId());
                                                        messagePayload.put("message", EncodingUtils.encodeText(message.getText().trim()));

                                                        MetaDataUpdate.updateLastActive(PartnerConversationFrag.this.getActivity());

                                                        RestClient.post(context, Endpoints.SEND_PARTNER_MESSAGE, messagePayload, new BaseJsonHttpResponseHandler<JSONObject>() {
                                                            @Override
                                                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                                                System.out.println(response);
                                                            }

                                                            @Override
                                                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                                                                System.out.println(rawJsonData);
                                                            }

                                                            @Override
                                                            protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                                                return new JSONObject(rawJsonData);
                                                            }
                                                        });
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                }

                                                @Override
                                                public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                                                    System.out.println(rawJsonData);
                                                }

                                                @Override
                                                protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                                    return new JSONObject(rawJsonData);
                                                }
                                            });
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

            } else {
                cacheItem(messageContent);
                String myId = LocalPrefs.getID(context);
                Message cachedMessage = new Message(myId, new User(myId), System.currentTimeMillis(), messageContent);
                adapter.addToStart(cachedMessage, true);
            }
            return true;
        });
    }

    private void cacheItem(String messageContent) {
        LocalCacheDatabase.CachedItem cachedItem = new LocalCacheDatabase.CachedItem(messageContent, partner.getId(), context);
        LocalCacheDatabaseHelper.cacheItem(cachedItem);
        LocalCacheDatabaseHelper.printCachedItemsContents(context);
        DisplayUtils.generateToast(context, "Easpa rochtain idirlín, seolfar do theachtaireacht ar ball");
    }

    private void setLoadMoreListener(final MessagesListAdapter<Message> adapter) {
        adapter.setLoadMoreListener((page, totalItemsCount) -> {
            if (messages.size() > 0) {
                JSONObject payload = new JSONObject();
                try {
                    payload.put("my_id", LocalPrefs.getID(context));
                    payload.put("partner_id", partner.getId());
                    payload.put("last_known_count", totalItemsCount - 1);

                    Message oldestMessage = paginatedMessages.size() == 0 ? messages.get(messages.size() - 1) : paginatedMessages.get(paginatedMessages.size() - 1);
                    payload.put("oldest_message_id", oldestMessage.getId());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                RestClient.post(context, Endpoints.GET_PARTNER_MESSAGES_PAGINATION, payload, new BaseJsonHttpResponseHandler<JSONArray>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                        paginatedMessages.clear();
                        if (response.length() > 0) {
                            for (int i = response.length() - 1; i >= 0; i--) {
                                try {
                                    JSONObject o = response.getJSONObject(i);
                                    User sender = new User(o.getJSONObject("user"));
                                    Message message = new Message(sender, o.getJSONObject("message"));
                                    paginatedMessages.add(message);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                            adapter.addToEnd(paginatedMessages, false);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {

                    }

                    @Override
                    protected JSONArray parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONArray(rawJsonData);
                    }
                });
            }
        });
    }

    private void setLongClickListener(MessagesListAdapter<Message> adapter) {
        adapter.setOnMessageLongClickListener(message -> copyText(getActivity(), message));
    }

    private void setupAdapter(View view) {
        adapter = new MessagesListAdapter<>(LocalPrefs.getID(context), loadImage());

        setLongClickListener(adapter);
        setLoadMoreListener(adapter);
        setMessageInputListener(adapter);

        MessagesList messagesList = view.findViewById(R.id.messages_list);
        messagesList.setAdapter(adapter);
    }

    private void loadMessages() {
        setupAdapter(view);
        RestClient.post(context, Endpoints.GET_PARTNER_MESSAGES,
                JSONUtils.getPartnerInteractionPayload(partner, context),
                new BaseJsonHttpResponseHandler<JSONArray>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                        messages.clear();
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject data = response.getJSONObject(i);
                                JSONObject dataMessage = data.getJSONObject("message");
                                User sender = new User(data.getJSONObject("user"));
                                Message message = new Message(sender, dataMessage);
                                messages.add(message);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        adapter.addToEnd(messages, true);
                        progressBar.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {
                        System.out.println("failed?");
                    }

                    @Override
                    protected JSONArray parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONArray(rawJsonData);
                    }
                });

        markSeen();
    }

    private void markSeen() {
        RestClient.post(context, Endpoints.MARK_PARTNER_MESSAGES_SEEN,
                JSONUtils.getPartnerInteractionPayload(partner, context),
                new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {

                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                    }

                    @Override
                    protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONObject(rawJsonData);
                    }
                });
    }

    public PartnerConversationFrag setPartner(User partner) {
        this.partner = partner;
        return this;
    }

    private ImageLoader loadImage() {
        return (imageView, url) -> {
            // can only use Facebook to sign up so use the embedded id in the url
            final String id = url.split("/")[3];

            if (!CachingUtil.doesImageExist(context, id)) {
                new GetImage(new NetworkCallback<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        Bitmap croppedImage = BitmapUtils.getCroppedCircle(response);
                        final Bitmap scaledAvatar = BitmapUtils.scaleBitmap(croppedImage, BitmapUtils.BITMAP_SIZE_SMALL);
                        CachingUtil.cacheImage(context, id, scaledAvatar);
                        imageView.setImageBitmap(scaledAvatar);
                    }

                    @Override
                    public void onFailure() {
                        System.out.println("dl failure on chat pic");
                    }
                }, url, true).execute();
            } else {
                final Bitmap cachedImage = CachingUtil.getCachedImage(context, id);
                imageView.setImageBitmap(cachedImage);
            }

            imageView.setOnClickListener(v -> {
                if (!id.equals(LocalPrefs.getID(getActivity()))) {
                    RestClient.post(getActivity(), Endpoints.GET_USER, JSONUtils.getUserIdPayload(getActivity(), id), new BaseJsonHttpResponseHandler<JSONObject>() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                            try {
                                final User user = new User(response);
                                DisplayUtils.generateBlockDialog(getActivity(), user, () -> {
                                    DisplayUtils.generateSnackbar(getActivity(), "Cuireadh cosc go rathúil " + LanguageUtils.getPrepositionalForm("ar", user.getForename()));
                                    loadMessages();
                                });
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                            DisplayUtils.generateToast(getActivity(), "Easpa rochtain idirlín");
                        }

                        @Override
                        protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                            return new JSONObject(rawJsonData);
                        }
                    });
                }
            });
        };
    }

    private void matchPartner(User partner) {
        RestClient.post(context, Endpoints.SUBSCRIBE_TO_PARTNER,
                JSONUtils.getPartnerInteractionPayload(partner, context),
                new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                        System.out.println(rawJsonResponse);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                        System.out.println(rawJsonData);
                    }

                    @Override
                    protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                        return new JSONObject(rawJsonData);
                    }
                });
    }
}
