package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.support.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.JsonRequest;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.network.BaseEventListRequest.EventCollection;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Base class for request to fetch event list.
 */
public abstract class BaseEventListRequest extends JsonRequest<EventCollection>  {
    public static final class TrendingTopic {
        public final String tagName;
        public final String imgUrl;

        public TrendingTopic(String tagName, String imgUrl) {
            this.tagName = tagName;
            this.imgUrl = imgUrl;
        }

        public static @Nullable TrendingTopic parse(@Nullable JSONObject json) {
            if (json == null) {
                return  null;
            }

            String name = json.optString("display_name");
            String imgUrl = json.optString("img_url");
            if (name == null || imgUrl == null) {
                return null;
            }

            return new TrendingTopic(name, imgUrl);
        }
    }

    public static class EventCollection {
        public final List<Event> events;
        public final List<TrendingTopic> trendingTopics;

        public EventCollection(List<Event> events, List<TrendingTopic> trendingTopics) {
            this.events = events;
            this.trendingTopics = trendingTopics;
        }
    }

    protected final Context context;
    protected final EventsContext eventsContext;
    private final Priority priority;
    protected final EventsMarkerManager eventsMarkerManager;
    private final boolean includeWithoutLocation;

    /**
     * Creates a new request.
     *
     * @param context application context.
     * @param url URL to fetch the JSON from.
     * @param shouldBypassCache true if local cache should be bypassed
     * @param priority priority of request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public BaseEventListRequest(Context context, String url, EventsContext eventsContext,
                                Priority priority, boolean shouldBypassCache, boolean includeWithoutLocation,
                                Listener<EventCollection> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(true);

        this.context = context;
        this.eventsContext = eventsContext;
        this.priority = priority;
        this.eventsMarkerManager = EventsMarkerManager.getInstance(context);
        this.includeWithoutLocation = includeWithoutLocation;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    protected EventCollection parseEventsFromNetworkResponse(NetworkResponse response)
            throws UnsupportedEncodingException, JSONException {
        new ReportTimingTask(context, "events").execute(response.networkTimeMs);

        String jsonString = new String(response.data, "UTF-8");
        JSONObject eventsJson = new JSONObject(jsonString);
        List<Event> events = Event.parseUpcomingEvents(eventsContext.city, eventsJson,
                includeWithoutLocation);

        // Filter out the events which has started more than two hours back.
        long twoHoursBack = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(2);
        for (Iterator<Event> it =  events.iterator(); it.hasNext(); ) {
            Event event = it.next();
            EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
            if (eventTime != null && eventTime.time != null && event.eventTimings[0] < twoHoursBack) {
                // The event has started more than two hours back. We filter it out either if
                // it has not future occurrences or if its a date query.
                if (event.eventTimings.length == 1 || !eventsContext.dateFilter.isEmpty()) {
                    it.remove();
                }
            }
        }

        // Filter out the event which belongs to user selected filter.
        if (!eventsContext.categoryFilters.isEmpty()) {
            Set<String> categoryFiltersSet = new HashSet<>(eventsContext.categoryFilters);
            for (Iterator<Event> iterator  = events.iterator(); iterator.hasNext(); ) {
                Event event = iterator.next();
                boolean found = false;
                for (String tag : event.tags) {
                    if (categoryFiltersSet.contains(tag)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    iterator.remove();
                }
            }
        }

        // Sort the event list to user.
        eventsMarkerManager.waitForLoading();
        Collections.sort(events, new EventComparator(eventsContext.location, eventsMarkerManager));

        // Parse Trending topics.
        List<TrendingTopic> trendingTopics = new ArrayList<>();
        JSONArray trendingTopicsJSON = eventsJson.optJSONArray("trending_topics");
        if (eventsContext.city == City.BANGALORE && trendingTopicsJSON != null) {
            for (int i = 0; i < trendingTopicsJSON.length(); i++) {
                TrendingTopic trendingTopic = TrendingTopic.parse(trendingTopicsJSON.optJSONObject(i));
                if (trendingTopic != null) {
                    trendingTopics.add(trendingTopic);
                }
            }
        }

        return new EventCollection(events, trendingTopics);
    }
}
