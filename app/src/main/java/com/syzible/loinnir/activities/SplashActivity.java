package com.syzible.loinnir.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.syzible.loinnir.R;

/**
 * Created by ed on 08/05/2017.
 */

public class SplashActivity extends AppCompatActivity {

    // TODO what needs to be polled during this for the app to function?
    // TODO is this needed?

    // last known location, new messages since last sync

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);
    }
}
