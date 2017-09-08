package com.syzible.loinnir.fragments.settings;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.syzible.loinnir.R;

/**
 * Created by ed on 29/05/2017.
 */

public class AboutLoinnirFragment extends Fragment {

    private ImageView logo;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.about_loinnir_fragment, container, false);

        logo = (ImageView) view.findViewById(R.id.about_app_logo);
        logo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                YoYo.with(Techniques.RubberBand).duration(700).playOn(logo);
            }
        });

        return view;
    }
}
