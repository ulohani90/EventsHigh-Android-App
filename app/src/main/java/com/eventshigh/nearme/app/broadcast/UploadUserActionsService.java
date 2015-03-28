package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Intent;

public class UploadUserActionsService extends IntentService {
    public UploadUserActionsService() {
        super("UploadUserActionsService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        NetworkChangeBroadcastReceiver.completeWakefulIntent(intent);
    }
}
