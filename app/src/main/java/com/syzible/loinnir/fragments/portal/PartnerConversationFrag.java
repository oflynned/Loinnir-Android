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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.messages.MessageInput;
import com.stfalcon.chatkit.messages.MessagesList;
import com.stfalcon.chatkit.messages.MessagesListAdapter;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.persistence.LocalCacheDatabase;
import com.syzible.loinnir.persistence.LocalCacheDatabaseHelper;
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
import com.syzible.loinnir.persistence.LocalPrefs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 07/05/2017.
 */

public class PartnerConversationFrag extends Fragment {
    private User partner;
    private View view;

    private ArrayList<Message> messages = new ArrayList<>();
    ArrayList<Message> paginatedMessages = new ArrayList<>();

    private MessagesListAdapter<Message> adapter;
    private BroadcastReceiver newPartnerMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BroadcastFilters.new_partner_message.toString())) {
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
            if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE"))
                if (NetworkAvailableService.isInternetAvailable(getActivity()))
                    NetworkAvailableService.syncCachedData(getActivity());
        }
    };

    private BroadcastReceiver onBlockEnactedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BroadcastFilters.block_enacted.toString())) {
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
            if (intent.getAction().equals(BroadcastFilters.message_seen.toString())) {
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.conversation_frag, container, false);
        setupAdapter(view);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(partner.getName());
            actionBar.setSubtitle(formatSubtitle());
        }

        return view;
    }

    private String formatSubtitle() {
        return partner.getLocality() + ", " + partner.getCounty();
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().registerReceiver(newPartnerMessageReceiver,
                new IntentFilter(BroadcastFilters.new_partner_message.toString()));
        getActivity().registerReceiver(onBlockEnactedReceiver,
                new IntentFilter(BroadcastFilters.block_enacted.toString()));
        getActivity().registerReceiver(onSeenReceiver,
                new IntentFilter(BroadcastFilters.message_seen.toString()));
        getActivity().registerReceiver(internetAvailableReceiver,
                new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        loadMessages();
        NotificationUtils.dismissNotification(getActivity(), partner);
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(newPartnerMessageReceiver);
        getActivity().unregisterReceiver(onBlockEnactedReceiver);
        getActivity().unregisterReceiver(onSeenReceiver);
        getActivity().unregisterReceiver(internetAvailableReceiver);
    }

    private void setMessageInputListener(final MessagesListAdapter<Message> adapter) {
        MessageInput messageInput = (MessageInput) view.findViewById(R.id.message_input);
        messageInput.setInputListener(new MessageInput.InputListener() {
            @Override
            public boolean onSubmit(final CharSequence input) {
                final String messageContent = input.toString().trim();
                if (NetworkAvailableService.isInternetAvailable(getActivity())) {
                    RestClient.post(getActivity(), Endpoints.GET_USER, JSONUtils.getIdPayload(getActivity()),
                            new BaseJsonHttpResponseHandler<JSONObject>() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                    try {
                                        final User me = new User(response);

                                        JSONObject payload = new JSONObject();
                                        payload.put("my_id", me.getId());
                                        payload.put("partner_id", partner.getId());

                                        RestClient.post(getActivity(), Endpoints.GET_PARTNER_MESSAGES_COUNT,
                                                JSONUtils.getPartnerInteractionPayload(partner.getId(), getActivity()),
                                                new BaseJsonHttpResponseHandler<JSONObject>() {
                                                    @Override
                                                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                                        try {
                                                            if (response.getInt("count") == 0)
                                                                matchPartner(partner);

                                                            Message message = new Message(LocalPrefs.getID(getActivity()),
                                                                    me, System.currentTimeMillis(), messageContent);
                                                            adapter.addToStart(message, true);

                                                            // send to server
                                                            JSONObject messagePayload = new JSONObject();
                                                            messagePayload.put("from_id", LocalPrefs.getID(getActivity()));
                                                            messagePayload.put("to_id", partner.getId());
                                                            messagePayload.put("message", EncodingUtils.encodeText(message.getText().trim()));

                                                            RestClient.post(getActivity(), Endpoints.SEND_PARTNER_MESSAGE, messagePayload, new BaseJsonHttpResponseHandler<JSONObject>() {
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
                    String myId = LocalPrefs.getID(getActivity());
                    Message cachedMessage = new Message(myId, new User(myId), System.currentTimeMillis(), messageContent);
                    adapter.addToStart(cachedMessage, true);
                }
                return true;
            }
        });
    }

    private void cacheItem(String messageContent) {
        LocalCacheDatabase.CachedItem cachedItem = new LocalCacheDatabase.CachedItem(messageContent, partner.getId(), getActivity());
        LocalCacheDatabaseHelper.cacheItem(cachedItem);
        LocalCacheDatabaseHelper.printCachedItemsContents(getActivity());
        DisplayUtils.generateToast(getActivity(), "Easpa rochtain idirlín, seolfar do theachtaireacht ar ball");
    }

    private void setLoadMoreListener(final MessagesListAdapter<Message> adapter) {
        adapter.setLoadMoreListener(new MessagesListAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                if (messages.size() > 0) {
                    JSONObject payload = new JSONObject();
                    try {
                        payload.put("my_id", LocalPrefs.getID(getActivity()));
                        payload.put("partner_id", partner.getId());
                        payload.put("last_known_count", totalItemsCount - 1);

                        Message oldestMessage = paginatedMessages.size() == 0 ? messages.get(messages.size() - 1) : paginatedMessages.get(paginatedMessages.size() - 1);
                        payload.put("oldest_message_id", oldestMessage.getId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    RestClient.post(getActivity(), Endpoints.GET_PARTNER_MESSAGES_PAGINATION, payload, new BaseJsonHttpResponseHandler<JSONArray>() {
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
            }
        });
    }

    private void setLongClickListener(MessagesListAdapter<Message> adapter) {
        adapter.setOnMessageViewLongClickListener(new MessagesListAdapter.OnMessageViewLongClickListener<Message>() {
            @Override
            public void onMessageViewLongClick(View view, final Message message) {
                // should not be able to block yourself
                if (!message.getUser().getId().equals(LocalPrefs.getID(getActivity())))
                    DisplayUtils.generateBlockDialog(getActivity(), (User) message.getUser(), new DisplayUtils.OnCallback() {
                        @Override
                        public void onCallback() {
                            DisplayUtils.generateSnackbar(getActivity(), "Cuireadh cosc go rathúil ar " + LanguageUtils.lenite(((User) message.getUser()).getForename()));

                            RestClient.post(getActivity(), Endpoints.GET_USER, JSONUtils.getIdPayload(getActivity()), new BaseJsonHttpResponseHandler<JSONObject>() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                    try {
                                        JSONArray blockedUsers = response.getJSONArray("blocked");
                                        for (int i = 0; i < blockedUsers.length(); i++) {
                                            if (blockedUsers.getString(i).equals(partner.getId())) {
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
                    });
            }
        });
    }

    private MessageHolders getViewHolder() {
        return new MessageHolders()
                .setOutcomingTextConfig(SentMessageHolder.class, R.layout.sent_message_holder);
    }

    private void setupAdapter(View view) {
        adapter = new MessagesListAdapter<>(LocalPrefs.getID(getActivity()), loadImage());

        setLongClickListener(adapter);
        setLoadMoreListener(adapter);
        setMessageInputListener(adapter);

        MessagesList messagesList = (MessagesList) view.findViewById(R.id.messages_list);
        messagesList.setAdapter(adapter);
    }

    private void loadMessages() {
        setupAdapter(view);
        RestClient.post(getActivity(), Endpoints.GET_PARTNER_MESSAGES,
                JSONUtils.getPartnerInteractionPayload(partner, getActivity()),
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

                        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.conversations_progress_bar);
                        progressBar.setVisibility(View.GONE);
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
        RestClient.post(getActivity(), Endpoints.MARK_PARTNER_MESSAGES_SEEN,
                JSONUtils.getPartnerInteractionPayload(partner, getActivity()),
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
        return new ImageLoader() {
            @Override
            public void loadImage(final ImageView imageView, final String url) {
                // can only use Facebook to sign up so use the embedded id in the url
                final String fileName = url.split("/")[3];

                if (!CachingUtil.doesImageExist(getActivity(), fileName)) {
                    new GetImage(new NetworkCallback<Bitmap>() {
                        @Override
                        public void onResponse(Bitmap response) {
                            Bitmap croppedImage = BitmapUtils.getCroppedCircle(response);
                            final Bitmap scaledAvatar = BitmapUtils.scaleBitmap(croppedImage, BitmapUtils.BITMAP_SIZE_SMALL);
                            CachingUtil.cacheImage(getActivity(), fileName, scaledAvatar);
                            imageView.setImageBitmap(scaledAvatar);
                            imageView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    ProfileFrag profileFrag = new ProfileFrag().setPartner(partner).setBitmap(scaledAvatar);
                                    MainActivity.setFragmentBackstack(getFragmentManager(), profileFrag);
                                }
                            });
                        }

                        @Override
                        public void onFailure() {
                            System.out.println("dl failure on chat pic");
                        }
                    }, url, true).execute();
                } else {
                    final Bitmap cachedImage = CachingUtil.getCachedImage(getActivity(), fileName);
                    imageView.setImageBitmap(cachedImage);
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ProfileFrag profileFrag = new ProfileFrag().setPartner(partner).setBitmap(cachedImage);
                            MainActivity.setFragmentBackstack(getFragmentManager(), profileFrag);
                        }
                    });
                }
            }
        };
    }

    private void matchPartner(User partner) {
        RestClient.post(getActivity(), Endpoints.SUBSCRIBE_TO_PARTNER,
                JSONUtils.getPartnerInteractionPayload(partner, getActivity()),
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
