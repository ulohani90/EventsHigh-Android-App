package com.eventshigh.nearme.app.utils;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.CustomUrlActivity;
import com.eventshigh.nearme.app.activity.EventDetailActivity;
import com.eventshigh.nearme.app.activity.LaunchActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.List;

/**
 * Helper to process the intent in EventsHigh app.
 */
public class IntentUtils {
    public static final String EXTRA_EVENT_CONTEXT = IntentUtils.class.getCanonicalName() + "_PARAM";
    public static final String QUERY_ALL = "All";

    public static EventsContext processIntent(BaseContextActivity activity, Intent inIntent) {
        IntentUtils utils = new IntentUtils(activity);
        utils.processIntent(inIntent);
        return utils.param;
    }

    private final BaseContextActivity activity;
    private EventsContext param;

    private IntentUtils(BaseContextActivity activity) {
        this.activity = activity;
    }

    private void processIntent(Intent inIntent) {
        param = inIntent.getParcelableExtra(EXTRA_EVENT_CONTEXT);
        if (param == null) {
            param = new EventsContext(null, "");
        }

        if (inIntent.getAction() != null) {
            if (Intent.ACTION_SEARCH.equals(inIntent.getAction())) {
                processSearchIntent(inIntent);
            } else if (Intent.ACTION_VIEW.equals(inIntent.getAction())) {
                processViewIntent(inIntent, true);
            } else if (inIntent.getAction().startsWith(BaseActivity.NOTIFICATION_ACTION)) {
                activity.reportActionToAnalytics("openNotification", param.query);
                processViewIntent(inIntent, false);
            }
        }

        if (param.query.equalsIgnoreCase(QUERY_ALL) && !param.dateFilter.isEmpty()) {
            param.query = param.dateFilter;
            param.dateFilter = "";
        }
        if (param.query.equalsIgnoreCase(QUERY_ALL)) {
            param.query = "";
        }
        if (param.query.endsWith("day")) {
            if (param.query.equalsIgnoreCase("today")) {
                param.query = "";
                param.setDateFilter(Calendar.getInstance());
            } else {
                try {
                    Integer day = (Integer) Calendar.class.getField(param.query.toUpperCase()).get(null);
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.DAY_OF_MONTH, (7 + day - calendar.get(Calendar.DAY_OF_WEEK)) % 7);
                    param.query = "";
                    param.setDateFilter(calendar);
                } catch (Exception e) {
                    // ignore.
                    Crashlytics.logException(e);
                }
            }
        }

        if (inIntent.getDataString() != null) {
            activity.reportCampaignParams(inIntent.getDataString());
        }
    }

    private void processSearchIntent(Intent inIntent) {
        String query = inIntent.getStringExtra(SearchManager.QUERY);
        activity.reportActionToAnalytics("search", query);
        EventSearchSuggestionsProvider.saveRecentQuery(activity, query.toLowerCase());

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
        if (inUri == null) {
            return;
        }

        if (isDeepLink) {
            String deepLinkName = "homepage";
            try {
                deepLinkName = inUri.getPathSegments().get(0);
            } catch (IndexOutOfBoundsException e) {
                // ignore.
                Crashlytics.logException(e);
            }
            activity.reportActionToAnalytics("deepLink", deepLinkName);
        }

        if (inUri.getPath().startsWith("/city")) {
            processCityViewIntent(inUri);
        } else if (inUri.getPath().startsWith("/search")) {
            processSearchViewIntent(inUri);
        } else if (inUri.getPath().startsWith("/detail")) {
            processDetailViewIntent(inUri);
        } else if (inUri.getPath().startsWith("/get_event_contest")) {
            CustomUrlActivity.launchCustomUrl(activity, inUri, null);
            activity.finish();
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
            Crashlytics.logException(e);
        }
    }

    private void processSearchViewIntent(Uri webUri) {
        try {
            City city = City.valueOf(webUri.getQueryParameter("city").toUpperCase());
            param.changeLocation(city.cityBounds.getCenter());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Invalid city in URI. Ignore.
            Crashlytics.logException(e);
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
                param.query = dateQuery;
            } else {
                param.query = URLDecoder.decode(query.toLowerCase(), "UTF-8").replace('+', ' ');
            }
        } catch (IndexOutOfBoundsException| IllegalArgumentException | NullPointerException | UnsupportedEncodingException e) {
            // Invalid city in URI. Ignore.
            Crashlytics.logException(e);
        }
    }

    private void processDetailViewIntent(Uri webUri) {
        activity.showEventDetails(webUri, "deeplink");
        activity.finish();
    }

    public static Intent createIntent(Context context, String eventId, City city) {
        if (city == null) {
            // placeholder for city.
            city = City.BANGALORE;
        }

        Intent intent = new Intent(context, EventDetailActivity.class);
        intent.setAction(BaseActivity.NOTIFICATION_ACTION);
        intent.setData(EventsHighEndpoints.getEventDetailsURI(city, eventId));
        return intent;
    }

    public static Intent createIntent(Context context, String query) {
        Intent intent = new Intent(context, LaunchActivity.class);
        intent.setAction(BaseActivity.NOTIFICATION_ACTION + query);
        intent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, new EventsContext(null, query));
        return intent;
    }
}
