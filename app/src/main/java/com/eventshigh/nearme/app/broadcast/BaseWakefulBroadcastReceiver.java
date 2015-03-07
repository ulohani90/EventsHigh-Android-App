package com.eventshigh.nearme.app.broadcast;

import android.app.Activity;
import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

/**
 * Base class for all broadcast receivers in this app. Launches the specific service when a
 * broadcast event is received.
 */
public abstract class BaseWakefulBroadcastReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Explicitly specify that GcmIntentService will handle the intent.
        ComponentName comp = new ComponentName(context.getPackageName(),
                getIntentServiceClass().getName());
        // Start the service, keeping the device awake while it is launching.
        startWakefulService(context, (intent.setComponent(comp)));
        setResultCode(Activity.RESULT_OK);
    }

    /**
     * @return the intent service class to launch
     */
    public abstract Class<? extends IntentService> getIntentServiceClass();
}
