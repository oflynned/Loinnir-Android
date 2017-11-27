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
import com.syzible.loinnir.persistence.LocalPrefs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 07/05/2017.
 */

public class ConversationsListFrag extends Fragment implements
        DialogsListAdapter.OnDialogClickListener<Conversation>,
        DialogsListAdapter.OnDialogLongClickListener<Conversation> {

    private ProgressBar progressBar;

    private ArrayList<Conversation> conversations = new ArrayList<>();
    private DialogsListAdapter<Conversation> dialogsListAdapter;
    private DialogsList dialogsList;
    private JSONArray response;
    private boolean hasResponseContent = false;

    private BroadcastReceiver newPartnerMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), BroadcastFilters.new_partner_message.toString()))
                loadConversationPreviews();
        }
    };

    private BroadcastReceiver onBlockEnactedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), BroadcastFilters.block_enacted.toString()))
                loadConversationPreviews();
        }
    };

    private void loadConversationPreviews() {
        RestClient.post(getActivity(), Endpoints.GET_PAST_CONVERSATION_PREVIEWS,
                JSONUtils.getIdPayload(getActivity()),
                new BaseJsonHttpResponseHandler<JSONArray>() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONArray response) {
                        loadMessages(response);
                        setListLayout();
                        NotificationUtils.dismissNotifications(getActivity(), conversations);

                        progressBar.setVisibility(View.INVISIBLE);
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

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, Bundle savedInstanceState) {
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setTitle(R.string.app_name);
            actionBar.setSubtitle(null);
        }

        View view;
        if (response.length() > 0) {
            hasResponseContent = true;
            view = inflater.inflate(R.layout.conversations_list_frag, container, false);
            progressBar = view.findViewById(R.id.conversations_list_progress_bar);
            dialogsList = view.findViewById(R.id.conversations_list);
            dialogsListAdapter = new DialogsListAdapter<>(loadImage());

            loadMessages(response);
        } else {
            view = inflater.inflate(R.layout.no_past_conversations_fragment, container, false);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (hasResponseContent) {
            progressBar.setVisibility(View.VISIBLE);
            Collections.reverse(conversations);
            loadConversationPreviews();
        }

        getActivity().registerReceiver(newPartnerMessageReceiver,
                new IntentFilter(BroadcastFilters.new_partner_message.toString()));
        getActivity().registerReceiver(onBlockEnactedReceiver,
                new IntentFilter(BroadcastFilters.block_enacted.toString()));
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(newPartnerMessageReceiver);
        getActivity().unregisterReceiver(onBlockEnactedReceiver);
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

                System.out.println(sender.getName() + " " + message.getText());

                conversations.add(conversation);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDialogClick(Conversation conversation) {
        for (int i = 0; i < conversations.size(); i++)
            if (conversations.get(i).getId().equals(conversation.getId()))
                conversations.get(i).setUnreadCount(0);

        User partner = (User) conversation.getUsers().get(0);
        PartnerConversationFrag frag = new PartnerConversationFrag().setPartner(partner);
        MainActivity.setFragmentBackstack(getFragmentManager(), frag);
    }

    private void setListLayout() {
        if (conversations.size() > 0) {
            dialogsListAdapter.setItems(conversations);
            dialogsListAdapter.setOnDialogClickListener(ConversationsListFrag.this);
            dialogsListAdapter.setOnDialogLongClickListener(ConversationsListFrag.this);
            dialogsList.setAdapter(dialogsListAdapter);
            dialogsList.scrollToPosition(conversations.size() - 1);
        } else {
            MainActivity.removeFragment(getFragmentManager());
            MainActivity.setFragment(getFragmentManager(), new NoConversationFrag());
        }
    }

    @Override
    public void onDialogLongClick(final Conversation conversation) {
        User blockee = null;
        for (IUser user : conversation.getUsers())
            if (!user.getId().equals(LocalPrefs.getID(getActivity())))
                blockee = (User) user;

        final User finalBlockee = blockee;
        new AlertDialog.Builder(getActivity())
                .setTitle("Cosc a Chur ar " + LanguageUtils.lenite(blockee.getForename()) + "?")
                .setMessage("Má chuireann tú cosc ar úsáideoir araile, ní féidir leat nó " +
                        LanguageUtils.getPrepositionalForm("le", blockee.getForename()) + " dul i dteagmháil lena chéile. " +
                        "Bain úsáid as seo amháin go bhfuil tú cinnte nach dteastaíonn uait faic a chloisteáil a thuilleadh ón úsáideoir seo. " +
                        "Cuir cosc ar dhuine má imrítear bulaíocht ort, nó mura dteastaíonn uait tuilleadh teagmhála. " +
                        "Má athraíonn tú do mheabhair ar ball, téigh chuig na socruithe agus bainistigh cé atá curtha ar cosc.")
                .setPositiveButton("Cuir cosc i bhfeidhm", (dialog, which) -> RestClient.post(getActivity(), Endpoints.BLOCK_USER,
                        JSONUtils.getPartnerInteractionPayload(finalBlockee, getActivity()),
                        new BaseJsonHttpResponseHandler<JSONObject>() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                DisplayUtils.generateSnackbar(getActivity(), "Cuireadh cosc ar " + LanguageUtils.lenite(finalBlockee.getForename()) + ".");
                                loadConversationPreviews();
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {

                            }

                            @Override
                            protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                                return new JSONObject(rawJsonData);
                            }
                        }))
                .setNegativeButton("Ná cuir", null)
                .show();
    }

    private ImageLoader loadImage() {
        return (imageView, url) -> {
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
        };
    }
}
