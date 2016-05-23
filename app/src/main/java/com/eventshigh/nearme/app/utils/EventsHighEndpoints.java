package com.eventshigh.nearme.app.utils;

import android.net.Uri;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.zendesk.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Defines various end points for EH api.
 */
public class EventsHighEndpoints {
    public static final String WEB_URI_BASE = "https://www.eventshigh.com/";
    public static final String API_URI_BASE = "https://api.eventshigh.com/";

    private static final String API_ENDPOINT_DATE_FORMAT =
            API_URI_BASE + "api/date/%s/%s?limit=100&mobile=1";
    private static final String API_ENDPOINT_DATE_CATEGORY_FORMAT =
            API_URI_BASE + "api/get_mobile_date_explore_screen/%s/%s?mobile=1";
    private static final String API_ENDPOINT_QUERY_FORMAT =
            API_URI_BASE + "api/events/%s/%s?limit=100&mobile=1";
    private static final String API_ENDPOINT_QUERY_DATE_FORMAT =
            API_URI_BASE + "api/events_for_interest_by_date/%s/%s/%s?limit=100&mobile=1";
    private static final String API_ENDPOINT_FEATURED_FORMAT =
            API_URI_BASE + "api/get_featured_events/%s?mobile=1";
    private static final String API_ENDPOINT_EVENT_UBER_FORMAT =
            API_URI_BASE + "api/get_event_uber_info/%s?mobile=1";
    private static final String API_ENDPOINT_MULTI_EVENT_UBER_FORMAT =
            API_URI_BASE + "api/get_event_uber_infos/%s?mobile=1";

    private static final String API_ENDPOINT_MULTI_EVENT_MOBILE_ID_FORMAT =
            API_URI_BASE + "api/get_events_for_mobile_user/%s?mobile=1";
    private static final String API_ENDPOINT_MOVIES_LIST_FORMAT =
            API_URI_BASE + "api/get_mobile_movies_home/%s";

    private static final String API_ENDPOINT_MULTI_MOVIE_UBER_FORMAT =
            API_URI_BASE + "api/get_movie_infos/%s/%s";


    private static final String API_ENDPOINT_MOVIE_UBER_INFO_FORMAT = API_URI_BASE + "api/get_movie_uber_info/%s";

    private static final String API_EVENTS_SUGGEST_FORMAT =
            "https://assets.eventshigh.com/autocomplete/autocomplete_events_%s.json";
    private static final String API_TAGS_SUGGEST_URL =
            "https://assets.eventshigh.com/autocomplete/autocomplete_tags.json";

    private static final String API_ENDPOINT_OFFERS_FORMAT = API_URI_BASE + "api/get_offers/%s";

    private static final String API_ENDPOINT_NOTIFICATIONS_FORMAT = API_URI_BASE + "api/get_latest_alerts/%s";

    public static final String QUERY_MY_EVENT = "my events";
    public static final String QUERY_MY_INTEREST_EVENTS = "my_interest_events";
    public static final String QUERY_FEATURED = "editor's picks";

    public static Uri getEventDetailsURI(Event event) {
        StringBuilder sb = new StringBuilder(event.id);
        String[] titleKgrams = event.title.replaceAll("\\p{C}", "").split("[\\p{Punct}\\s]+");
        for (int i = 0; i < 5 && i < titleKgrams.length; i++) {
            sb.append("-");
            sb.append(titleKgrams[i]);
        }

        return getEventDetailsURI(event.city, sb.toString());
    }

    public static Uri getEventShareURI(Event event, @Nullable String src) {
        return getEventDetailsURI(event.city, event.id).buildUpon()
                .appendQueryParameter("src", "ehm" + (src == null ? "" : "_" + src))
                .build();
    }

    public static String getEventSuggestURI(City city) {
        return String.format(API_EVENTS_SUGGEST_FORMAT, city.toString().toLowerCase());
    }

    public static String getTagSuggestURI() {
        return API_TAGS_SUGGEST_URL;
    }

    public static Uri getEventDetailsURI(City city, String eventId) {
        return Uri.parse(WEB_URI_BASE).buildUpon()
                .appendPath("detail")
                .appendPath(Utils.capitalize(city.toString()))
                .appendPath(eventId)
                .build();
    }

    @SuppressWarnings("deprecation")
    public static Uri getWebUri(EventsContext param) {
        Uri.Builder builder = Uri.parse(WEB_URI_BASE).buildUpon();
        String city = param.city == null ? null : param.city.toString().toLowerCase();

        if (param.query.isEmpty()) {
            if (city != null) {
                builder.appendPath("city").appendPath(city);
            }
            return builder.build();
        }

        if (city != null) {
            builder.appendPath(city);
            String browseQuery = DateTimeUtils.toBrowseDate(param.query);
            if (browseQuery == null) {
                browseQuery = URLEncoder.encode(param.query.toLowerCase());
            }
            builder.appendEncodedPath(browseQuery);
            return builder.build();
        } else {
            builder.appendPath("search");
            builder.appendQueryParameter("interest", param.query);
            return builder.build();
        }
    }

    public static String getFeaturedEventsEndpoint(City city) {
        return String.format(API_ENDPOINT_FEATURED_FORMAT, city.toString().toLowerCase());
    }

    public static String getDateCategoryEndpoint(City city, String date) {
        return String.format(API_ENDPOINT_DATE_CATEGORY_FORMAT, city.toString().toLowerCase(), date);
    }

    public static String getApiEndpoint(EventsContext eventsContext) throws IllegalArgumentException {
        if (eventsContext.city == null) {
            throw new IllegalArgumentException("city is not passed");
        }

        if (eventsContext.query.isEmpty() || isMyEventQuery(eventsContext.query)) {
            return String.format(API_ENDPOINT_DATE_FORMAT,
                    eventsContext.city.toString().toLowerCase(),
                    eventsContext.dateFilter.isEmpty() ? "this+week" : eventsContext.dateFilter);
        }

        if (isFeaturedEventQuery(eventsContext.query)) {
            return getFeaturedEventsEndpoint(eventsContext.city);
        }

        try {
            if (isDateQuery(eventsContext.query)) {
                return String.format(API_ENDPOINT_DATE_FORMAT,
                        eventsContext.city.toString().toLowerCase(),
                        URLEncoder.encode(eventsContext.query, "UTF-8"));
            }

            if (eventsContext.dateFilter.isEmpty()) {
                return String.format(API_ENDPOINT_QUERY_FORMAT,
                        eventsContext.city.toString().toLowerCase(),
                        URLEncoder.encode(eventsContext.query, "UTF-8"));
            }

            return String.format(API_ENDPOINT_QUERY_DATE_FORMAT,
                    eventsContext.city.toString().toLowerCase(),
                    URLEncoder.encode(eventsContext.query, "UTF-8"),
                    eventsContext.dateFilter);
        } catch (UnsupportedEncodingException e) {
            Crashlytics.getInstance().core.logException(e);
            throw new IllegalArgumentException(e);
        }
    }

    public static String getApiEndPointForOffers(String cityName) {
        return String.format(API_ENDPOINT_OFFERS_FORMAT, cityName);
    }

    public static String getApiEndPointForAlerts(String cityName) {
        return String.format(API_ENDPOINT_NOTIFICATIONS_FORMAT, cityName);
    }

    public static String getApiEndpointEventUber(String eventId) {
        return String.format(API_ENDPOINT_EVENT_UBER_FORMAT, eventId);
    }

    public static String getApiEndpointForMoviesList(String cityName) {
        return String.format(API_ENDPOINT_MOVIES_LIST_FORMAT, cityName);

    }

    public static String getApiEndpointEventsUber(List<String> eventIds) {
        Collections.sort(eventIds);
        return String.format(API_ENDPOINT_MULTI_EVENT_UBER_FORMAT, StringUtils.toCsvString(eventIds));
    }


    public static String getApiEndpointMoviesUber(List<String> movieIds, String cityName) {
        Collections.sort(movieIds);
        return String.format(API_ENDPOINT_MULTI_MOVIE_UBER_FORMAT, StringUtils.toCsvString(movieIds), cityName);
    }

    public static String getApiEndpointEventsMobileUser(String androidId) {

        return String.format(API_ENDPOINT_MULTI_EVENT_MOBILE_ID_FORMAT, androidId);
    }

    public static String getApiEndpointBlogEntry(String blogUrl) {
        return Uri.parse(API_URI_BASE + "api/get_blog_post").buildUpon()
                .appendQueryParameter("url", blogUrl).build().toString();
    }

    public static String getApiEndpointBlogFeed(City city) {
        return API_URI_BASE + "api/get_blog_posts/" + city.toString().toLowerCase();
    }

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    public static boolean isDateQuery(String query) {
        // this week, this weekend are valid date queries
        return query.toLowerCase().startsWith("this") || DATE_PATTERN.matcher(query).matches();
    }

    public static boolean isMyEventQuery(String query) {
        return query.equalsIgnoreCase(QUERY_MY_EVENT);
    }

    public static boolean isMyInterestEventQuery(String query) {
        return query.equalsIgnoreCase(QUERY_MY_INTEREST_EVENTS);
    }

    public static boolean isFeaturedEventQuery(String query) {
        return query.equalsIgnoreCase(QUERY_FEATURED);
    }

    public static String getApiEndpointForMovieUberDetail(int id) {
        return String.format(API_ENDPOINT_MOVIE_UBER_INFO_FORMAT, id + "");
    }
}
