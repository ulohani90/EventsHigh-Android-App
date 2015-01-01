package com.eventshigh.nearme.app.utils;

import android.app.Activity;
import android.content.Context;

import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class GAHelper {
    private final GoogleAnalytics googleAnalytics;
    private final Tracker tracker;

    public GAHelper(Context context) {
        googleAnalytics = GoogleAnalytics.getInstance(context);
        tracker = googleAnalytics.newTracker(R.xml.analytics);
        tracker.enableAdvertisingIdCollection(true);

        // Disable GA reporting in debug build.
        if (BuildConfig.DEBUG) {
            googleAnalytics.setAppOptOut(true);
        }
    }

    public void reportActivityStart(Activity activity) {
        googleAnalytics.reportActivityStart(activity);
    }

    public void reportActivityStop(Activity activity) {
        googleAnalytics.reportActivityStop(activity);
    }

    public void setAppOptOut(boolean optOut) {
        googleAnalytics.setAppOptOut(optOut);
    }

    public void reportActionToAnalytics(String category, String actionName, String label, long value) {
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(actionName)
                .setLabel(label)
                .setValue(value)
                .build());
    }
}
