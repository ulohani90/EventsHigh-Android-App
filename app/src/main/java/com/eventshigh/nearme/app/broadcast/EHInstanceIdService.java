package com.eventshigh.nearme.app.broadcast;

import com.google.android.gms.iid.InstanceIDListenerService;

/**
 * https://developers.google.com/instance-id/guides/android-implementation.
 */
public class EHInstanceIdService extends InstanceIDListenerService {
    public void onTokenRefresh() {
        UpdateAccountInfoService.refreshGCMToken(this);
    }
}
