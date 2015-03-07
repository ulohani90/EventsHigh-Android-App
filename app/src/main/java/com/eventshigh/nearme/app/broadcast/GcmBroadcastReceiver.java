package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;

/**
 * An Google Cloud messaging client which receives the messages through broadcast. These
 * messages are used for notification which are server controlled.
 *
 * See See https://developer.android.com/google/gcm/client.html.
 */
public class GcmBroadcastReceiver extends BaseWakefulBroadcastReceiver {
    @Override
    public Class<? extends IntentService> getIntentServiceClass() {
        return GcmIntentService.class;
    }
}
