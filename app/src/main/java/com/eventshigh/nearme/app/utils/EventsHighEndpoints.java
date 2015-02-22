package com.eventshigh.nearme.app.utils;

import android.net.Uri;

import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Pattern;

/**
 * Defines various end points for EH api.
 */
public class EventsHighEndpoints {
    private static final String WEB_URI_BASE = "http://www.eventshigh.com/";
    private static final String API_ENDPOINT_DATE_FORMAT =
            "http://apiserver.eventshigh.com:8888/api/date/%s/%s?limit=500&mobile=%d";
    private static final String API_ENDPOINT_QUERY_FORMAT =
            "http://apiserver.eventshigh.com:8888/api/events/%s/%s?limit=200&mobile=%d";
    private static final String API_ENDPOINT_FEATURED_FORMAT =
            "http://apiserver.eventshigh.com:8888/api/get_featured_events/%s?mobile=%d";
    private static final String API_ENDPOINT_EVENT_UBER_FORMAT =
            "http://apiserver.eventshigh.com:8888/api/get_event_uber_info/%s?mobile=1";

    public static Uri getEventDetailsURI(Event event) {
        StringBuilder sb = new StringBuilder(event.id);
        String [] titleKgrams = event.title.replaceAll("\\p{C}", "").split("[\\p{Punct}\\s]+");
        for (int i = 0; i < 5 && i < titleKgrams.length; i++) {
            sb.append("-");
            sb.append(titleKgrams[i]);
        }

        return getEventDetailsURI(event.city, sb.toString());
    }

    public static Uri getEventDetailsURI(City city, String eventId) {
        return Uri.parse(WEB_URI_BASE).buildUpon()
                .appendPath("detail")
                .appendPath(Utils.capitalize(city.toString()))
                .appendPath(eventId)
                .build();
    }

    public static Uri getWebUri(EventsContext param) {
        Uri.Builder builder = Uri.parse(WEB_URI_BASE).buildUpon();
        if (param.query.isEmpty()) {
            if (param.city != null) {
                builder.appendPath("city").appendPath(param.city.toString().toLowerCase());
            }
            return builder.build();
        }

        builder.appendPath("search");
        if (param.city != null) {
            builder.appendQueryParameter("city", param.city.toString().toLowerCase());
        }
        return builder.appendQueryParameter("interest", param.query).build();
    }

    public static String getFeaturedEventsEndpoint(City city) {
        return String.format(API_ENDPOINT_FEATURED_FORMAT,
                city.toString().toLowerCase(),
                BuildConfig.VERSION_CODE);
    }

    public static String getApiEndpoint(EventsContext eventsContext) throws IllegalArgumentException {
        if (eventsContext.city == null) {
            throw new IllegalArgumentException("city is not passed");
        }

        if (eventsContext.query.isEmpty()) {
            return String.format(API_ENDPOINT_DATE_FORMAT,
                    eventsContext.city.toString().toLowerCase(),
                    eventsContext.dateFilter.isEmpty() ? "this%20week" : eventsContext.dateFilter,
                    BuildConfig.VERSION_CODE);
        }

        try {
            if (isDateQuery(eventsContext.query)) {
                return String.format(API_ENDPOINT_DATE_FORMAT,
                        eventsContext.city.toString().toLowerCase(),
                        URLEncoder.encode(eventsContext.query, "UTF-8"), BuildConfig.VERSION_CODE);
            }

            String url = String.format(API_ENDPOINT_QUERY_FORMAT,
                        eventsContext.city.toString().toLowerCase(),
                        URLEncoder.encode(eventsContext.query, "UTF-8"), BuildConfig.VERSION_CODE);
            if (eventsContext.dateFilter.isEmpty()) {
                url += "&date=" + eventsContext.dateFilter;
            }
            return url;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String getApiEndpointEventUber(String eventId) {
        return String.format(API_ENDPOINT_EVENT_UBER_FORMAT, eventId);
    }

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    public static boolean isDateQuery(String query) {
        // this week, this weekend are valid date queries
        return query.toLowerCase().startsWith("this") ||  DATE_PATTERN.matcher(query).matches();
    }
}
