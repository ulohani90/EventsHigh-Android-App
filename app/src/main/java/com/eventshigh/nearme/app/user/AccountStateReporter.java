package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;

import com.eventshigh.nearme.app.broadcast.InstallReferrer;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.utils.StreamUtils;

import org.json.JSONException;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class AccountStateReporter {

    public static boolean reportReferrer(Context context, String referrer) {
        return sendSignedRequest(getBaseUri(context, "reportReferrer")
            .appendQueryParameter("referrer", referrer)
            .build());
    }

    public static boolean reportReferrerCode(Context context, String referrerCode) {
        return sendSignedRequest(getBaseUri(context, "reportReferrerId")
                .appendQueryParameter("referrer_id", referrerCode)
                .build());
    }

    public static boolean reportGcmRegistrationId(Context context, String gcmRegistationId) {
        return sendSignedRequest(getBaseUri(context, "reportGcmRegistationId")
                .appendQueryParameter("gcm_registration_id", gcmRegistationId)
                .build());
    }

    public static boolean reportLastCity(Context context, City city) {
        return sendSignedRequest(getBaseUri(context, "reportLastCity")
                .appendQueryParameter("last_city", city.toString())
                .build());
    }

    private static boolean sendSignedRequest(Uri uri) {
        try {
            StreamUtils.fetchJSON(Signer.sign(uri).toString());
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
