package com.eventshigh.nearme.app.utils;

import android.net.Uri;

import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;

public class EventsHighEndpoints {
    public static final String WEB_URI_BASE = "http://www.eventshigh.com/";
    private static final String API_ENDPOINT_DATE_FORMAT =
            "http://apiserver.eventshigh.com:8888/api/date/%s/%s?sortby=popularity&limit=200&mobile=1";
    private static final String API_ENDPOINT_QUERY_FORMAT =
            "http://apiserver.eventshigh.com:8888/api/events/%s/%s?sortby=popularity&limit=200&mobile=1";
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

    public static Uri getWebUri(EventFetcherParam param) {
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

    public static String getApiEndpointDate(City city, Date date) {
        return String.format(API_ENDPOINT_DATE_FORMAT,
                city.toString().toLowerCase(), DateTimeUtils.getDateString(date));
    }
    public static String getApiEndpointQuery(City city, String query) throws UnsupportedEncodingException {
        return String.format(API_ENDPOINT_QUERY_FORMAT,
                city.toString().toLowerCase(), URLEncoder.encode(query, "UTF-8"));
    }
    public static String getApiEndpointEventUber(String eventId) {
        return String.format(API_ENDPOINT_EVENT_UBER_FORMAT, eventId);
    }
}
