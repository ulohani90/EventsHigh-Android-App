package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;

public class DownloadEventsBroadcastReceiver extends BaseWakefulBroadcastReceiver {
    @Override
    public Class<? extends IntentService> getIntentServiceClass() {
        return DownloadEventsIntentService.class;
    }
}
