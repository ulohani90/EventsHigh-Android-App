package com.eventshigh.nearme.app.utils;

import android.app.Activity;
import android.content.Context;

import com.amplitude.api.Amplitude;
import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class GAHelper {
    private static GAHelper instance;

    private final GoogleAnalytics googleAnalytics;
    private final Tracker tracker;

    private GAHelper(Context context) {
        googleAnalytics = GoogleAnalytics.getInstance(context);
        tracker = googleAnalytics.newTracker(R.xml.analytics);
        tracker.enableAdvertisingIdCollection(true);

        // Disable GA reporting in debug build.
        if (BuildConfig.DEBUG) {
            googleAnalytics.setAppOptOut(true);
        }

        if (!BuildConfig.DEBUG) {
            // Setup Flurry.
            FlurryAgent.setVersionName(BuildConfig.VERSION_NAME);
            FlurryAgent.init(context, "2MD4D4TP7WQZH6Q6257T");
            FlurryAgent.setLogEnabled(false);
            FlurryAgent.setReportLocation(false);

            // Setup Amplitude
            Amplitude.initialize(context, "41ed6c5c945d7f1c2f2d829b90288562");
        }
    }

    public static synchronized GAHelper getInstance(Context context) {
        if (instance == null) {
            instance = new GAHelper(context);
        }

        return instance;
    }

    public void reportActivityStart(Activity activity) {
        googleAnalytics.reportActivityStart(activity);

        // Flurry, Amplitude reporting
        if (!BuildConfig.DEBUG) {
            FlurryAgent.onStartSession(activity);
            FlurryAgent.logEvent(getClass().getSimpleName());

            Amplitude.startSession();
            Amplitude.logEvent(getClass().getSimpleName());
        }
    }

    public void reportActivityStop(Activity activity) {
        googleAnalytics.reportActivityStop(activity);

        // Flurry, Amplitude reporting
        if (!BuildConfig.DEBUG) {
            FlurryAgent.onEndSession(activity);
            Amplitude.endSession();
        }
    }

    public void setAppOptOut(boolean optOut) {
        googleAnalytics.setAppOptOut(optOut);
    }

    public boolean getAppOptOut() {
        return googleAnalytics.getAppOptOut();
    }

    public void reportActionToAnalytics(String category, String actionName, String label, long value) {
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(actionName)
                .setLabel(label)
                .setValue(value)
                .build());

        if (!BuildConfig.DEBUG) {
            FlurryAgent.logEvent(actionName);
            Amplitude.logEvent(actionName);
        }
    }
}
