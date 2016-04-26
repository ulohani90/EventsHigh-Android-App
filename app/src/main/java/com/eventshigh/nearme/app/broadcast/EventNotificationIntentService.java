package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Intent;


import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.notification.EHNotification;

/**
 * Created by umesh on 26/04/16.
 */
public class EventNotificationIntentService extends IntentService {
    public static final String BUNDLE_EVENT_KEY = "event";

    public EventNotificationIntentService() {
        super("EventNotificationIntentService");
    }

    @Override
    protected void onHandleIntent(Intent wakefulIntent) {
        Event event = wakefulIntent.getParcelableExtra(BUNDLE_EVENT_KEY);
        if (event == null) {
            // Release the wake lock provided by the WakefulBroadcastReceiver.
            EventAlarmBroadcastReceiver.completeWakefulIntent(wakefulIntent);
            return;
        }

        // Notify user about the Event.
        EHNotification EHNotification = new EHNotification(this, wakefulIntent, event,
                event.hashCode());
        EHNotification.showNotification();
    }
}