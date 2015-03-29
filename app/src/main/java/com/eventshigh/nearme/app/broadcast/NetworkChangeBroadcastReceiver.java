package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

public class NetworkChangeBroadcastReceiver extends BaseWakefulBroadcastReceiver {
    public static final String PREF_IS_UPLOADING = "isUploading";

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

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        // TODO: do not upload if an upload was successful in the last 24 hrs.

        // Return if another upload is already in progress (see below)
        if (preferences.getBoolean(PREF_IS_UPLOADING, false)) {
            return;
        }

        // Mark that upload has started, because we can receive multiple intents while we are
        // processing one upload
        preferences.edit().putBoolean(PREF_IS_UPLOADING, true).apply();
        super.onReceive(context, intent);
    }

    public boolean shouldSetResultCode() {
        return false;
    }

    @Override
    public Class<? extends IntentService> getIntentServiceClass() {
        return UploadUserActionsService.class;
    }
}
