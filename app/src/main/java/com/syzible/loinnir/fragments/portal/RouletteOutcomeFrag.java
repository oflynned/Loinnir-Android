package com.syzible.loinnir.fragments.portal;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.media.Image;
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
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.utils.BitmapUtils;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EmojiUtils;

import mehdi.sakout.fancybuttons.FancyButton;

import static com.syzible.loinnir.persistence.Constants.getCountyFileName;

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
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(R.string.app_name);
            actionBar.setSubtitle(null);
        }

        ImageView profilePictureImageView = view.findViewById(R.id.roulette_partner_profile_pic);
        ImageView profilePictureBadgeView = view.findViewById(R.id.roulette_partner_profile_badge);
        TextView usernameTextView = view.findViewById(R.id.name_text_roulette);
        TextView localityTextView = view.findViewById(R.id.locality_text_roulette);
        TextView countyTextView = view.findViewById(R.id.county_text_roulette);
        TextView matchWarningTextView = view.findViewById(R.id.match_loss_warning_if_no_chat);

        FancyButton backToRouletteButton = view.findViewById(R.id.back_to_roulette_btn);
        backToRouletteButton.setOnClickListener(v -> {
            MainActivity.removeFragment(getFragmentManager());
            MainActivity.setFragment(getFragmentManager(), new RouletteFrag());
        });

        FancyButton startConversationButton = view.findViewById(R.id.start_conversation_btn);
        startConversationButton.setOnClickListener(v -> {
            PartnerConversationFrag frag = new PartnerConversationFrag().setPartner(partner);
            MainActivity.clearBackstack(getFragmentManager());
            MainActivity.setFragmentBackstack(getFragmentManager(), new RouletteFrag());
            MainActivity.setFragmentBackstack(getFragmentManager(), frag);
        });

        profilePictureImageView.setImageBitmap(BitmapUtils.getCroppedCircle(profilePic));

        usernameTextView.setText(partner.getName());
        localityTextView.setText(partner.getLocality());
        countyTextView.setText(partner.getCounty());

        String countyFlagFile = getCountyFileName(partner.getCounty());
        int flagDrawable = getResources().getIdentifier(countyFlagFile, "drawable", getActivity().getPackageName());
        ImageView countyFlag = (ImageView) view.findViewById(R.id.county_flag_roulette);
        countyFlag.setImageResource(flagDrawable);

        String outcome = "Is féidir an rúiléid a atriail, " +
                "ach caillfidh tú an nasc leis an duine seo mura thosaíonn tú comhrá " +
                (partner.isFemale() ? "léi" : "leis");
        matchWarningTextView.setText(outcome);

        // Dónal, me
        if (partner.getId().equals("10207354314614509") || partner.getId().equals("1433224973407916")) {
            profilePictureBadgeView.setOnClickListener(v -> DisplayUtils.generateToast(getActivity(),
                    "Ball den fhoireann Loinnir " + EmojiUtils.getEmoji(EmojiUtils.COOL)));
        } else {
            profilePictureBadgeView.setVisibility(View.GONE);
        }

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
