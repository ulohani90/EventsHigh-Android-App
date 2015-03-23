package com.eventshigh.nearme.app.utils;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
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
    public static final int WEEKEND_EVENTS_NOTIFICATION_ID = 3;

    private static final int MAX_EVENTS_TO_SHOW_IN_NOTIFICATION = 3;

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

    private static Notification createNotification(Context context, String title,
            CharSequence message, PendingIntent contentIntent) {
        return createNotificationBuilder(context, title, message, contentIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .build();
    }

    public static void showNotificationAndReleaseWakeLock(Context context, Intent alarmIntent,
                                                          Event event, int notificationId) {
        NotificationData notificationData = new NotificationData(context, alarmIntent, event,
                notificationId);
        showNotificationAndReleaseWakeLock(context, notificationData);
    }

    public static void showNotificationAndReleaseWakeLock(Context context,
            final NotificationData notificationData) {
        if (notificationData.imageUrl == null) {
            showNotificationAndReleaseWakeLock(notificationData);
            return;
        }
        ImageRequest request = new ImageRequest(notificationData.imageUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap, boolean b) {
                        showNotificationAndReleaseWakeLock(notificationData, bitmap);
                    }
                }, 0, 0, null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        showNotificationAndReleaseWakeLock(notificationData);
                    }
                }
        );

        // Access the RequestQueue through your singleton class.
        VolleyHelper.addToRequestQueue(context, request);
    }

    private static void showNotificationAndReleaseWakeLock(
            NotificationData notificationData) {
        Notification notification = createNotificationBuilder(notificationData.context,
                notificationData.title, notificationData.message, notificationData.pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationData.message))
                .build();

        showNotification(notificationData.context, notification, notificationData.notificationId);

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        WakefulBroadcastReceiver.completeWakefulIntent(notificationData.alarmIntent);
    }

    private static void showNotificationAndReleaseWakeLock(NotificationData notificationData,
                                                           Bitmap bitmap) {
        NotificationCompat.Builder notificationBuilder = createNotificationBuilder(
                notificationData.context, notificationData.title, notificationData.message,
                notificationData.pendingIntent)
                .setStyle(new NotificationCompat.BigPictureStyle()
                                .setSummaryText(notificationData.message)
                                .bigPicture(bitmap)
                                .setBigContentTitle(notificationData.title)
                );

        if (notificationData.showOnMapIntent != null) {
            PendingIntent showOnMapPendingIntent = PendingIntent.getActivity(
                    notificationData.context, 0, notificationData.showOnMapIntent, 0);
            NotificationCompat.Action showOnMapAction = new NotificationCompat.Action(
                    R.drawable.ic_location_on_grey600_24dp,
                    notificationData.context.getString(R.string.ui_view_location_on_map),
                    showOnMapPendingIntent);
            notificationBuilder.addAction(showOnMapAction);
        }

        showNotification(notificationData.context, notificationBuilder.build(),
                notificationData.notificationId);

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        WakefulBroadcastReceiver.completeWakefulIntent(notificationData.alarmIntent);
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

    public synchronized static void showMyEventsNotificationAndReleaseWakeLock(
            Context context, List<Event> events, Intent alarmIntent) {
        showNotificationAndReleaseWakeLock(context, events, R.string.ui_upcoming_events,
                R.string.ui_upcoming_events_msg, EventsHighEndpoints.QUERY_MY_EVENT,
                MY_EVENTS_NOTIFICATION_ID, alarmIntent);
    }

    public synchronized static void showFeaturedEventsNotificationAndReleaseWakeLock(
            Context context, List<Event> events, Intent alarmIntent) {
        showNotificationAndReleaseWakeLock(context, events, R.string.ui_weekend_events,
                R.string.ui_weekend_events_msg, EventsHighEndpoints.QUERY_WEEKEND,
                WEEKEND_EVENTS_NOTIFICATION_ID, alarmIntent);
    }

    private synchronized static void showNotificationAndReleaseWakeLock(
            Context context, List<Event> events, int titleResourceId, int messageResourceId,
            String query, int notificationId, Intent alarmIntent) {
        StringBuilder message = new StringBuilder(
                context.getString(messageResourceId)).append("\n");
        for (int i = 0; i < events.size() && i < MAX_EVENTS_TO_SHOW_IN_NOTIFICATION; i++) {
            message.append(events.get(i).title);
            if (events.get(i).venue != null) {
                message.append(" @ ").append(events.get(i).venue);
            }
            message.append("\n");
        }

        Intent launchIntent = new Intent(context, LaunchActivity.class);
        launchIntent.setAction(Intent.ACTION_SEARCH);
        launchIntent.putExtra(SearchManager.QUERY, query);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, launchIntent, 0);
        Notification notification = createNotification(context,
                context.getString(titleResourceId), message, pendingIntent);
        showNotification(context, notification, notificationId);

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        WakefulBroadcastReceiver.completeWakefulIntent(alarmIntent);
    }

    public static class NotificationData {
        public final Context context;
        public final Intent alarmIntent;

        public final String title;
        public final String message;
        public final String imageUrl;

        public final PendingIntent pendingIntent;
        public final Intent showOnMapIntent;
        public final int notificationId;

        public NotificationData(Context context, Intent alarmIntent, Event event,
                                int notificationId) {
            this.context = context;
            this.alarmIntent = alarmIntent;

            title = event.title;
            CharSequence relativeTime = DateUtils.getRelativeDateTimeString(
                    context, event.eventTimings[0],
                    DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
            message = String.format(
                    context.getResources().getString(R.string.event_time_venue),
                    relativeTime, event.getShortAddress());
            imageUrl = event.imgUrl;

            pendingIntent = createPendingIntent(context, event.id, event.city);
            showOnMapIntent = event.getShowOnMapIntent();
            this.notificationId = notificationId;
        }

        public NotificationData(Context context, Intent alarmIntent, String title, String message,
                                String imageUrl, PendingIntent pendingIntent) {
            this.context = context;
            this.alarmIntent = alarmIntent;

            this.title = title;
            this.message = message;
            this.imageUrl = imageUrl;

            this.pendingIntent = pendingIntent;
            showOnMapIntent = null;
            notificationId = NotificationUtils.GCM_NOTIFICATION_ID;
        }
    }
}
