package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.EventDetailActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.GAHelper;
import com.google.android.gms.gcm.GoogleCloudMessaging;

/**
 * See https://developer.android.com/google/gcm/client.html.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        Bundle extras = intent.getExtras();
        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                // Post notification of received message.
                sendNotification(extras);
            }
        }

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    private void sendNotification(Bundle msg) {
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        String eventId = msg.getString("id");
        String title = msg.getString("t");
        String message = msg.getString("m");

        GAHelper gaHelper = GAHelper.getInstance(getApplicationContext());
        if (eventId == null || message == null || title == null) {
            // Invalid notification. Ignore.
            gaHelper.reportActionToAnalytics(
                    getClass().getSimpleName(), "invalidNotification", "", 1);
            Log.w(getClass().getSimpleName(),
                    "Invalid notification: eventId: " + eventId +
                    ", message: " + message +
                    ", title: " + title);
            return;
        }

        GcmRegistration gcmRegistration = new GcmRegistration(this);
        City city = gcmRegistration.getLastCity();
        if (city == null) {
            // placeholder for city.
            city = City.BANGALORE;
        }

        Intent intent = new Intent(this, EventDetailActivity.class);
        intent.setAction(EventDetailActivity.NOTIFICATION_ACTION);
        intent.setData(EventsHighEndpoints.getEventDetailsURI(city, eventId));
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setShowWhen(false)
                        .setCategory(Notification.CATEGORY_RECOMMENDATION)
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                        .setContentIntent(contentIntent);

        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        gaHelper.reportActionToAnalytics(
                getClass().getSimpleName(), "notificationShown", eventId, 1)    ;
    }
}
