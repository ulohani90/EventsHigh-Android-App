package com.eventshigh.nearme.app.broadcast;

import com.eventshigh.nearme.app.user.GcmRegistration;
import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * https://developers.google.com/instance-id/guides/android-implementation.
 */
public class EHInstanceIdService extends InstanceIDListenerService {
    public void onTokenRefresh() {
        GcmRegistration.getInstance(this).resetGcmRegistrationId();
    }
}
