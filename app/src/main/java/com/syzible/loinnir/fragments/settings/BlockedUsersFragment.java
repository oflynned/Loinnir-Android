package com.syzible.loinnir.fragments.settings;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.SettingsActivity;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.services.CachingUtil;
import com.syzible.loinnir.utils.BitmapUtils;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EmojiUtils;
import com.syzible.loinnir.utils.JSONUtils;
import com.syzible.loinnir.utils.LanguageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import mehdi.sakout.fancybuttons.FancyButton;

/**
 * Created by ed on 29/05/2017.
 */

public class BlockedUsersFragment extends Fragment {
    private ArrayList<User> blockedUsers = new ArrayList<>();
    private ArrayList<String> blockedUserIds = new ArrayList<>();
    private ListView listView;
    private BlockedUsersAdapter adapter;
    private int count;

    private View view;

    private interface OnFinishedPolling {
        void onDone();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        if (count > 0) {
            view = inflater.inflate(R.layout.blocked_users_fragment, container, false);
            loadUsers();

        } else {
            view = inflater.inflate(R.layout.no_blocked_users_fragment, container, false);

            TextView emoji = (TextView) view.findViewById(R.id.no_blocked_users_emoji);
            emoji.setText(EmojiUtils.getEmoji(EmojiUtils.COOL));
        }

        return view;
    }

    private void loadUsers() {
        listView = (ListView) view.findViewById(R.id.blocked_users_list_view);
        final OnFinishedPolling callback = new OnFinishedPolling() {
            @Override
            public void onDone() {
                adapter = new BlockedUsersAdapter(blockedUsers, getActivity());
                listView.setAdapter(adapter);
            }
        };

        for (int i = 0; i < blockedUserIds.size(); i++) {
            JSONObject payload = new JSONObject();
            try {
                payload.put("fb_id", blockedUserIds.get(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            final int finalIndex = i;
            RestClient.post(getActivity(), Endpoints.GET_USER, payload, new BaseJsonHttpResponseHandler<JSONObject>() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                    try {
                        blockedUsers.add(new User(response));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (finalIndex == blockedUserIds.size() - 1)
                        callback.onDone();
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
    }

    private class BlockedUsersAdapter extends ArrayAdapter<User> {

        private Context context;
        private User blockedUser;
        private ViewHolder viewHolder;
        private View view;
        private ArrayList<User> blockedUsers = new ArrayList<>();

        BlockedUsersAdapter(ArrayList<User> blockedUsers, Context context) {
            super(context, R.layout.blocked_user, blockedUsers);
            this.blockedUsers = blockedUsers;
            this.context = context;
        }

        @NonNull
        @Override
        public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            this.blockedUser = blockedUsers.get(position);

            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.blocked_user, parent, false);

                viewHolder.profilePicture = (ImageView) convertView.findViewById(R.id.blocked_users_profile_picture);
                viewHolder.name = (TextView) convertView.findViewById(R.id.blocked_users_name);
                viewHolder.unblockUser = (FancyButton) convertView.findViewById(R.id.unblock_user_button);

                view = convertView;
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
                view = convertView;
            }

            viewHolder.name.setText(blockedUser.getName());
            viewHolder.unblockUser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RestClient.post(context, Endpoints.UNBLOCK_USER, JSONUtils.getPartnerInteractionPayload(blockedUser, context), new BaseJsonHttpResponseHandler<JSONObject>() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                            DisplayUtils.generateSnackbar(getActivity(), "Baineadh an cosc de " + LanguageUtils.lenite(blockedUser.getName()));

                            // now remove the selected blocked user and invalidate the list
                            blockedUsers.remove(position);
                            ((BaseAdapter) listView.getAdapter()).notifyDataSetChanged();

                            if (blockedUsers.size() == 0) {
                                // now refresh the fragment to display the "no blocked users" frag if required
                                SettingsActivity.removeFragment(getFragmentManager());
                                SettingsActivity.setFragmentBackstack(getFragmentManager(), new BlockedUsersFragment().setCount(0));
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

            if (!CachingUtil.doesImageExist(getActivity(), blockedUser.getId())) {
                new GetImage(new NetworkCallback<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap response) {
                        Bitmap avatar = BitmapUtils.getCroppedCircle(response);
                        Bitmap scaledAvatar = BitmapUtils.scaleBitmap(avatar, BitmapUtils.BITMAP_SIZE_SMALL);
                        viewHolder.profilePicture.setImageBitmap(scaledAvatar);
                        CachingUtil.cacheImage(getActivity(), blockedUser.getId(), scaledAvatar);
                    }

                    @Override
                    public void onFailure() {
                        DisplayUtils.generateSnackbar(getActivity(), "This earáid amach " + EmojiUtils.getEmoji(EmojiUtils.SAD));
                    }
                }, blockedUser.getAvatar(), true).execute();
            } else {
                Bitmap avatar = CachingUtil.getCachedImage(getActivity(), blockedUser.getId());
                Bitmap scaledAvatar = BitmapUtils.scaleBitmap(avatar, BitmapUtils.BITMAP_SIZE_SMALL);
                viewHolder.profilePicture.setImageBitmap(scaledAvatar);
            }

            return view;
        }
    }

    private static class ViewHolder {
        ImageView profilePicture;
        TextView name;
        FancyButton unblockUser;
    }

    public BlockedUsersFragment setCount(int count) {
        this.count = count;
        return this;
    }

    public BlockedUsersFragment setBlockedUsers(ArrayList<String> blockedUserIds) {
        this.blockedUserIds = blockedUserIds;
        return this;
    }
}
