package com.syzible.loinnir.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.syzible.loinnir.fragments.settings.SettingsFragment;

public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static void setFragmentBackstack(FragmentManager fragmentManager, Fragment fragment) {
        fragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .addToBackStack(fragment.getClass().getName())
                .commit();
    }

    public static void removeFragment(FragmentManager fragmentManager) {
        fragmentManager.popBackStack();
    }

}
