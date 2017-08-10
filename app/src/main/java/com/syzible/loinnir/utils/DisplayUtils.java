package com.syzible.loinnir.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.support.design.widget.Snackbar;
import android.widget.Toast;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.fragments.portal.ConversationsListFrag;
import com.syzible.loinnir.fragments.portal.NoConversationFrag;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.User;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.util.LangUtils;

/**
 * Created by ed on 13/05/2017.
 */

public class DisplayUtils {

    public static void generateToast(Activity activity, String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    public static void generateSnackbar(Activity activity, String message) {
        Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .show();
    }

    public interface OnCallback {
        void onCallback();
    }

    public static AlertDialog generateBlockDialog(final Activity activity, final User blockee, final OnCallback callback) {
        return new AlertDialog.Builder(activity)
                .setTitle("Cosc a Chur " + LanguageUtils.getPrepositionalForm("ar", LanguageUtils.lenite(blockee.getForename())) + "?")
                .setMessage("Má chuireann tú cosc ar úsáideoir araile, ní féidir leat nó " + LanguageUtils.getPrepositionalForm("le", blockee.getForename()) + " dul i dteagmháil lena chéile. " +
                        "Bain úsáid as seo amháin go bhfuil tú cinnte nach dteastaíonn uait faic a chloisteáil a thuilleadh ón úsáideoir seo. " +
                        "Cur cosc ar dhuine má imrítear bulaíocht ort, nó mura dteastaíonn uait tuilleadh teagmhála. " +
                        "Má athraíonn tú do mheabhair ar ball, téigh chuig na socruithe agus bainistigh cé atá curtha ar cosc.")
                .setPositiveButton("Cur cosc i bhfeidhm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        RestClient.post(activity, Endpoints.BLOCK_USER,
                                JSONUtils.getPartnerInteractionPayload(blockee, activity),
                                new BaseJsonHttpResponseHandler<JSONObject>() {
                                    @Override
                                    public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                                        callback.onCallback();
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
                })
                .setNegativeButton("Ná cur", null)
                .show();
    }

}
