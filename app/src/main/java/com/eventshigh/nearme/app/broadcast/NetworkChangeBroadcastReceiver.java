package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import java.util.concurrent.TimeUnit;

public class NetworkChangeBroadcastReceiver extends BaseWakefulBroadcastReceiver {
    private static final String PREF_LAST_UPLOAD_TRY_TIMESTAMP = "lastUploadTryTimestamp";
    public static final long TIME_DELAY_FOR_PUSH = TimeUnit.HOURS.toMillis(2);

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.isConnected() && shouldUpload(context)) {
            // If the device is connected and enough time from last upload, try it out.
            super.onReceive(context, intent);
        }
    }

    public boolean shouldSetResultCode() {
        return false;
    }

    @Override
    public Class<? extends IntentService> getIntentServiceClass() {
        return UploadUserActionsService.class;
    }

    private static synchronized boolean shouldUpload(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        long timeSinceLastUpload = System.currentTimeMillis()
                - preferences.getLong(PREF_LAST_UPLOAD_TRY_TIMESTAMP, 0);
        if (timeSinceLastUpload < TIME_DELAY_FOR_PUSH) {
            preferences.edit().putLong(PREF_LAST_UPLOAD_TRY_TIMESTAMP, System.currentTimeMillis()).apply();
            return true;
        }

        return false;
    }
}
