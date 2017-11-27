package com.syzible.loinnir.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.network.interfaces.OnBooleanCallback;
import com.syzible.loinnir.network.interfaces.OnIntentCallback;
import com.syzible.loinnir.objects.Conversation;
import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.utils.BitmapUtils;
import com.syzible.loinnir.utils.EncodingUtils;
import com.syzible.loinnir.utils.FacebookUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 22/05/2017.
 */

public class NotificationUtils {

    private static final int VIBRATION_INTENSITY = 150;

    private static void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null)
            vibrator.vibrate(VIBRATION_INTENSITY);
    }

    private static int generateUniqueId() {
        Date now = new Date();
        return Integer.parseInt(new SimpleDateFormat("ddHHmmss", Locale.getDefault()).format(now));
    }

    public static void generatePushNotification(final Context context, String title, String content, final String url, final String notificationId) {
        final NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context)
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                        .setSmallIcon(R.drawable.logo_small)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                        .setAutoCancel(true);

        final Intent[] resultingIntent = new Intent[1];
        final OnIntentCallback onIntentCallback = intent -> generatePushNotification(intent, context, notificationBuilder);

        System.out.println(url);

        if (isFacebookLink(url)) {
            RestClient.getExternal("https://graph.facebook.com/v2.7/" + getPageName(url) + "?fields=id,name,fan_count,picture,is_verified&access_token=" + FacebookUtils.getToken(context), new BaseJsonHttpResponseHandler<JSONObject>() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, String rawJsonResponse, JSONObject response) {
                    try {
                        context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
                        resultingIntent[0] = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/" + response.getString("id")));
                        onIntentCallback.onCallback(resultingIntent[0]);
                    } catch (Exception e) {
                        resultingIntent[0] = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        onIntentCallback.onCallback(resultingIntent[0]);
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, String rawJsonData, JSONObject errorResponse) {
                    resultingIntent[0] = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    onIntentCallback.onCallback(resultingIntent[0]);
                }

                @Override
                protected JSONObject parseResponse(String rawJsonData, boolean isFailure) throws Throwable {
                    return new JSONObject(rawJsonData);
                }
            });
        } else if (url.equals("weekly_topic")) {
            resultingIntent[0] = new Intent(context, MainActivity.class);
            resultingIntent[0].putExtra("invoker", "weekly_topic");
            resultingIntent[0].putExtra("id", notificationId);
            onIntentCallback.onCallback(resultingIntent[0]);
        } else {
            resultingIntent[0] = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            onIntentCallback.onCallback(resultingIntent[0]);
        }
    }

    private static void generatePushNotification(Intent resultingIntent, Context context, NotificationCompat.Builder notificationBuilder) {
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultingIntent);

        PendingIntent resultingPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultingPendingIntent);

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null)
            manager.notify(generateUniqueId(), notificationBuilder.build());
    }

    private static String getPageName(String url) {
        return url.split("/")[3];
    }

    private static boolean isFacebookLink(String url) {
        return url.split("https://www.facebook.com/").length > 1;
    }

    public static void generateMessageNotification(final Context context, final User user,
                                                   final Message message) throws JSONException {
        if (CachingUtil.doesImageExist(context, user.getId())) {
            Bitmap avatar = CachingUtil.getCachedImage(context, user.getId());
            notifyUser(context, avatar, user, message);
        } else {
            new GetImage(new NetworkCallback<Bitmap>() {
                @Override
                public void onResponse(Bitmap response) {
                    Bitmap scaledAvatar = BitmapUtils.generateMetUserAvatar(response);
                    CachingUtil.cacheImage(context, user.getId(), scaledAvatar);
                    notifyUser(context, scaledAvatar, user, message);
                }

                @Override
                public void onFailure() {

                }
            }, user.getAvatar(), true).execute();
        }
    }

    private static void notifyUser(Context context, Bitmap avatar, User user, Message message) {
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context)
                        .setLargeIcon(avatar)
                        .setSmallIcon(R.drawable.logo_small)
                        .setContentTitle(user.getName())
                        .setContentText(EncodingUtils.decodeText(message.getText()));

        if (!MainActivity.isActivityVisible()) {
            notificationBuilder.setDefaults(Notification.DEFAULT_ALL);
            notificationBuilder.setPriority(Notification.PRIORITY_HIGH);
            vibrate(context);
        }

        // intent for opening partner conversation window
        Intent resultingIntent = new Intent(context, MainActivity.class);
        resultingIntent.putExtra("invoker", "notification");
        resultingIntent.putExtra("user", user.getId());

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultingIntent);

        PendingIntent resultingPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(resultingPendingIntent);

        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (manager != null)
            manager.notify(getNotificationId(user), notificationBuilder.build());
    }

    public static void dismissNotifications(Context context, ArrayList<Conversation> conversations) {
        for (Conversation c : conversations) {
            dismissNotification(context, (User) c.getUsers().get(0));
        }
    }

    public static void dismissNotification(Context context, int id) {
        if (context != null) {
            NotificationManager manager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null)
                manager.cancel(id);
        }
    }

    public static void dismissNotification(Context context, User user) {
        if (context != null) {
            NotificationManager manager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null)
                manager.cancel(getNotificationId(user));
        }
    }

    private static int getNotificationId(User user) {
        long originalId = Long.parseLong(user.getId());
        return (int) originalId;
    }
}
