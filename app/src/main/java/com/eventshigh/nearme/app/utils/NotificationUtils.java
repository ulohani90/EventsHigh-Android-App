package com.eventshigh.nearme.app.utils;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.format.DateUtils;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.EventDetailActivity;
import com.eventshigh.nearme.app.activity.LaunchActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.network.VolleyHelper;

import java.util.List;

/**
 * Helper class to create and show notifications in android notification bar.
 */
public class NotificationUtils {
    public static final int GCM_NOTIFICATION_ID = 1;
    public static final int MY_EVENTS_NOTIFICATION_ID = 2;
    private static final int MAX_MY_EVENTS_TO_SHOW_IN_NOTIFICATION = 3;

    private static final String SHARED_PREFS_FOR_MY_EVENTS_NOTIFICATIONS = "MyEventsNotifications";

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

    public static void showNotificationAndReleaseWakeLock(
            final Context context, final Event event, final Intent intent) {
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

    private static void showNotificationAndReleaseWakeLock(
            Context context, Event event, Bitmap bitmap, Intent intent) {
        PendingIntent pendingIntent = createPendingIntent(context, event.id, event.city);
        CharSequence relativeTime = DateUtils.getRelativeDateTimeString(
                context, event.eventTimings[0],
                DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        String message = String.format(
                context.getResources().getString(R.string.event_time_venue),
                relativeTime, event.getShortAddress());
        NotificationCompat.Builder notificationBuilder = createNotificationBuilder(
                context, event.title, message, pendingIntent)
                .setStyle(new NotificationCompat.BigPictureStyle()
                    .setSummaryText(message)
                    .bigPicture(bitmap)
                    .setBigContentTitle(event.title)
                );

        Intent showOnMapIntent = event.getShowOnMapIntent();
        if (showOnMapIntent != null) {
            PendingIntent showOnMapPendingIntent = PendingIntent.getActivity(
                    context, 0, showOnMapIntent, 0);
            NotificationCompat.Action showOnMapAction = new NotificationCompat.Action(
                    R.drawable.ic_location_on_grey600_24dp,
                    context.getString(R.string.ui_view_location_on_map),
                    showOnMapPendingIntent);
            notificationBuilder.addAction(showOnMapAction);
        }

        showNotification(context, notificationBuilder.build(), event.hashCode());

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }

    @SuppressLint("InlinedApi")
    private static NotificationCompat.Builder createNotificationBuilder(Context context,
            String title, CharSequence message, PendingIntent contentIntent) {
        Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher);
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

    public static void showNotification(Context context, Notification notification, int notificationId) {
        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notification);
    }

    public synchronized static void showNotificationAndReleaseWakeLock(
            Context context, List<Event> events, Intent alarmIntent) {
        SharedPreferences myEventsNotificationPreferences = context.getSharedPreferences(
                SHARED_PREFS_FOR_MY_EVENTS_NOTIFICATIONS, Context.MODE_PRIVATE);
        int count = 0;
        StringBuilder message = new StringBuilder(
                context.getString(R.string.ui_upcoming_events_msg)).append("\n");
        for (int i = 0; i < events.size(); i++) {
            if (count >= MAX_MY_EVENTS_TO_SHOW_IN_NOTIFICATION) {
                break;
            }
            if (myEventsNotificationPreferences.contains(events.get(i).id)) {
                continue;
            }
            count++;
            message.append(events.get(i).title);
            message.append("\n");
            myEventsNotificationPreferences.edit().putBoolean(events.get(i).id, true).apply();
        }

        Intent myEventsIntent = new Intent(context, LaunchActivity.class);
        myEventsIntent.putExtra(SearchManager.QUERY, EventsHighEndpoints.QUERY_MY_EVENT);
        myEventsIntent.setAction(Intent.ACTION_SEARCH);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, myEventsIntent, 0);
        Notification notification = createNotification(context,
                context.getString(R.string.ui_upcoming_events), message, pendingIntent);
        showNotification(context, notification, MY_EVENTS_NOTIFICATION_ID);

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        WakefulBroadcastReceiver.completeWakefulIntent(alarmIntent);
    }
}
