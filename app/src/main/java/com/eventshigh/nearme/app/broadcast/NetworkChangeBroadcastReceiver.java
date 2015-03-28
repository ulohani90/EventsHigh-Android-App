package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkChangeBroadcastReceiver extends BaseWakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if the device is connected to wifi
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()
                || activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) {
            // If the device is not connected to wifi, then there is nothing to do
            return;
        }

        // TODO: do not upload if an upload was successful in the last 24 hrs.

        // TODO: call super only if another upload is not already in progress (see TODO below)
        super.onReceive(context, intent);

        // TODO: mark that upload has started, because we can receive multiple intents while we are processing one upload
    }

    @Override
    public Class<? extends IntentService> getIntentServiceClass() {
        return UploadUserActionsService.class;
    }
}
