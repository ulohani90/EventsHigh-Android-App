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
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Volley Request to fetch Featured events.
 */
public class FeaturedEventsRequest extends JsonRequest<List<Event>> {
    private static final int MAX_FEATURED_EVENTS = 5;

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
                              Listener<List<Event>> listener, ErrorListener errorListener) {
        if (eventsContext.city == null) {
            errorListener.onErrorResponse(new VolleyError("No City for: " + eventsContext.toString()));
            return;
        }

        String url = EventsHighEndpoints.getFeaturedEventsEndpoint(eventsContext.city);
        FeaturedEventsRequest request = new FeaturedEventsRequest(
                activity, url, eventsContext, shouldBypassCache, priority, listener, errorListener);
        request.setTag(activity);
        VolleyHelper.addToRequestQueue(activity, request);
    }

    private final BaseActivity activity;
    private final Priority priority;
    private final EventsContext eventsContext;
    private final EventsMarkerManager eventsMarkerManager;

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
    public FeaturedEventsRequest(BaseActivity activity, String url, EventsContext eventsContext,
                                 boolean shouldBypassCache, Priority priority,
                                 Listener<List<Event>> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(true);

        this.activity = activity;
        this.priority = priority;
        this.eventsContext = eventsContext;
        this.eventsMarkerManager = EventsMarkerManager.getInstance(activity);
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<List<Event>> parseNetworkResponse(NetworkResponse response) {
        new ReportTimingTask(activity, "featured_events").execute(response.networkTimeMs);

        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            JSONObject eventsJson = new JSONObject(jsonString);
            List<Event> events = Event.parseUpcomingEvents(
                    eventsContext, eventsMarkerManager, eventsJson, true);
            List<Event> filteredEvents = new ArrayList<>(MAX_FEATURED_EVENTS);

            for (Event event : events) {
                if (event.imgUrl != null) {
                    filteredEvents.add(event);
                    if (filteredEvents.size() == MAX_FEATURED_EVENTS) {
                        break;
                    }
                }
            }

            return Response.success(filteredEvents, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException e) {
            return Response.error(new ParseError(e));
        }
    }
}
