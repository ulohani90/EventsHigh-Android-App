package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.text.format.DateUtils;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.NotificationUtils;

public class EventNotificationIntentService extends IntentService {
    public static final String BUNDLE_EVENT_KEY = "event";

    public EventNotificationIntentService() {
        super("EventNotificationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            byte[] byteArrayExtra = intent.getByteArrayExtra(BUNDLE_EVENT_KEY);
            if (byteArrayExtra != null) {
                Parcel parcel = Parcel.obtain();
                parcel.unmarshall(byteArrayExtra, 0, byteArrayExtra.length);
                parcel.setDataPosition(0);
                Event event = Event.CREATOR.createFromParcel(parcel);
                showNotification(event);
                parcel.recycle();
            }
        }

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        EventAlarmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void showNotification(Event event) {
        CharSequence relativeTime = DateUtils.getRelativeDateTimeString(
                getApplicationContext(), event.eventTimings[0],
                DateUtils.DAY_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        PendingIntent pendingIntent = NotificationUtils.createPendingIntent(this, event.id,
                event.city);
        String message = String.format(
                getResources().getString(R.string.event_time_venue),
                relativeTime, event.getShortAddress());
        Notification notification = NotificationUtils.createNotification(this, event.title, message,
                pendingIntent);
        NotificationUtils.showNotification(this, notification, event.hashCode());
    }
}
