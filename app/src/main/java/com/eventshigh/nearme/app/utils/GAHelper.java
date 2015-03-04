package com.eventshigh.nearme.app.utils;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.HitBuilders.EventBuilder;
import com.google.android.gms.analytics.Tracker;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Helper around analytics which maintains the singleton instance and reports the
 * events to both Google Analytics and Amplitude.
 */
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
    }

    public static synchronized GAHelper getInstance(Context context) {
        if (instance == null) {
            instance = new GAHelper(context.getApplicationContext());
        }

        return instance;
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

    public void reportActionToAnalytics(String category, String actionName) {
        reportActionToAnalytics(category, actionName, "");
    }

    public void reportActionToAnalytics(String category, String actionName, String label) {
        reportActionToAnalytics(category, actionName, label, 1);
    }

    public void reportCampaignParams(String campaignData) {
        tracker.send(new HitBuilders.ScreenViewBuilder()
            .setCampaignParamsFromUrl(campaignData)
            .build()
        );
    }

    public void reportActionToAnalytics(String category, String actionName, String label, long value,
                                        String... customValues) {
        EventBuilder builder = new EventBuilder()
                .setCategory(category)
                .setAction(actionName)
                .setLabel(label)
                .setValue(value);
        for (int i = 0; i < customValues.length; i++) {
            builder.setCustomDimension(i + 1, customValues[i]);
        }
        tracker.send(builder.build());
    }

    public static String getDateReportString(@Nullable Date date) {
        Date today = DateTimeUtils.toMidnight(Calendar.getInstance(),null).getTime();
        if (date == null || today.after(date)) {
            return "";
        }
        long numDaysAhead = TimeUnit.MILLISECONDS.toDays(date.getTime() - today.getTime());
        return "" + numDaysAhead + " days later";
    }
}
