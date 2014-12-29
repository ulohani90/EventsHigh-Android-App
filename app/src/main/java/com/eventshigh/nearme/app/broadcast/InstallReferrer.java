package com.eventshigh.nearme.app.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.Settings.Secure;
import android.util.Log;
import android.widget.Toast;

import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;

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
                Toast.makeText(context, "Found referrer: " + referrer, Toast.LENGTH_LONG).show();
                if (referrer == null) {
                    return;
                }

                referrer = URLDecoder.decode(referrer, "UTF-8");
                if (new Account(context).recordReferrer(referrer) || BuildConfig.DEBUG) {
                    new ReportReferrer().execute(referrer);
                }
            } catch (UnsupportedEncodingException e) {
                // Ignore.
            }
        }
    }

    public static class ReportReferrer extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String referrer = params[0];
            try {
                Uri uri = Uri.parse("http://apiserver.eventshigh.com:8888/mobileapp/reportReferrer")
                    .buildUpon()
                    .appendQueryParameter("referrer", referrer)
                    .appendQueryParameter("android_id", Secure.ANDROID_ID)
                    .build();

                Utils.fetchJSON(Signer.sign(uri).toString());
            } catch (IOException | JSONException | GeneralSecurityException  e) {
                Log.w(InstallReferrer.class.getSimpleName(), "Failed to report referrer", e);
            }

            return null;
        }
    }
}
