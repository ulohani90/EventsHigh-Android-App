package com.eventshigh.nearme.app.application;

import android.app.Application;
import android.os.AsyncTask;

import io.branch.referral.Branch;

public class EHApplication extends Application{

    @Override
    public void onCreate() {
        super.onCreate();


        Branch.getAutoInstance(this);

        Branch.getAutoInstance(EHApplication.this);
        // Twitter CrashAnalytics

    }

}
