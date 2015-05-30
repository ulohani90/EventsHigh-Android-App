package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eventshigh.nearme.app.data.UserActionDbHelper;

public class UploadUserActionsService extends IntentService {
    private static final String PREF_LAST_UPLOAD_TIMESTAMP = "lastUploadTimestamp";

    public UploadUserActionsService() {
        super("UploadUserActionsService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            long lastUploadTimestamp = preferences.getLong(PREF_LAST_UPLOAD_TIMESTAMP, 0);
            UserActionDbHelper.getInstance(this).reportActionSince(lastUploadTimestamp);
        } finally {
            NetworkChangeBroadcastReceiver.completeWakefulIntent(intent);
        }
    }
}
