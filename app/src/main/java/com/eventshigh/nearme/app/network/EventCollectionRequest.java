package com.eventshigh.nearme.app.network;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Volley Request to fetch Events collections.
 */
public class EventCollectionRequest extends JsonRequest<List<Event>> {
    /**
     * Helper method to submit a volley request to fetch Events information.
     *
     * @param activity an application eventsContext to initiate the volley.
     * @param eventsContext EventsContext representing the request.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(BaseActivity activity, EventsContext eventsContext,
                              Priority priority, boolean shouldBypassCache,
                              boolean includeWithoutLocation,
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
                activity, url, eventsContext, priority, shouldBypassCache, includeWithoutLocation,
                listener, errorListener);
        request.setTag(activity);
        VolleyHelper.addToRequestQueue(activity, request);
    }

    private final BaseActivity activity;
    private final EventsContext eventsContext;
    private final Priority priority;
    private final EventsMarkerManager eventsMarkerManager;
    private final boolean includeWithoutLocation;

    /**
     * Creates a new request.
     *
     * @param activity application context.
     * @param url URL to fetch the JSON from.
     * @param shouldBypassCache true if local cache should be bypassed
     * @param priority priority of request.
     * @param listener Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public EventCollectionRequest(BaseActivity activity, String url, EventsContext eventsContext,
                                  Priority priority, boolean shouldBypassCache, boolean includeWithoutLocation,
                                  Listener<List<Event>> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(true);

        this.activity = activity;
        this.eventsContext = eventsContext;
        this.priority = priority;
        this.eventsMarkerManager = EventsMarkerManager.getInstance(activity);
        this.includeWithoutLocation = includeWithoutLocation;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<List<Event>> parseNetworkResponse(NetworkResponse response) {
        new ReportTimingTask(activity, "events").execute(response.networkTimeMs);

        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            JSONObject eventsJson = new JSONObject(jsonString);
            List<Event> events = Event.parseUpcomingEvents( eventsContext.city, eventsJson,
                    includeWithoutLocation);

            // if its my events, filter out the events which user has favourited or is following.
            eventsMarkerManager.waitForLoading();
            if (EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
                for (Iterator<Event> it =  events.iterator(); it.hasNext(); ) {
                    Event event = it.next();
                    if (! eventsMarkerManager.isFavourite(event.id)) {
                        it.remove();
                    }
                }
            }

            // Sort the event list to user.
            if (eventsContext.location != null) {
                Collections.sort(events, new EventComparator(eventsContext.location, eventsMarkerManager));
            }

            return Response.success(events, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException e) {
            return Response.error(new ParseError(e));
        }
    }
}
