package com.eventshigh.nearme.app.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.eventshigh.nearme.app.user.Account;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import io.branch.referral.InstallListener;

/**
 * Received the Broadcast intent after app is installed. The intent contains the
 * referrer information.
 */
public class InstallReferrer extends BroadcastReceiver {
    public InstallReferrer() {}

    @Override
    public void onReceive(Context context, Intent intent) {



        if (intent.getAction().equals("com.android.vending.INSTALL_REFERRER")) {
            try {
                String referrer = intent.getStringExtra("referrer");
                if (referrer == null) {
                    return;
                }
                /*String referrer2 = intent.getStringExtra("referrer2");
                if(referrer2!=null){
                    Log.i("EHInstall_Referrer2", referrer2);
                }*/
                referrer = URLDecoder.decode(referrer, "UTF-8");
                new Account(context).recordReferrer(referrer,false);
            } catch (UnsupportedEncodingException e) {
                // Ignore.
            }
        }

        // Branch.
        InstallListener listener = new InstallListener();
        listener.onReceive(context, intent);
    }
}
