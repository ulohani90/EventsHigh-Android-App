package com.eventshigh.nearme.app.utils;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.CustomUrlActivity;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper to process the intent in EventsHigh app.
 */
public class IntentUtils {
    public static final String EXTRA_EVENT_CONTEXT = IntentUtils.class.getCanonicalName() + "_PARAM";
    public static final String QUERY_ALL = "All";
    public static final Set<String> days = new HashSet<>(8);
    static {
        days.add("today");
        days.add("today");
    }

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
        activity.finish();
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
