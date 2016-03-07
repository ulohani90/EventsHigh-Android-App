package com.eventshigh.nearme.app.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.branch.referral.InstallListener;

/**
 * Created by umesh on 01/03/16.
 */
public class CustomInstallListener extends BroadcastReceiver{

    public CustomInstallListener(){

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        InstallListener listener = new InstallListener();
        listener.onReceive(context, intent);
    }
}
