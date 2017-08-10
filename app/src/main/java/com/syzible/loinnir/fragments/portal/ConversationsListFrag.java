package com.syzible.loinnir.fragments.portal;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.stfalcon.chatkit.commons.ImageLoader;
import com.stfalcon.chatkit.commons.models.IUser;
import com.stfalcon.chatkit.dialogs.DialogsList;
import com.stfalcon.chatkit.dialogs.DialogsListAdapter;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.Conversation;
import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.services.CachingUtil;
import com.syzible.loinnir.services.NotificationUtils;
import com.syzible.loinnir.utils.BitmapUtils;
import com.syzible.loinnir.utils.BroadcastFilters;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.JSONUtils;
import com.syzible.loinnir.utils.LanguageUtils;
import com.syzible.loinnir.utils.LocalStorage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.zip.Inflater;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 07/05/2017.
 */

public class ConversationsListFrag extends Fragment implements
        DialogsListAdapter.OnDialogClickListener<Conversation>,
        DialogsListAdapter.OnDialogLongClickListener<Conversation> {

    private ArrayList<Conversation> conversations = new ArrayList<>();
    private DialogsListAdapter<Conversation> dialogsListAdapter;
    private DialogsList dialogsList;
    private JSONArray response;

    private boolean shouldShowMessages = false;
    private BroadcastReceiver newPartnerMessageConversationReceover = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BroadcastFilters.new_partner_message.toString())) {
                RestClient.post(getActivity(), Endpoints.GET_PAST_CONVERSATION_PREVIEWS,
                        JSONUtils.getIdPayload(getActivity()),
                        new BaseJsonHttpResponseHandler<JSONArray>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                                loadMessages(response);
                                NotificationUtils.dismissNotifications(getActivity(), conversations);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONArray errorResponse) {
                                System.out.println(rawJsonData);
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
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, Bundle savedInstanceState) {
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.app_name);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(null);

        shouldShowMessages = response.length() > 0;

        View view;
        if (shouldShowMessages) {
            view = inflater.inflate(R.layout.conversations_list_frag, container, false);

            dialogsList = (DialogsList) view.findViewById(R.id.conversations_list);
            dialogsListAdapter = new DialogsListAdapter<>(loadImage());

            loadMessages(response);
        } else {
            view = inflater.inflate(R.layout.no_past_conversations_fragment, container, false);
        }

        return view;
    }

    @Override
    public void onResume() {
        if (shouldShowMessages) {
            NotificationUtils.dismissNotifications(getActivity(), conversations);
            RestClient.post(getActivity(), Endpoints.GET_PAST_CONVERSATION_PREVIEWS,
                    JSONUtils.getIdPayload(getActivity()),
                    new BaseJsonHttpResponseHandler<JSONArray>() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                            loadMessages(response);
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

        getActivity().registerReceiver(newPartnerMessageConversationReceover,
                new IntentFilter(BroadcastFilters.new_partner_message.toString()));

        super.onResume();
    }

    @Override
    public void onPause() {
        getActivity().unregisterReceiver(newPartnerMessageConversationReceover);
        super.onPause();
    }

    public ConversationsListFrag setResponse(JSONArray response) {
        this.response = response;
        return this;
    }

    private void loadMessages(JSONArray response) {
        conversations.clear();
        for (int i = 0; i < response.length(); i++) {
            try {
                JSONObject o = response.getJSONObject(i);
                int unreadCount = o.getInt("count");
                User sender = new User(o.getJSONObject("user"));
                Message message = new Message(sender, o.getJSONObject("message"));
                Conversation conversation = new Conversation(sender, message, unreadCount);

                conversations.add(conversation);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        dialogsListAdapter.setItems(conversations);
        dialogsListAdapter.setOnDialogClickListener(ConversationsListFrag.this);
        dialogsListAdapter.setOnDialogLongClickListener(ConversationsListFrag.this);
        dialogsList.setAdapter(dialogsListAdapter);
    }

    @Override
    public void onDialogClick(Conversation conversation) {
        for (int i = 0; i < conversations.size(); i++) {
            if (conversations.get(i).getId().equals(conversation.getId())) {
                conversations.get(i).setUnreadCount(0);
            }
        }

        User partner = (User) conversation.getUsers().get(0);
        PartnerConversationFrag frag = new PartnerConversationFrag().setPartner(partner);
        MainActivity.setFragmentBackstack(getFragmentManager(), frag);
    }

    @Override
    public void onDialogLongClick(final Conversation conversation) {
        User blockee = null;
        for (IUser user : conversation.getUsers()) {
            if (!user.getId().equals(LocalStorage.getID(getActivity()))) {
                blockee = (User) user;
            }
        }

        final User finalBlockee = blockee;
        new AlertDialog.Builder(getActivity())
                .setTitle("Cosc a Chur ar " + LanguageUtils.lenite(blockee.getForename()) + "?")
                .setMessage("Má chuireann tú cosc ar úsáideoir araile, ní féidir leat nó le " + blockee + " dul i dteagmháil lena chéile. " +
                        "Bain úsáid as seo amháin go bhfuil tú cinnte nach dteastaíonn uait faic a chloisteáil a thuilleadh ón úsáideoir seo. " +
                        "Cur cosc ar dhuine má imrítear bulaíocht ort, nó mura dteastaíonn uait tuilleadh teagmhála. " +
                        "Má athraíonn tú do mheabhair ar ball, téigh chuig na socruithe agus bainistigh cé atá curtha ar cosc.")
                .setPositiveButton("Cur cosc i bhfeidhm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        RestClient.post(getActivity(), Endpoints.BLOCK_USER,
                                JSONUtils.getPartnerInteractionPayload(finalBlockee, getActivity()),
                                new BaseJsonHttpResponseHandler<JSONObject>() {
                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                        DisplayUtils.generateSnackbar(getActivity(), "Cuireadh cosc ar " + LanguageUtils.lenite(finalBlockee.getForename()) + ".");

                                        conversations.remove(which + 1);

                                        if (conversations.size() > 0) {
                                            dialogsListAdapter.setItems(conversations);
                                            dialogsListAdapter.setOnDialogClickListener(ConversationsListFrag.this);
                                            dialogsListAdapter.setOnDialogLongClickListener(ConversationsListFrag.this);
                                            dialogsList.setAdapter(dialogsListAdapter);
                                        } else {
                                            MainActivity.removeFragment(getFragmentManager());
                                            MainActivity.setFragment(getFragmentManager(), new NoConversationFrag());
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
                })
                .setNegativeButton("Ná cur", null)
                .show();
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
                            Bitmap scaledAvatar = BitmapUtils.generateMetUserAvatar(response);
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
