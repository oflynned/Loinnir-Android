package com.syzible.loinnir.fragments.portal;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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
import com.syzible.loinnir.services.CachingUtil;
import com.syzible.loinnir.utils.BitmapUtils;
import com.syzible.loinnir.utils.BroadcastFilters;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EncodingUtils;
import com.syzible.loinnir.utils.JSONUtils;
import com.syzible.loinnir.utils.LanguageUtils;
import com.syzible.loinnir.utils.LocalStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 07/05/2017.
 */

public class LocalityConversationFrag extends Fragment {

    private View view;
    private ArrayList<Message> messages = new ArrayList<>();
    private MessagesListAdapter<Message> adapter;
    private BroadcastReceiver newLocalityInformationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BroadcastFilters.new_locality_info_update.toString())) {
                RestClient.post(getActivity(), Endpoints.GET_NEARBY_COUNT, JSONUtils.getIdPayload(getActivity()),
                        new BaseJsonHttpResponseHandler<JSONObject>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                try {
                                    String localityName = response.getString("locality");
                                    int nearbyUsers = response.getInt("count");
                                    String localUsers = nearbyUsers + " eile anseo";

                                    // set title and subtitle
                                    ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(localityName);
                                    ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(localUsers);
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.conversation_frag, container, false);
        return view;
    }

    @Override
    public void onResume() {
        loadMessages();
        getActivity().registerReceiver(newLocalityInformationReceiver,
                new IntentFilter(BroadcastFilters.new_locality_info_update.toString()));
        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(newLocalityInformationReceiver);
        super.onPause();
    }

    private void setupAdapter(View view) {
        MessageHolders holdersConfig = new MessageHolders();
        holdersConfig.setIncomingTextConfig(IncomingMessage.class, R.layout.chat_message_layout);

        adapter = new MessagesListAdapter<>(LocalStorage.getID(getActivity()), holdersConfig, loadImage());
        adapter.setOnMessageViewLongClickListener(new MessagesListAdapter.OnMessageViewLongClickListener<Message>() {
            @Override
            public void onMessageViewLongClick(View view, final Message message) {
                // should not be able to block yourself
                if (!message.getUser().getId().equals(LocalStorage.getID(getActivity())))
                    DisplayUtils.generateBlockDialog(getActivity(), (User) message.getUser(), new DisplayUtils.OnCallback() {
                        @Override
                        public void onCallback() {
                            DisplayUtils.generateSnackbar(getActivity(), "Cuireadh cosc go rathúil ar " + LanguageUtils.lenite(((User) message.getUser()).getForename()));
                            loadMessages();
                        }
                    });
            }
        });
        adapter.setLoadMoreListener(new MessagesListAdapter.OnLoadMoreListener() {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                if (messages.size() > 0) {
                    JSONObject payload = new JSONObject();
                    try {
                        payload.put("fb_id", LocalStorage.getID(getActivity()));
                        payload.put("oldest_message_id", messages.get(messages.size() - 1).getId());
                        payload.put("last_known_count", totalItemsCount - 1);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    RestClient.post(getActivity(), Endpoints.GET_LOCALITY_MESSAGES_PAGINATION, payload, new BaseJsonHttpResponseHandler<JSONArray>() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                            if (response.length() > 0) {
                                ArrayList<Message> paginatedMessages = new ArrayList<>();
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
            }
        });

        MessagesList messagesList = (MessagesList) view.findViewById(R.id.messages_list);
        messagesList.setAdapter(adapter);

        MessageInput messageInput = (MessageInput) view.findViewById(R.id.message_input);
        messageInput.setInputListener(new MessageInput.InputListener() {
            @Override
            public boolean onSubmit(final CharSequence input) {
                RestClient.post(getActivity(), Endpoints.GET_USER, JSONUtils.getIdPayload(getActivity()),
                        new BaseJsonHttpResponseHandler<JSONObject>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                try {
                                    final User me = new User(response);
                                    final String messageContent = input.toString().trim();

                                    // send to server
                                    JSONObject messagePayload = new JSONObject();
                                    messagePayload.put("fb_id", LocalStorage.getID(getActivity()));
                                    messagePayload.put("message", EncodingUtils.encodeText(messageContent));

                                    RestClient.post(getActivity(), Endpoints.SEND_LOCALITY_MESSAGE, messagePayload, new BaseJsonHttpResponseHandler<JSONObject>() {
                                        @Override
                                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                            System.out.println("Message submitted to locality (" + me.getLocality() + ")");

                                            RestClient.post(getActivity(), Endpoints.GET_LOCALITY_MESSAGES, JSONUtils.getIdPayload(getActivity()), new BaseJsonHttpResponseHandler<JSONArray>() {
                                                @Override
                                                public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                                                    try {
                                                        JSONObject latestMessage = response.getJSONObject(response.length() - 1);
                                                        User sender = new User(latestMessage.getJSONObject("user"));
                                                        Message message = new Message(latestMessage.getJSONObject("_id").getString("$oid"), sender,
                                                                System.currentTimeMillis(), latestMessage.getString("message"));
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
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                            }

                            @Override
                            protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                return new JSONObject(rawJsonData);
                            }
                        });

                return true;
            }
        });


        RestClient.post(getActivity(), Endpoints.GET_NEARBY_COUNT, JSONUtils.getIdPayload(getActivity()),
                new BaseJsonHttpResponseHandler<JSONObject>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                        try {
                            String localityName = response.getString("locality");
                            int nearbyUsers = response.getInt("count");
                            String localUsers = nearbyUsers + " eile anseo";

                            // set title and subtitle
                            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(localityName);
                            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(localUsers);
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
                                String messageContent = userMessage.getString("message");
                                long timeSent = userMessage.getLong("time");

                                User user = new User(userMessage.getJSONObject("user"));
                                Message message = new Message(id, user, timeSent, messageContent);
                                messages.add(message);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        adapter.addToEnd(messages, true);
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
                            Bitmap scaledAvatar = BitmapUtils.scaleBitmap(croppedImage, BitmapUtils.BITMAP_SIZE_SMALL);
                            imageView.setImageBitmap(scaledAvatar);
                            CachingUtil.cacheImage(getActivity(), fileName, scaledAvatar);
                        }

                        @Override
                        public void onFailure() {
                            System.out.println("dl failure on chat pic");
                        }
                    }, url, true).execute();
                } else {
                    Bitmap cachedImage = CachingUtil.getCachedImage(getActivity(), fileName);
                    imageView.setImageBitmap(cachedImage);
                }
            }
        };
    }
}
