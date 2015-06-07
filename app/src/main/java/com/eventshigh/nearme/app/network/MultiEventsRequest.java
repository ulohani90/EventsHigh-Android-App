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
import com.eventshigh.nearme.app.data.EventComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Fetches the multiple events information by their id.
 */
public class MultiEventsRequest extends JsonRequest<List<Event>> {
    /**
     * Helper method to submit a volley request to fetch Events information.
     *
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, EventsContext eventsContext, List<String> eventIds,
            Priority priority, Object tag, boolean shouldBypassCache, boolean includeWithoutLocation,
            Listener<List<Event>> listener, ErrorListener errorListener) {
        String url;
        try {
            url = EventsHighEndpoints.getApiEndpointEventsUber(eventIds);
        } catch (IllegalArgumentException e) {
            errorListener.onErrorResponse(new VolleyError("Invalid Query", e));
            return;
        }

        MultiEventsRequest request = new MultiEventsRequest(eventsContext, url, priority,
                shouldBypassCache, includeWithoutLocation, listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final EventsContext eventsContext;
    private final Priority priority;
    private final boolean includeWithoutLocation;

    public MultiEventsRequest(EventsContext eventsContext, String url, Priority priority,
                              boolean shouldBypassCache, boolean includeWithoutLocation,
                              Listener<List<Event>> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(true);

        this.eventsContext = eventsContext;
        this.priority = priority;
        this.includeWithoutLocation = includeWithoutLocation;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<List<Event>> parseNetworkResponse(NetworkResponse response) {
        try {
            List<Event> events = new ArrayList<>();

            String jsonString = new String(response.data, "UTF-8");
            JSONObject eventsJson = new JSONObject(jsonString);
            Iterator<String> keys = eventsJson.keys();
            while (keys.hasNext()) {
                try {
                    Event event = Event.fromJSON(eventsContext.city, eventsJson.getJSONObject(keys.next()));
                    if (includeWithoutLocation || event.location != null) {
                        events.add(event);
                    }
                } catch (JSONException | ParseException e) {
                    Crashlytics.getInstance().core.logException(e);
                }
            }
            EventCollectionRequest.filterOldEvents(events);

            // Sort the event list to user.
            Collections.sort(events, new EventComparator(eventsContext.location));

            return Response.success(events, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }
}
