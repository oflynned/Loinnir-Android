package com.syzible.loinnir.utils;

import android.content.Context;

import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.objects.User;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Created by ed on 11/06/2017.
 */

public class EncodingUtils {

    public static void copyText(Context context, Message message) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text",
                EncodingUtils.decodeText(message.getText()));
        clipboard.setPrimaryClip(clip);

        DisplayUtils.generateToast(context, "Rinneadh cóipeáil den téacs " +
                LanguageUtils.getPrepositionalForm("ó", ((User) message.getUser()).getForename()));
    }

    public static String encodeText(String text) {
        try {
            return URLEncoder.encode(text, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return text;
    }

    public static String decodeText(String encodedText) {
        try {
            return URLDecoder.decode(encodedText, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return encodedText;
    }
}
