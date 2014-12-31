package com.eventshigh.nearme.app.utils;

import android.content.Context;
import android.util.Pair;

import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class GAHelper {

    public static Pair<GoogleAnalytics, Tracker> getTracker(Context context) {
        GoogleAnalytics googleAnalytics = GoogleAnalytics.getInstance(context);
        Tracker tracker = googleAnalytics.newTracker(R.xml.analytics);
        tracker.enableAdvertisingIdCollection(true);

        // Disable GA reporting in debug build.
        if (BuildConfig.DEBUG) {
            googleAnalytics.setAppOptOut(true);
        }

        return Pair.create(googleAnalytics, tracker);
    }
}
