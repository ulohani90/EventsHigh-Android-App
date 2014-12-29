package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.AsyncTask;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;

import com.eventshigh.nearme.app.broadcast.InstallReferrer;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class AccountStateReporter {

    public static class ReferrerReporter extends AsyncTask<String, Void, Void> {
        private final Context context;

        public ReferrerReporter(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(String... params) {
            sendSignedRequest(getBaseUri(context, "reportReferrer")
                .appendQueryParameter("referrer", params[0])
                .build());
            return null;
        }
    }

    public static class ReferrerIdReporter extends AsyncTask<String, Void, Void> {
        private final Context context;

        public ReferrerIdReporter(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(String... params) {
            sendSignedRequest(getBaseUri(context, "reportReferrerId")
                    .appendQueryParameter("referrerId", params[0])
                    .build());
            return null;
        }
    }

    private static void sendSignedRequest(Uri uri) {
        try {
            Utils.fetchJSON(Signer.sign(uri).toString());
        } catch (IOException | JSONException | GeneralSecurityException e) {
            Log.w(InstallReferrer.class.getSimpleName(), "Failed to report referrerId", e);
        }
    }

    private static Builder getBaseUri(Context context, String path) {
        return Uri.parse("http://apiserver.eventshigh.com:8888/mobileapp")
            .buildUpon()
            .appendPath(path)
            .appendQueryParameter("android_id",
                Settings.Secure.getString(context.getContentResolver(), Secure.ANDROID_ID));
    }

}
