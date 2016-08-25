package com.eventshigh.nearme.app.application;

import android.app.Application;
import android.os.AsyncTask;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;
import com.eventshigh.nearme.app.BuildConfig;

import io.branch.referral.Branch;
import io.fabric.sdk.android.Fabric;

/**
 * Created by umesh on 14/03/16.
 */
public class EHApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Branch.getAutoInstance(EHApplication.this);
        // Twitter CrashAnalytics
        new InitailizeSdks().execute();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Runtime.getRuntime().gc();
    }

    public class InitailizeSdks extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
            Fabric.with(EHApplication.this, new Crashlytics.Builder().core(core).build());
            return null;
        }
    }
}
