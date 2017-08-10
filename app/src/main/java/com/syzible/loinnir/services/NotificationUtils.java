package com.syzible.loinnir.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;

import com.google.firebase.messaging.RemoteMessage;
import com.loopj.android.http.BaseJsonHttpResponseHandler;
import com.syzible.loinnir.R;
import com.syzible.loinnir.activities.MainActivity;
import com.syzible.loinnir.network.Endpoints;
import com.syzible.loinnir.network.GetImage;
import com.syzible.loinnir.network.NetworkCallback;
import com.syzible.loinnir.network.RestClient;
import com.syzible.loinnir.objects.Conversation;
import com.syzible.loinnir.objects.Message;
import com.syzible.loinnir.objects.User;
import com.syzible.loinnir.utils.BitmapUtils;
import com.syzible.loinnir.utils.EncodingUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

/**
 * Created by ed on 22/05/2017.
 */

public class NotificationUtils {

    private static final int VIBRATION_INTENSITY = 150;

    private static void vibrate(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VIBRATION_INTENSITY);
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

        manager.notify(getNotificationId(user), notificationBuilder.build());
    }

    public static void dismissNotifications(Context context, ArrayList<Conversation> conversations) {
        for (Conversation c : conversations) {
            dismissNotification(context, (User) c.getUsers().get(0));
        }
    }

    public static void dismissNotification(Context context, User user) {
        NotificationManager manager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(getNotificationId(user));
    }

    private static int getNotificationId(User user) {
        long originalId = Long.parseLong(user.getId());
        return (int) originalId;
    }
}
