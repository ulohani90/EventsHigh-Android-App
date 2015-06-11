package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request.Method;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Helper class to report the account state to EH site so that it is updated in db.
 */
public class AccountStateReporter {

    public static void reportReferrer(Context context, String referrer, Runnable onSuccess) {
        sendSignedRequest(context, getBaseUri(context, "reportReferrer")
            .appendQueryParameter("referrer", referrer)
            .build(), onSuccess);
    }

    public static void reportGcmRegistrationId(Context context, String gcmRegistationId, Runnable onSuccess) {
        sendSignedRequest(context, getBaseUri(context, "reportGcmRegistationId")
                .appendQueryParameter("gcm_registration_id", gcmRegistationId)
                .build(), onSuccess);
    }

    public static void reportInstanceId(Context context, String iid, Runnable onSuccess) {
        sendSignedRequest(context, getBaseUri(context, "reportInstanceId")
                .appendQueryParameter("instance_id", iid)
                .build(), onSuccess);
    }

    public static void reportLastCity(Context context, City city, @Nullable LatLng location, Runnable onSuccess) {
        if (location == null) {
            location = new LatLng(0, 0);
        }

        sendSignedRequest(context, getBaseUri(context, "reportLastCity")
                .appendQueryParameter("last_city", city.toString())
                .appendQueryParameter("lat", Double.toString(location.latitude))
                .appendQueryParameter("lon", Double.toString(location.longitude))
                .build(), onSuccess);
    }

    private static void sendSignedRequest(Context context, Uri uri, final Runnable onSuccess) {
        try {
            VolleyHelper.addToRequestQueue(context,
                    new StringRequest(Method.GET, Signer.sign(uri).toString(),
                            new Listener<String>() {
                                @Override
                                public void onResponse(String s, boolean isIntermediate) {
                                    onSuccess.run();
                                }
                            },
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    // do nothing.
                                    Crashlytics.getInstance().core.logException(volleyError.getCause());
                                }
                            }
                    )
            );
        } catch (IOException | GeneralSecurityException e) {
            Crashlytics.getInstance().core.logException(e);
            Log.w(AccountStateReporter.class.getSimpleName(), "Failed to sendSignedRequest: " + uri, e);
        }
    }

    public static Builder getBaseUri(Context context, String path) {
        return getBaseUriWithoutAndroidId(path)
            .appendQueryParameter("android_id", Utils.getAndroidId(context));
    }

  public static Builder getBaseUriWithoutAndroidId(String path) {
      return Uri.parse(EventsHighEndpoints.API_URI_BASE)
          .buildUpon()
          .appendPath("mobileapp")
          .appendPath(path);
  }
}
