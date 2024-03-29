package com.syzible.loinnir.fragments.portal;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.syzible.loinnir.R;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.utils.BitmapUtils;

import static com.syzible.loinnir.persistence.Constants.getCountyFileName;

/**
 * Created by ed on 07/05/2017.
 */

public class ProfileFrag extends Fragment {
    private User partner;
    private Bitmap profilePic;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_frag, container, false);

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
            actionBar.setTitle(R.string.app_name);
            actionBar.setSubtitle(null);
        }

        ImageView profilePictureImageView = view.findViewById(R.id.roulette_partner_profile_pic);
        TextView usernameTextView = view.findViewById(R.id.name_text_roulette);
        TextView localityTextView = view.findViewById(R.id.locality_text_roulette);
        TextView countyTextView = view.findViewById(R.id.county_text_roulette);

        profilePictureImageView.setImageBitmap(BitmapUtils.getCroppedCircle(profilePic));
        usernameTextView.setText(partner.getName());
        localityTextView.setText(partner.getLocality());
        countyTextView.setText(partner.getCounty());

        String countyFlagFile = getCountyFileName(partner.getCounty());
        int flagDrawable = getResources().getIdentifier(countyFlagFile, "drawable", getActivity().getPackageName());
        ImageView countyFlag = view.findViewById(R.id.county_flag_roulette);
        countyFlag.setImageResource(flagDrawable);

        return view;
    }

    public ProfileFrag setPartner(User partner) {
        this.partner = partner;
        return this;
    }

    public ProfileFrag setBitmap(Bitmap profilePic) {
        this.profilePic = profilePic;
        return this;
    }
}
