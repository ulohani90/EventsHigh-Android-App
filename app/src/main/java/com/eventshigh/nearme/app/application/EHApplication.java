package com.eventshigh.nearme.app.application;

import android.app.Application;

import com.crashlytics.android.Crashlytics;

import io.branch.referral.Branch;
import io.fabric.sdk.android.Fabric;

/**
 * Created by umesh on 14/03/16.
 */
public class EHApplication extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        Branch.getAutoInstance(this);
    }
}
