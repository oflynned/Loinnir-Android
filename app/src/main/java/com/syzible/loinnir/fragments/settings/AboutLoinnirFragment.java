package com.syzible.loinnir.fragments.settings;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.syzible.loinnir.R;

/**
 * Created by ed on 29/05/2017.
 */

public class AboutLoinnirFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.about_loinnir_fragment, container, false);
    }
}
