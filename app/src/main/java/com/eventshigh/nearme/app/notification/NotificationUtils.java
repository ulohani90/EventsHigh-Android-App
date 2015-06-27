package com.eventshigh.nearme.app.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.GAHelper;

/**
 * Helper class to create and show notifications in android notification bar.
 */
public class NotificationUtils {
    public static final int GCM_NOTIFICATION_ID = 1;
    public static final int MY_EVENTS_NOTIFICATION_ID = 2;

    @SuppressLint("InlinedApi")
    public static NotificationCompat.Builder createNotificationBuilder(Context context, String title,
            CharSequence message, Bitmap largeIcon, PendingIntent contentIntent, int priority) {
        if (largeIcon == null) {
            largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_EVENT)
                .setPriority(priority)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setLargeIcon(largeIcon)
                .setContentIntent(contentIntent);

        if (priority >= Notification.PRIORITY_DEFAULT) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        }

        return builder;
    }

    public static void showNotification(Context context, Notification notification, int notificationId) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        GAHelper.getInstance(context).reportActionToAnalytics("background", "notificationShown",
                Integer.toString(notificationId));
        notificationManager.notify(notificationId, notification);
    }
}
