package com.eventshigh.nearme.app.utils;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.text.format.DateUtils;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.EventDetailActivity;
import com.eventshigh.nearme.app.broadcast.EventAlarmBroadcastReceiver;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.network.VolleyHelper;

/**
 * Helper class to create and show notifications in android notification bar.
 */
public class NotificationUtils {
    public static PendingIntent createPendingIntent(Context context, String eventId, City city) {
        if (city == null) {
            // placeholder for city.
            city = City.BANGALORE;
        }
        Intent intent = new Intent(context, EventDetailActivity.class);
        intent.setAction(BaseActivity.NOTIFICATION_ACTION);
        intent.setData(EventsHighEndpoints.getEventDetailsURI(city, eventId));
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    public static Notification createNotification(Context context, String title,
            CharSequence message, PendingIntent contentIntent) {
        return createNotificationBuilder(context, title, message, contentIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .build();
    }

    public static void showNotification(final Context context, final Event event,
                                        final Intent intent) {
        ImageRequest request = new ImageRequest(event.imgUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap, boolean b) {
                        showNotificationAndReleaseWakeLock(context, event, bitmap, intent);
                    }
                }, 0, 0, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        showNotificationAndReleaseWakeLock(context, event, null, intent);
                    }
                }
        );

        // Access the RequestQueue through your singleton class.
        VolleyHelper.addToRequestQueue(context, request);
    }

    private static void showNotificationAndReleaseWakeLock(final Context context, final Event event,
                                                           Bitmap bitmap, Intent intent) {
        PendingIntent pendingIntent = NotificationUtils.createPendingIntent(
                context, event.id, event.city);
        CharSequence relativeTime = DateUtils.getRelativeDateTimeString(
                context, event.eventTimings[0],
                DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        String message = String.format(
                context.getResources().getString(R.string.event_time_venue),
                relativeTime, event.getShortAddress());

        Notification notification = createNotificationBuilder(context, event.title,
                message, pendingIntent)
                .setStyle(
                        new NotificationCompat.BigPictureStyle()
                                .setSummaryText(message)
                                .bigPicture(bitmap)
                                .setBigContentTitle(event.title)
                )
                .build();
        showNotification(context, notification, event.hashCode());

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        EventAlarmBroadcastReceiver.completeWakefulIntent(intent);
    }

    @SuppressLint("InlinedApi")
    private static NotificationCompat.Builder createNotificationBuilder(Context context,
            String title, CharSequence message, PendingIntent contentIntent) {
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.ic_launcher);
        return new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_RECOMMENDATION)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setLargeIcon(largeIcon)
                .setContentIntent(contentIntent);
    }

    public static void showNotification(Context context, Notification notification,
                                        int notificationId) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);
    }
}
