package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.TrendingTopic;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest.EventCollection;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Volley Request to fetch Featured events.
 */
public class FeaturedEventsRequest extends JsonRequest<EventCollection> {
    private static final int MAX_FEATURED_EVENTS = 8;

    public static class EventCollection {
        public final List<Event> events;
        public final List<TrendingTopic> trendingTopics;

        public EventCollection(List<Event> events, List<TrendingTopic> trendingTopics) {
            this.events = events;
            this.trendingTopics = trendingTopics;
        }
    }

    /**
     * Helper method to submit a volley request to fetch Events information.
     *
     * @param context an application context to initiate the volley.
     * @param eventsContext EventsContext representing the request.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, EventsContext eventsContext,
                              Priority priority, Object tag, boolean shouldBypassCache,
                              Listener<EventCollection> listener, ErrorListener errorListener) {
        if (eventsContext.city == null) {
            errorListener.onErrorResponse(new VolleyError("No City for: " + eventsContext.toString()));
            return;
        }

        String url = EventsHighEndpoints.getFeaturedEventsEndpoint(eventsContext.city);
        FeaturedEventsRequest request = new FeaturedEventsRequest(
                context, url, eventsContext, shouldBypassCache, priority, listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final Context context;
    private final EventsContext eventsContext;
    private final Priority priority;

    public FeaturedEventsRequest(Context context, String url, EventsContext eventsContext,
                                 boolean shouldBypassCache, Priority priority,
                                 Listener<EventCollection> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(true);

        this.context = context;
        this.eventsContext = eventsContext;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<EventCollection> parseNetworkResponse(NetworkResponse response) {
        ReportTimingTask.report(context, "featured-events", response.networkTimeMs);

        try {
            // Parse the response.
            List<Event> events = EventCollectionRequest.parseEventsFromNetworkResponse(response,
                    context, eventsContext, true).events;
            List<Event> filteredEvents = new ArrayList<>(MAX_FEATURED_EVENTS);
            for (Event event : events) {
                if (event.imgUrl != null) {
                    filteredEvents.add(event);
                    if (filteredEvents.size() == MAX_FEATURED_EVENTS) {
                        break;
                    }
                }
            }

            // Parse Trending topics.
            List<TrendingTopic> trendingTopics = new ArrayList<>();
            trendingTopics.add(new TrendingTopic("Comedy shows", "http://martinmayhew.com/wordpress/wp-content/uploads/2012/06/528279_351279038257615_2065667218_n1.jpg", null));
            trendingTopics.add(new TrendingTopic("Bengaluru walking tours", "http://cntcdn.cntraveller.in/sites/default/files/Bengaluru.jpg", null));
            trendingTopics.add(new TrendingTopic("Dance", "https://upload.wikimedia.org/wikipedia/commons/4/4f/Modern_Indian_dance,_India_Habitat_Centre,_New_Delhi.jpg", null));
            trendingTopics.add(new TrendingTopic("Kids", "http://images.indianexpress.com/2014/05/schools-m.jpg", null));

            return Response.success(new EventCollection(filteredEvents, trendingTopics),
                HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }
}
