package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.eventshigh.nearme.app.utils.Utils;

public class NetworkChangeBroadcastReceiver extends BaseWakefulBroadcastReceiver {
    public static final String PREF_LAST_UPLOAD_TIMESTAMP = "lastUploadTimestamp";

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

        // Do not upload if an upload was successful in the last 24 hrs.
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (!Utils.isDebug(context)) {
            long timeSinceLastUpload = System.currentTimeMillis()
                    - preferences.getLong(PREF_LAST_UPLOAD_TIMESTAMP, 0);
            if (!Utils.isDebug(context) && timeSinceLastUpload < DateUtils.DAY_IN_MILLIS) {
                return;
            }
        }

        // Return if another upload is already in progress (see below)
        if (setUploading()) {
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

    private static final Object lock = new Object();
    private static boolean isUploading = false;

    private static boolean setUploading() {
        synchronized (lock) {
            if (isUploading) {
                return false;
            }

            isUploading = true;
            return true;
        }
    }

    public static void setUploadFinished() {
        synchronized (lock) {
            isUploading = false;
        }
    }
}
