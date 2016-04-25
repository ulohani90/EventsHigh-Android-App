package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GcmListenerService;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * This class stores the GCM registration data locally within app.
 */
public class GcmRegistration {
    public static final String SENDER_ID = "708156551009";

    public static void sendUpstream(Context context, Bundle data) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        if (apiAvailability.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
            GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);

            data.putLong("time_to_live", TimeUnit.DAYS.toSeconds(1));
            try {
                String messageId = UUID.randomUUID().toString();
                Log.i("Sending message id",messageId);
                gcm.send(SENDER_ID + "@gcm.googleapis.com", messageId, data);
            } catch (Exception e) {
                Crashlytics.getInstance().core.logException(e);
            }
        }
    }



}
