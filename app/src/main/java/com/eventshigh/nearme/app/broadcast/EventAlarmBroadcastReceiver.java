package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;

/**
 * Receives the event alarm broadcasts. The message is used to show a notification for the upcoming
 * event.
 */
public class EventAlarmBroadcastReceiver extends BaseWakefulBroadcastReceiver {
    @Override
    public Class<? extends IntentService> getIntentServiceClass() {
        return EventNotificationIntentService.class;
    }
}
