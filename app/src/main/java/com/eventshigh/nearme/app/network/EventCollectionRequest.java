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
import com.eventshigh.nearme.app.data.EventsCollection;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Volley Request to fetch Events collections.
 */
public class EventCollectionRequest extends JsonRequest<EventsCollection> {
    /**
     * Helper method to submit a volley request to fetch Events information.
     *
     * @param activity an application eventsContext to initiate the volley.
     * @param eventsContext EventFetcherParam representing the request.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(BaseActivity activity, EventsContext eventsContext,
                              Priority priority, boolean shouldBypassCache,
                              Listener<EventsCollection> listener, ErrorListener errorListener) {
        if (eventsContext.city == null) {
            errorListener.onErrorResponse(new VolleyError("No City for: " + eventsContext.toString()));
            return;
        }

        String url;
        if (eventsContext.query.isEmpty()) {
            url = EventsHighEndpoints.getApiEndpointDate(eventsContext.city);
        } else {
            try {
                url = EventsHighEndpoints.getApiEndpointQuery(eventsContext.city, eventsContext.query);
            } catch (UnsupportedEncodingException e) {
                errorListener.onErrorResponse(new VolleyError("Invalid Query", e));
                return;
            }
        }

        EventCollectionRequest request = new EventCollectionRequest(
                activity, url, eventsContext, shouldBypassCache, priority, listener, errorListener);
        request.setTag(activity);
        VolleyHelper.addToRequestQueue(activity, request);
    }

    private final BaseActivity activity;
    private final EventsContext param;
    private final Priority priority;
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
    public EventCollectionRequest(BaseActivity activity, String url, EventsContext param,
                                  boolean shouldBypassCache, Priority priority,
                                  Listener<EventsCollection> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(true);

        this.activity = activity;
        this.param = param;
        this.priority = priority;
        this.eventsMarkerManager = EventsMarkerManager.getInstance(activity);
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<EventsCollection> parseNetworkResponse(NetworkResponse response) {
        new ReportTimingTask(activity, "events").execute(response.networkTimeMs);

        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            JSONObject eventsJson = new JSONObject(jsonString);
            return Response.success(Event.parseUpcomingEvents(param, eventsMarkerManager, eventsJson),
                HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException e) {
            return Response.error(new ParseError(e));
        }
    }
}
