package com.eventshigh.nearme.app.broadcast;


import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * https://developers.google.com/instance-id/guides/android-implementation.
 */
public class EHInstanceIdService extends FirebaseInstanceIdService {
    public void onTokenRefresh() {
        UpdateAccountInfoService.refreshGCMToken(this);
    }
}
