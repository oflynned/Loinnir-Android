package com.syzible.loinnir.fragments.portal;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.utils.BitmapUtils;
import com.syzible.loinnir.utils.LocalStorage;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import mehdi.sakout.fancybuttons.FancyButton;

import static com.syzible.loinnir.utils.Constants.getCountyFileName;

/**
 * Created by ed on 07/05/2017.
 */

public class RouletteOutcomeFrag extends Fragment {

    private User partner;
    private Bitmap profilePic;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.roulette_outcome_frag, container, false);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.app_name);
        ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(null);

        ImageView profilePictureImageView = (ImageView) view.findViewById(R.id.roulette_partner_profile_pic);
        TextView usernameTextView = (TextView) view.findViewById(R.id.name_text_roulette);
        TextView localityTextView = (TextView) view.findViewById(R.id.locality_text_roulette);
        TextView countyTextView = (TextView) view.findViewById(R.id.county_text_roulette);
        TextView matchWarningTextView = (TextView) view.findViewById(R.id.match_loss_warning_if_no_chat);

        FancyButton backToRouletteButton = (FancyButton) view.findViewById(R.id.back_to_roulette_btn);
        backToRouletteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.removeFragment(getFragmentManager());
                MainActivity.setFragment(getFragmentManager(), new RouletteFrag());
            }
        });

        FancyButton startConversationButton = (FancyButton) view.findViewById(R.id.start_conversation_btn);
        startConversationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PartnerConversationFrag frag = new PartnerConversationFrag().setPartner(partner);

                MainActivity.clearBackstack(getFragmentManager());
                MainActivity.setFragment(getFragmentManager(), frag);
            }
        });

        profilePictureImageView.setImageBitmap(BitmapUtils.getCroppedCircle(profilePic));

        usernameTextView.setText(partner.getName());
        localityTextView.setText(partner.getLocality());
        countyTextView.setText(partner.getCounty());

        String countyFlagFile = getCountyFileName(partner.getCounty());
        int flagDrawable = getResources().getIdentifier(countyFlagFile, "drawable", getActivity().getPackageName());
        ImageView countyFlag = (ImageView) view.findViewById(R.id.county_flag_roulette);
        countyFlag.setImageResource(flagDrawable);

        String outcome = "Is féidir an rúiléid a atriail, ach caillfidh tú an nasc leis an duine seo mura thosaíonn tú comhrá ";
        outcome += partner.isFemale() ? "léi" : "leis";
        matchWarningTextView.setText(outcome);

        return view;
    }

    public RouletteOutcomeFrag setPartner(User partner) {
        this.partner = partner;
        return this;
    }

    public RouletteOutcomeFrag setBitmap(Bitmap profilePic) {
        this.profilePic = profilePic;
        return this;
    }
}
