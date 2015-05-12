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
import java.util.Iterator;
import java.util.List;

/**
 * Volley Request to fetch Events collections.
 */
public class EventCollectionRequest extends BaseEventListRequest {
    /**
     * Helper method to submit a volley request to fetch Events information.
     *
     * @param context an application eventsContext to initiate the volley.
     * @param eventsContext EventsContext representing the request.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, EventsContext eventsContext, Priority priority,
                              Object tag, boolean shouldBypassCache, boolean includeWithoutLocation,
                              Listener<List<Event>> listener, ErrorListener errorListener) {
        if (eventsContext.city == null) {
            errorListener.onErrorResponse(new VolleyError("No City for: " + eventsContext.toString()));
            return;
        }

        String url;
        try {
            url = EventsHighEndpoints.getApiEndpoint(eventsContext);
        } catch (IllegalArgumentException e) {
            errorListener.onErrorResponse(new VolleyError("Invalid Query", e));
            return;
        }

        EventCollectionRequest request = new EventCollectionRequest(
                context, url, eventsContext, priority, shouldBypassCache, includeWithoutLocation,
                listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }

    public EventCollectionRequest(Context context, String url, EventsContext eventsContext,
          Priority priority, boolean shouldBypassCache, boolean includeWithoutLocation,
          Listener<List<Event>> listener, ErrorListener errorListener) {
        super(context, url, eventsContext, priority, shouldBypassCache, includeWithoutLocation,
                listener, errorListener);
    }

    @Override
    protected Response<List<Event>> parseNetworkResponse(NetworkResponse response) {
        try {
            // Parse the response.
            List<Event> events = parseEventsFromNetworkResponse(response);

            // In case of MyEvents request, filter out the events which user has favourited.
            if (EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
                for (Iterator<Event> it =  events.iterator(); it.hasNext(); ) {
                    Event event = it.next();
                    if (! eventsMarkerManager.isFavourite(event.id)) {
                        it.remove();
                    }
                }
            }

            return Response.success(events, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException e) {
            VolleyHelper.getInstance(context).invalidateCache(this);
            return Response.error(new ParseError(e));
        }
    }
}
