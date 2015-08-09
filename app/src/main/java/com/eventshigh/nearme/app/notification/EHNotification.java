package com.eventshigh.nearme.app.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.text.format.DateUtils;
import android.widget.ImageView.ScaleType;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.EventDetailActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.data.stream.EventNotificationStreamItem;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;

/**
 * Represents the notification shown in EH.
 */
public class EHNotification {
    // See https://code.google.com/p/android/issues/detail?id=36744.
    public static final int NOTIFICATION_IMAGE_WIDTH_DP = 450;
    public static final int NOTIFICATION_IMAGE_HEIGHT_DP = 225;

    public final Context context;
    public final Intent wakefulIntent;

    public final String title;
    public final String message;
    public final String imageUrl;

    public final PendingIntent launchIntent;
    public final int notificationId;
    public final int priority;

    @Nullable UserContact contact;

    public EHNotification(Context context, Intent wakefulIntent, Event event, int notificationId) {
        this.context = context;
        this.wakefulIntent = wakefulIntent;
        this.notificationId = notificationId;
        this.priority = Notification.PRIORITY_HIGH;

        title = event.title;
        CharSequence relativeTime = DateUtils.getRelativeDateTimeString(
                context, event.eventTimings[0],
                DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        message = String.format(
                context.getResources().getString(R.string.event_time_venue),
                relativeTime, event.getShortAddress());
        imageUrl = event.imgUrl;

        launchIntent = createPendingIntent(context, event.id, event.city);
        contact = null;

        // Record notification in stream.
        EventNotificationStreamItem.record(context, title, message, imageUrl, null, event.id,
                event.city);
    }

    public EHNotification(Context context, Intent wakefulIntent, String title, String message,
            String imageUrl, PendingIntent launchIntent, int priority, @Nullable UserContact contact) {
        this.context = context;
        this.wakefulIntent = wakefulIntent;
        this.notificationId = contact == null ?
                NotificationUtils.GCM_NOTIFICATION_ID : contact.mobileNo.hashCode();
        this.priority = priority;

        this.title = title;
        this.message = message;
        this.imageUrl = imageUrl;
        this.launchIntent = launchIntent;
        this.contact = contact;
    }

    public void showNotificationAndReleaseWakeLock() {
        if (imageUrl == null) {
            showNotificationNoImage();
            return;
        }

        // Submit the image request to Volley.
        ImageRequest request = new ImageRequest(imageUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap, boolean isIntermediate) {
                        if (!isIntermediate) {
                            showNotificationWithImage(bitmap);
                        }
                    }
                },
                Utils.dpToPx(context, NOTIFICATION_IMAGE_WIDTH_DP),
                Utils.dpToPx(context, NOTIFICATION_IMAGE_HEIGHT_DP),
                ScaleType.CENTER_CROP,
                null,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        showNotificationNoImage();
                    }
                }
        );
        VolleyHelper.addToRequestQueue(context, request);
    }

    private void showNotificationNoImage() {
        Notification notification = NotificationUtils.createNotificationBuilder(context, title,
                message, contact, launchIntent, priority)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .build();

        NotificationUtils.showNotification(context, notification, notificationId, title);
        WakefulBroadcastReceiver.completeWakefulIntent(wakefulIntent);
    }

    private void showNotificationWithImage(Bitmap bitmap) {
        Notification notification = NotificationUtils.createNotificationBuilder(
                context, title, message, contact, launchIntent, priority)
                .setStyle(new NotificationCompat.BigPictureStyle()
                                .setSummaryText(message)
                                .bigPicture(bitmap)
                                .setBigContentTitle(title)
                )
                .build();


        NotificationUtils.showNotification(context, notification, notificationId, title);
        WakefulBroadcastReceiver.completeWakefulIntent(wakefulIntent);
    }

    public static PendingIntent createPendingIntent(Context context, String eventId,
            @Nullable City city) {
        if (city == null) {
            // placeholder for city.
            city = City.BANGALORE;
        }

        Intent intent = new Intent(context, EventDetailActivity.class);
        intent.setAction(BaseActivity.NOTIFICATION_ACTION);
        intent.setData(EventsHighEndpoints.getEventDetailsURI(city, eventId));
        return PendingIntent.getActivity(context, 0, intent, 0);
    }
}
