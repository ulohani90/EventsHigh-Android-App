package com.eventshigh.nearme.app.utils;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.CustomUrlActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;
import com.google.android.gms.maps.model.LatLng;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

/**
 * Helper to process the intent in EventsHigh app.
 */
public class IntentUtils {
    public static final String EXTRA_EVENT_CONTEXT = IntentUtils.class.getCanonicalName() + "_PARAM";
    public static final String EXTRA_LATITUDE_PARAM = IntentUtils.class.getCanonicalName() + "_LAT";
    public static final String EXTRA_LONGITUDE_PARAM = IntentUtils.class.getCanonicalName() + "_LON";
    public static final String QUERY_ALL = "All";

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

        if (param.query.equalsIgnoreCase(QUERY_ALL)) {
            param.query = "";
        }

        if (inIntent.getDataString() != null) {
            activity.reportCampaignParams(inIntent.getDataString());
        }
    }

    private void processSearchIntent(Intent inIntent) {
        String query = inIntent.getStringExtra(SearchManager.QUERY);
        activity.reportActionToAnalytics("search", query);
        EventSearchSuggestionsProvider.saveRecentQuery(activity, query);

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

        if (isDeepLink) {
            String deepLinkName = "homepage";
            try {
                deepLinkName = inUri.getPathSegments().get(0);
            } catch (IndexOutOfBoundsException e) {
                // ignore.
            }
            activity.reportActionToAnalytics("deepLink", deepLinkName);
        }

        if (inUri.getPath().startsWith("/city")) {
            processCityViewIntent(inUri);
        } else if (inUri.getPath().startsWith("/search")) {
            processSearchViewIntent(inUri);
        } else if (inUri.getPath().startsWith("/detail")) {
            processDetailViewIntent(inUri);
        } else if (inUri.getPath().startsWith("/contest")) {
            processContestViewIntent(activity, inUri, null);
        } else if (inUri.getPath().startsWith("/browse")) {
            processBrowseViewIntent(inUri);
        }
    }

    private void processCityViewIntent(Uri webUri) {
        try {
            City city = City.valueOf(webUri.getLastPathSegment().toUpperCase());
            param.changeLocation(city.cityBounds.getCenter());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Invalid city in URI. Ignore.
        }
    }

    private void processSearchViewIntent(Uri webUri) {
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

    private void processBrowseViewIntent(Uri webUri) {
        try {
            List<String> pathSegments = webUri.getPathSegments();
            City city = City.valueOf(pathSegments.get(pathSegments.size() - 2).toUpperCase());
            param.changeLocation(city.cityBounds.getCenter());

            String query = pathSegments.get(pathSegments.size() - 1).split("-in-")[0];
            String dateQuery = DateTimeUtils.parseBrowseDate(query);
            if (dateQuery != null) {
                param.dateFilter = dateQuery;
            } else {
                param.query = URLDecoder.decode(query.toLowerCase(), "UTF-8").replace('+', ' ');
            }
        } catch (IndexOutOfBoundsException| IllegalArgumentException | NullPointerException | UnsupportedEncodingException e) {
            // Invalid city in URI. Ignore.
        }
    }

    private void processDetailViewIntent(Uri webUri) {
        activity.showEventDetails(webUri);
    }

    public static void processContestViewIntent(Context context, Uri webUri, @Nullable String title) {
        Intent intent = new Intent(context, CustomUrlActivity.class);
        intent.setData(webUri);
        if (title != null) {
            intent.putExtra(CustomUrlActivity.EXTRA_TITLE_KEY, title);
        }
        context.startActivity(intent);
    }
}
