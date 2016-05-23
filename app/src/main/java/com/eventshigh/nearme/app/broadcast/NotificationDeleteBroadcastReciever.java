package com.eventshigh.nearme.app.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.eventshigh.nearme.app.user.Preferences;

/**
 * Created by umesh on 18/05/16.
 */
public class NotificationDeleteBroadcastReciever extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Preferences.getInstance(context).setIsNotificationActive(false);
    }
}
