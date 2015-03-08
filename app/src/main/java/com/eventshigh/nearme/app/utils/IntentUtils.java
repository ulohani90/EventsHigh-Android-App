package com.eventshigh.nearme.app.utils;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.EventDetailActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventsContext;
import com.google.android.gms.maps.model.LatLng;

/**
 * Helper to process the intent in EventsHigh app.
 */
public class IntentUtils {
    public static final String EXTRA_EVENT_CONTEXT = IntentUtils.class.getCanonicalName() + "_PARAM";
    public static final String EXTRA_LATITUDE_PARAM = IntentUtils.class.getCanonicalName() + "_LAT";
    public static final String EXTRA_LONGITUDE_PARAM = IntentUtils.class.getCanonicalName() + "_LON";

    public static EventsContext processIntent(BaseActivity activity, Intent inIntent) {
        IntentUtils utils = new IntentUtils(activity);
        utils.processIntent(inIntent);
        return utils.param;
    }

    private final BaseActivity activity;
    private EventsContext param;

    private IntentUtils(BaseActivity activity) {
        this.activity = activity;
    }

    private void processIntent(Intent inIntent) {
        param = inIntent.getParcelableExtra(EXTRA_EVENT_CONTEXT);
        if (param == null) {
            param = new EventsContext(null, "");
        }

        if (inIntent.hasExtra(EXTRA_LATITUDE_PARAM) && inIntent.hasExtra(EXTRA_LONGITUDE_PARAM)) {
            param.changeLocation(new LatLng(
                    inIntent.getDoubleExtra(EXTRA_LATITUDE_PARAM, 0),
                    inIntent.getDoubleExtra(EXTRA_LONGITUDE_PARAM, 0)));
        }

        if (Intent.ACTION_SEARCH.equals(inIntent.getAction())) {
            processSearchIntent(inIntent);
        } else if (Intent.ACTION_VIEW.equals(inIntent.getAction())) {
            processViewIntent(inIntent, true);
        } else if (BaseActivity.NOTIFICATION_ACTION.equals(inIntent.getAction())) {
            activity.reportActionToAnalytics("openNotification");
            processViewIntent(inIntent, false);
        }

        if (inIntent.getDataString() != null) {
            activity.reportCampaignParams(inIntent.getDataString());
        }
    }

    private void processSearchIntent(Intent inIntent) {
        String query = inIntent.getStringExtra(SearchManager.QUERY);
        activity.reportActionToAnalytics("search", query);

        Bundle appData = inIntent.getBundleExtra(SearchManager.APP_DATA);
        if (appData != null) {
            EventsContext appDataParam =
                    appData.getParcelable(EXTRA_EVENT_CONTEXT);
            if (appDataParam != null) {
                param = appDataParam;
            }
        }

        param.query = query;
    }

    private void processViewIntent(Intent inIntent, boolean isDeepLink) {
        Uri inUri = inIntent.getData();

        if (inUri.getPath().startsWith("/city")) {
            processCityViewIntent(inUri, isDeepLink);
        } else if (inUri.getPath().startsWith("/search")) {
            processSearchViewIntent(inUri, isDeepLink);
        } else if (inUri.getPath().startsWith("/detail")) {
            processDetailViewIntent(inUri, isDeepLink);
        } else if (isDeepLink) {
            activity.reportActionToAnalytics("deepLink", "homepage");
        }
    }

    private void processCityViewIntent(Uri webUri, boolean isDeepLink) {
        if (isDeepLink) {
            activity.reportActionToAnalytics("deepLink", "city");
        }

        try {
            City city = City.valueOf(webUri.getLastPathSegment().toUpperCase());
            param.changeLocation(city.cityBounds.getCenter());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Invalid city in URI. Ignore.
        }
    }

    private void processSearchViewIntent(Uri webUri, boolean isDeepLink) {
        if (isDeepLink) {
            activity.reportActionToAnalytics("deepLink", "search");
        }

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

    private void processDetailViewIntent(Uri webUri, boolean isDeepLink) {
        if (isDeepLink) {
            activity.reportActionToAnalytics("deepLink", "detail");
        }
        Intent intent = new Intent(activity, EventDetailActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(webUri);
        activity.startActivity(intent);
    }
}
