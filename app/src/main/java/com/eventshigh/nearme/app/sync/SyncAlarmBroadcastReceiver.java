package com.eventshigh.nearme.app.sync;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.LaunchActivity;

public class SyncAlarmBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = new Bundle();
        // If the sync needs to happen immediately, uncomment this
        //bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        //bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        String provider = context.getResources().getString(R.string.sync_adapter_provider);
        ContentResolver.requestSync(LaunchActivity.createSyncAccount(context), provider, bundle);
    }
}
