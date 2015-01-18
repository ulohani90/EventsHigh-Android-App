package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.util.Log;

import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.eventshigh.nearme.app.broadcast.InstallReferrer;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Helper class to report the account state to EH site so that it is updated in db.
 */
public class AccountStateReporter {

    public static boolean reportReferrer(Context context, String referrer) {
        return sendSignedRequest(context, getBaseUri(context, "reportReferrer")
            .appendQueryParameter("referrer", referrer)
            .build());
    }

    public static boolean reportReferrerCode(Context context, String referrerCode) {
        return sendSignedRequest(context, getBaseUri(context, "reportReferrerId")
                .appendQueryParameter("referrer_id", referrerCode)
                .build());
    }

    public static boolean reportGcmRegistrationId(Context context, String gcmRegistationId) {
        return sendSignedRequest(context, getBaseUri(context, "reportGcmRegistationId")
                .appendQueryParameter("gcm_registration_id", gcmRegistationId)
                .build());
    }

    public static boolean reportLastCity(Context context, City city) {
        return sendSignedRequest(context, getBaseUri(context, "reportLastCity")
                .appendQueryParameter("last_city", city.toString())
                .build());
    }

    private static boolean sendSignedRequest(Context context, Uri uri) {
        try {
            VolleyHelper.addToRequestQueue(context,
                    new StringRequest(Method.GET, Signer.sign(uri).toString(),
                            new Listener<String>() {
                                @Override
                                public void onResponse(String s) {
                                    // do nothing.
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    // do nothing.
                                }
                            }
                    )
            );
            return true;
        } catch (IOException | GeneralSecurityException e) {
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
