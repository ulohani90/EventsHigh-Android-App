package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;

/**
 * Created by umesh on 26/04/16.
 */
public class EventAlarmBroadcastReceiver  extends BaseWakefulBroadcastReceiver {
    @Override
    public Class<? extends IntentService> getIntentServiceClass() {
        return EventNotificationIntentService.class;
    }
}
