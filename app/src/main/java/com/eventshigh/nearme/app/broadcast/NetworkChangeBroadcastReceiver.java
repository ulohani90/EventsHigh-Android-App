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

        super.onReceive(context, intent);
    }

    @Override
    public Class<? extends IntentService> getIntentServiceClass() {
        return UploadUserActionsService.class;
    }
}
