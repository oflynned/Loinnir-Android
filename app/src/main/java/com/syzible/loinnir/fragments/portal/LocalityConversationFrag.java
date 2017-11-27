package com.syzible.loinnir.fragments.portal;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.syzible.loinnir.R;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.persistence.LocalCacheDatabase;
import com.syzible.loinnir.persistence.LocalCacheDatabaseHelper;
import com.syzible.loinnir.persistence.LocalPrefs;
import com.syzible.loinnir.services.CachingUtil;
import com.syzible.loinnir.services.NetworkAvailableService;
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

import java.util.ArrayList;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 07/05/2017.
 */

public class LocalityConversationFrag extends Fragment {
    private View view;
    private ArrayList<Message> messages = new ArrayList<>();
    private ArrayList<Message> paginatedMessages = new ArrayList<>();

    private MessagesListAdapter<Message> adapter;
    private ProgressBar progressBar;

    private BroadcastReceiver onChangeInLocalityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), BroadcastFilters.changed_locality.toString())) {
                // the newest update in locality doesn't correspond to the last one on record
                // a user should be changed into a new chat room and the messages be reloaded
                loadMessages();
            }
        }
    };

    private BroadcastReceiver onNewLocalityInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), BroadcastFilters.new_locality_info_update.toString())) {
                RestClient.post(getActivity(), Endpoints.GET_NEARBY_COUNT, JSONUtils.getIdPayload(getActivity()),
                        new BaseJsonHttpResponseHandler<JSONObject>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                try {
                                    String localityName = response.getString("locality");
                                    int nearbyUsers = response.getInt("count");
                                    String count = nearbyUsers + " eile anseo";

                                    ActionBar actionBar = ((AppCompatActivity) context).getSupportActionBar();
                                    if (actionBar != null) {
                                        actionBar.setTitle(localityName);
                                        actionBar.setSubtitle(count);
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

                RestClient.post(getActivity(), Endpoints.GET_LOCALITY_MESSAGES, JSONUtils.getIdPayload(getActivity()),
                        new BaseJsonHttpResponseHandler<JSONArray>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                                try {
                                    JSONObject latestPayload = response.getJSONObject(response.length() - 1);
                                    User sender = new User(latestPayload.getJSONObject("user"));
                                    Message message = new Message(sender, latestPayload);
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
    };


    private BroadcastReceiver internetAvailableReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), "android.net.conn.CONNECTIVITY_CHANGE"))
                if (NetworkAvailableService.isInternetAvailable(getActivity()))
                    NetworkAvailableService.syncCachedData(getActivity());
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.conversation_frag, container, false);
        progressBar = view.findViewById(R.id.conversations_progress_bar);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        progressBar.setVisibility(View.VISIBLE);
        loadMessages();

        getActivity().registerReceiver(onNewLocalityInfoReceiver,
                new IntentFilter(BroadcastFilters.new_locality_info_update.toString()));

        getActivity().registerReceiver(onChangeInLocalityReceiver,
                new IntentFilter(BroadcastFilters.changed_locality.toString()));

        getActivity().registerReceiver(internetAvailableReceiver,
                new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(onNewLocalityInfoReceiver);
        getActivity().unregisterReceiver(onChangeInLocalityReceiver);
        getActivity().unregisterReceiver(internetAvailableReceiver);
    }

    private MessageHolders getIncomingHolder() {
        return new MessageHolders()
                .setIncomingTextConfig(IncomingMessage.class, R.layout.chat_message_layout);
    }

    private void setSendMessageListener(final MessagesListAdapter<Message> adapter) {
        final boolean[] outcome = {true};

        MessageInput messageInput = view.findViewById(R.id.message_input);
        messageInput.setInputListener(input -> {
            final String messageContent = input.toString().trim();
            outcome[0] = Patterns.WEB_URL.matcher(messageContent.toLowerCase()).matches();

            // check if the message contains a link
            if (!outcome[0]) {
                if (NetworkAvailableService.isInternetAvailable(getActivity())) {
                    try {
                        JSONObject messagePayload = new JSONObject();
                        messagePayload.put("fb_id", LocalPrefs.getID(getActivity()));
                        messagePayload.put("message", EncodingUtils.encodeText(messageContent));

                        RestClient.post(getActivity(), Endpoints.SEND_LOCALITY_MESSAGE, messagePayload, new BaseJsonHttpResponseHandler<JSONObject>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                RestClient.post(getActivity(), Endpoints.GET_LOCALITY_MESSAGES, JSONUtils.getIdPayload(getActivity()), new BaseJsonHttpResponseHandler<JSONArray>() {
                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                                        try {
                                            JSONObject latestMessage = response.getJSONObject(response.length() - 1);
                                            User sender = new User(latestMessage.getJSONObject("user"));
                                            String userId = latestMessage.getJSONObject("_id").getString("$oid");
                                            String userMessage = EncodingUtils.decodeText(latestMessage.getString("message"));
                                            Message message = new Message(userId, sender, System.currentTimeMillis(), userMessage);
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

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                            }

                            @Override
                            protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                return new JSONObject(rawJsonData);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    cacheItem(messageContent);
                    String myId = LocalPrefs.getID(getActivity());
                    Message cachedMessage = new Message(myId, new User(myId), System.currentTimeMillis(), messageContent);
                    adapter.addToStart(cachedMessage, true);
                }
            } else {
                DisplayUtils.generateSnackbar(getActivity(), "Ní cheadaítear nascanna sa seomra seo. " + EmojiUtils.getEmoji(EmojiUtils.TONGUE));
            }
            return !outcome[0];
        });
    }

    private void cacheItem(String messageContent) {
        LocalCacheDatabase.CachedItem cachedItem = new LocalCacheDatabase.CachedItem(messageContent, getActivity());
        LocalCacheDatabaseHelper.cacheItem(cachedItem);
        LocalCacheDatabaseHelper.printCachedItemsContents(getActivity());
        DisplayUtils.generateToast(getActivity(), "Easpa rochtain idirlín, seolfar do theachtaireacht ar ball");
    }

    private void setLoadMoreListener(final MessagesListAdapter<Message> adapter) {
        adapter.setLoadMoreListener((page, totalItemsCount) -> {
            if (messages.size() > 0) {
                JSONObject payload = new JSONObject();
                try {
                    payload.put("fb_id", LocalPrefs.getID(getActivity()));
                    Message oldestMessage = paginatedMessages.size() == 0 ? messages.get(messages.size() - 1) : paginatedMessages.get(paginatedMessages.size() - 1);
                    payload.put("oldest_message_id", oldestMessage.getId());
                    payload.put("last_known_count", totalItemsCount - 1);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                RestClient.post(getActivity(), Endpoints.GET_LOCALITY_MESSAGES_PAGINATION, payload, new BaseJsonHttpResponseHandler<JSONArray>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                        paginatedMessages.clear();
                        if (response.length() > 0) {
                            for (int i = response.length() - 1; i >= 0; i--) {
                                try {
                                    JSONObject o = response.getJSONObject(i);
                                    User sender = new User(o.getJSONObject("user"));
                                    Message message = new Message(sender, o);
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
                        System.out.println(rawJsonData);
                        return new JSONArray(rawJsonData);
                    }
                });
            }
        });
    }

    private void setMessageOnLongClick(MessagesListAdapter<Message> adapter) {
        adapter.setOnMessageViewLongClickListener((view, message) -> EncodingUtils.copyText(getActivity(), message));
    }

    private void setupAdapter(View view) {
        MessagesList messagesList = view.findViewById(R.id.messages_list);
        adapter = new MessagesListAdapter<>(LocalPrefs.getID(getActivity()), getIncomingHolder(), loadImage());

        setMessageOnLongClick(adapter);
        setLoadMoreListener(adapter);
        setSendMessageListener(adapter);

        messagesList.setAdapter(adapter);

        RestClient.post(getActivity(), Endpoints.GET_NEARBY_COUNT, JSONUtils.getIdPayload(getActivity()),
                new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                        try {
                            String localityName = response.getString("locality");
                            int nearbyUsers = response.getInt("count");
                            String count = nearbyUsers + " eile anseo";

                            // set title and subtitle
                            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

                            if (actionBar != null) {
                                actionBar.setTitle(localityName);
                                actionBar.setSubtitle(count);
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
    }

    private void loadMessages() {
        setupAdapter(view);
        RestClient.post(getActivity(), Endpoints.GET_LOCALITY_MESSAGES, JSONUtils.getIdPayload(getActivity()),
                new BaseJsonHttpResponseHandler<JSONArray>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                        messages.clear();
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject userMessage = response.getJSONObject(i);
                                String id = userMessage.getJSONObject("_id").getString("$oid");
                                String messageContent = EncodingUtils.decodeText(userMessage.getString("message"));
                                long timeSent = userMessage.getLong("time");

                                User user = new User(userMessage.getJSONObject("user"));
                                Message message = new Message(id, user, timeSent, messageContent);
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
    }

    private ImageLoader loadImage() {
        return (imageView, url) -> {
            // can only use Facebook to sign up so use the embedded id in the url
            final String id = url.split("/")[3];

            if (!CachingUtil.doesImageExist(getActivity(), id)) {
                new GetImage(new NetworkCallback<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        Bitmap croppedImage = BitmapUtils.getCroppedCircle(response);
                        Bitmap scaledAvatar = BitmapUtils.scaleBitmap(croppedImage, BitmapUtils.BITMAP_SIZE_SMALL);
                        imageView.setImageBitmap(scaledAvatar);
                        CachingUtil.cacheImage(getActivity(), id, scaledAvatar);
                    }

                    @Override
                    public void onFailure() {
                        System.out.println("dl failure on chat pic");
                    }
                }, url, true).execute();
            } else {
                Bitmap cachedImage = CachingUtil.getCachedImage(getActivity(), id);
                imageView.setImageBitmap(cachedImage);
            }

            imageView.setOnLongClickListener(v -> {
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
                return false;
            });
        };
    }
}
