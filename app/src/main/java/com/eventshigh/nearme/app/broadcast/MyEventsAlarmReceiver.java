package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;

public class MyEventsAlarmReceiver extends BaseWakefulBroadcastReceiver {
    @Override
    public Class<? extends IntentService> getIntentServiceClass() {
        return MyEventsNotificationService.class;
    }
}
