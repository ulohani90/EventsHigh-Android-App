package com.eventshigh.nearme.app.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.user.Account;
import com.google.android.gms.analytics.CampaignTrackingReceiver;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

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
                if (BuildConfig.DEBUG) {
                    Toast.makeText(context, "Got INSTALL_REFERRER: " + referrer, Toast.LENGTH_SHORT).show();
                }
                if (referrer == null) {
                    return;
                }

                referrer = URLDecoder.decode(referrer, "UTF-8");
                new Account(context).recordReferrer(referrer);
            } catch (UnsupportedEncodingException e) {
                // Ignore.
            }
        }

        // Report the install referrer intent to Google Analytics.
        new CampaignTrackingReceiver().onReceive(context, intent);
    }
}
