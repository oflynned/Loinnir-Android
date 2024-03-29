package com.syzible.loinnir.fragments.portal;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.utils.AnimationUtils;

/**
 * Created by ed on 07/05/2017.
 */

public class RouletteLoadingFrag extends Fragment {

    private User partner;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.roulette_loading_frag, container, false);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setTitle(R.string.app_name);
            actionBar.setSubtitle(null);
        }

        ImageView rouletteButton = view.findViewById(R.id.roulette_spinner_button);

        AnimationUtils.rotateView(rouletteButton, true);

        new GetImage(new NetworkCallback<Bitmap>() {
            @Override
            public void onResponse(Bitmap response) {
                final RouletteOutcomeFrag matchFrag = new RouletteOutcomeFrag()
                        .setPartner(partner)
                        .setBitmap(response);

                // show loading screen for at least one second
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.removeFragment(getFragmentManager());
                        MainActivity.setFragmentBackstack(getFragmentManager(), matchFrag);
                    }
                }, 1000);
            }

            @Override
            public void onFailure() {
                System.out.println("Failure");
            }
        }, partner.getAvatar(), true).execute();

        return view;
    }


    public RouletteLoadingFrag setPartner(User partner) {
        this.partner = partner;
        return this;
    }
}
