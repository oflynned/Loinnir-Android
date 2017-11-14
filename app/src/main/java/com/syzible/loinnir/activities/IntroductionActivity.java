package com.syzible.loinnir.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.ContextThemeWrapper;

import com.syzible.loinnir.R;
import com.syzible.loinnir.persistence.Constants;
import com.syzible.loinnir.utils.DisplayUtils;
import com.syzible.loinnir.utils.EmojiUtils;
import com.syzible.loinnir.utils.FacebookUtils;
import com.syzible.loinnir.persistence.LocalPrefs;

import agency.tango.materialintroscreen.MaterialIntroActivity;
import agency.tango.materialintroscreen.SlideFragment;
import agency.tango.materialintroscreen.SlideFragmentBuilder;

/**
 * Created by ed on 11/06/2017.
 */

public class IntroductionActivity extends MaterialIntroActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FacebookUtils.hasExistingToken(this)) {
            this.finish();
            startActivity(new Intent(this, MainActivity.class));
        } else {
            if (LocalPrefs.isFirstRunCompleted(this)) {
                this.finish();
                startActivity(new Intent(this, AuthenticationActivity.class));
            } else {
                new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.DarkerDialogAppTheme))
                        .setTitle("Téarmaí Seirbhíse")
                        .setMessage("De bheith ag glacadh leis na téarmaí seirbhíse, glacann tú go bhfuil na siad léite agat ar an suíomh idirlín, agus go ndéanfar iarracht iad a choimeád san intinn nuair a bhíonn an aip in úsáid.")
                        .setPositiveButton("Aontaím", (dialog, which) -> {
                            LocalPrefs.setUserAgreementsVersion(getApplicationContext(), Constants.USER_AGREEMENT_VERSION);
                            DisplayUtils.generateToast(IntroductionActivity.this, "Go raibh maith agat! " + EmojiUtils.getEmoji(EmojiUtils.HEART_EYES));
                        })
                        .setNegativeButton("Ní Aontaím", (dialog, which) -> IntroductionActivity.this.finish())
                        .show();

                addSlide(introductionSlide());
                addSlide(locationSlide());
                addSlide(chatSlide());
                addSlide(permissionsSlide());
                addSlide(getStartedSlide());
            }
        }
    }

    @Override
    public void onFinish() {
        super.onFinish();
        Context context = IntroductionActivity.this;
        LocalPrefs.setBooleanPref(LocalPrefs.Pref.first_run_completed, true, context);
        startActivity(new Intent(context, AuthenticationActivity.class));
    }

    private SlideFragment introductionSlide() {
        return new SlideFragmentBuilder()
                .backgroundColor(R.color.amber500)
                .buttonsColor(R.color.blue500)
                .image(R.drawable.logo_small)
                .title("Fáilte go dtí Loinnir")
                .description("Ag fionnadh pobail don Ghaeilge.\nFionn. Nasc. Braith.")
                .build();
    }

    private SlideFragment locationSlide() {
        return new SlideFragmentBuilder()
                .backgroundColor(R.color.blue500)
                .buttonsColor(R.color.amber500)
                .image(R.drawable.ic_map_pin_white)
                .title("Ceantar")
                .description("Féach umat cá bhfuil an Ghaeilge ar léarscáil agus labhair i seomra cainte atá bunaithe ar an áit a bhfuil tú lonnaithe.")
                .build();
    }

    private SlideFragment chatSlide() {
        return new SlideFragmentBuilder()
                .backgroundColor(R.color.amber500)
                .buttonsColor(R.color.blue500)
                .image(R.drawable.ic_conversation_white)
                .title("I mBun Allagair")
                .description("Déan rúiléid chun bualadh le daoine nua timpeall na cruinne, agus fionn pobal le grá don Ghaeilge.")
                .build();
    }

    private SlideFragment permissionsSlide() {
        return new SlideFragmentBuilder()
                .backgroundColor(R.color.blue500)
                .buttonsColor(R.color.amber500)
                .image(R.drawable.ic_permissions_white)
                .title("Ceadanna")
                .description("Iarraimid ort glacadh le ceadanna an cheantair ionas go mbeidh an eispéireas is fearr agat don aip.")
                .neededPermissions(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                })
                .build();
    }

    private SlideFragment getStartedSlide() {
        return new SlideFragmentBuilder()
                .backgroundColor(R.color.amber500)
                .buttonsColor(R.color.blue500)
                .image(R.drawable.ic_facebook_white)
                .title("Cúntas Facebook")
                .description("Tá sé riachtanach cúntas Facebook a bheith agat le síniú isteach ar sheirbhísí Loinnir ionas go mbeidh an eispéireas is fearr agat.")
                .build();
    }

}
