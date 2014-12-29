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

    public static interface OnSuccessHandler {
        public void onSuccess();
    }

    public static class ReferrerReporter extends AsyncTask<String, Void, Void> {
        private final Context context;
        private final OnSuccessHandler onSuccessHandler;

        public ReferrerReporter(Context context, OnSuccessHandler onSuccessHandler) {
            this.context = context;
            this.onSuccessHandler = onSuccessHandler;
        }

        @Override
        protected Void doInBackground(String... params) {
            sendSignedRequest(getBaseUri(context, "reportReferrer")
                .appendQueryParameter("referrer", params[0])
                .build(), onSuccessHandler);
            return null;
        }
    }

    public static class ReferrerIdReporter extends AsyncTask<String, Void, Void> {
        private final Context context;
        private final OnSuccessHandler onSuccessHandler;

        public ReferrerIdReporter(Context context, OnSuccessHandler onSuccessHandler) {
            this.context = context;
            this.onSuccessHandler = onSuccessHandler;
        }

        @Override
        protected Void doInBackground(String... params) {
            sendSignedRequest(getBaseUri(context, "reportReferrerId")
                    .appendQueryParameter("referrer_id", params[0])
                    .build(), onSuccessHandler);
            return null;
        }
    }

    private static boolean sendSignedRequest(Uri uri, OnSuccessHandler onSuccessHandler) {
        try {
            Utils.fetchJSON(Signer.sign(uri).toString());
            onSuccessHandler.onSuccess();
            return true;
        } catch (IOException | JSONException | GeneralSecurityException e) {
            Log.w(InstallReferrer.class.getSimpleName(), "Failed to report referrerId", e);
            return false;
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
