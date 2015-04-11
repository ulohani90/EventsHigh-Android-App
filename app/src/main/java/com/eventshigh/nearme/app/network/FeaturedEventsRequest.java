package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Volley Request to fetch Featured events.
 */
public class FeaturedEventsRequest extends BaseEventListRequest {
    private static final int MAX_FEATURED_EVENTS = 8;

    /**
     * Helper method to submit a volley request to fetch Events information.
     *
     * @param context an application context to initiate the volley.
     * @param eventsContext EventsContext representing the request.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, EventsContext eventsContext,
                              Priority priority, boolean shouldBypassCache,
                              Listener<EventCollection> listener, ErrorListener errorListener) {
        if (eventsContext.city == null) {
            errorListener.onErrorResponse(new VolleyError("No City for: " + eventsContext.toString()));
            return;
        }

        String url = EventsHighEndpoints.getFeaturedEventsEndpoint(eventsContext.city);
        FeaturedEventsRequest request = new FeaturedEventsRequest(
                context, url, eventsContext, shouldBypassCache, priority, listener, errorListener);
        request.setTag(context);
        VolleyHelper.addToRequestQueue(context, request);
    }

    public FeaturedEventsRequest(Context context, String url, EventsContext eventsContext,
                                 boolean shouldBypassCache, Priority priority,
                                 Listener<EventCollection> listener, ErrorListener errorListener) {
        super(context, url, eventsContext, priority, shouldBypassCache, true, listener,
                errorListener);
    }

    @Override
    protected Response<EventCollection> parseNetworkResponse(NetworkResponse response) {
        try {
            // Parse the response.
            EventCollection eventCollection = parseEventsFromNetworkResponse(response);

            List<Event> filteredEvents = new ArrayList<>(MAX_FEATURED_EVENTS);
            for (Event event : eventCollection.events) {
                if (event.imgUrl != null) {
                    filteredEvents.add(event);
                    if (filteredEvents.size() == MAX_FEATURED_EVENTS) {
                        break;
                    }
                }
            }

            return Response.success(
                new EventCollection(filteredEvents, eventCollection.trendingTopics),
                HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException e) {
            return Response.error(new ParseError(e));
        }
    }
}
