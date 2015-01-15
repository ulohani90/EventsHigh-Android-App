package com.eventshigh.nearme.app.utils;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseEventsActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventFetcherParam;

/**
 * Helper to process the intent in EventsHigh app.
 */
public class IntentUtils {

    public static EventFetcherParam processIntent(Activity activity, Intent inIntent) {
        EventFetcherParam param =
                inIntent.getParcelableExtra(BaseEventsActivity.EXTRA_EVENT_FETCHER_PARAM);
        if (param == null) {
            param = new EventFetcherParam(null, "");
        }

        IntentUtils utils = new IntentUtils(activity, param);
        if (Intent.ACTION_SEARCH.equals(inIntent.getAction())) {
            utils.processSearchIntent(inIntent);
        } else if (Intent.ACTION_VIEW.equals(inIntent.getAction())) {
            utils.processViewIntent(inIntent);
        } else if (BaseActivity.NOTIFICATION_ACTION.equals(inIntent.getAction())) {
            utils.reportToAnalytics("openNotification");
            utils.processViewIntent(inIntent);
        }

        return utils.param;
    }

    private final Activity activity;
    private EventFetcherParam param;

    private IntentUtils(Activity activity, EventFetcherParam param) {
        this.activity = activity;
        this.param = param;
    }

    private void processSearchIntent(Intent inIntent) {
        String query = inIntent.getStringExtra(SearchManager.QUERY);
        reportToAnalytics("search", query);

        Bundle appData = inIntent.getBundleExtra(SearchManager.APP_DATA);
        if (appData != null) {
            EventFetcherParam appDataParam =
                    appData.getParcelable(BaseEventsActivity.EXTRA_EVENT_FETCHER_PARAM);
            if (appDataParam != null) {
                param = appDataParam;
            }
        }

        param.query = query;
    }

    private void processViewIntent(Intent inIntent) {
        Uri inUri = inIntent.getData();

        if (inUri.getPath().startsWith("/city")) {
            processCityViewIntent(inUri);
        } else if (inUri.getPath().startsWith("/search")) {
            processSearchViewIntent(inUri);
        }

        reportToAnalytics("deepLink", "homepage");
    }

    private void processCityViewIntent(Uri webUri) {
        reportToAnalytics("deepLink", "city");

        try {
            City city = City.valueOf(webUri.getLastPathSegment().toUpperCase());
            param.changeLocation(city.cityBounds.getCenter());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Invalid city in URI. Ignore.
        }
    }

    private void processSearchViewIntent(Uri webUri) {
        reportToAnalytics("deepLink", "search");

        try {
            City city = City.valueOf(webUri.getQueryParameter("city").toUpperCase());
            param.changeLocation(city.cityBounds.getCenter());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Invalid city in URI. Ignore.
        }

        String query = webUri.getQueryParameter("interest");
        if (query != null) {
            param.query = query;
        }
    }

    private void reportToAnalytics(String action) {
        reportToAnalytics(action, "");
    }

    private void reportToAnalytics(String action, String label) {
        if (activity instanceof  BaseActivity) {
            ((BaseActivity) activity).reportActionToAnalytics(action, label);
        }

        GAHelper.getInstance(activity.getApplicationContext())
                .reportActionToAnalytics(activity.getClass().getSimpleName(), action, label, 1);
    }
}
