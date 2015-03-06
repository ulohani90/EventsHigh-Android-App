package com.eventshigh.nearme.app.network;

import com.android.volley.NetworkResponse;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.JsonRequest;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.task.ReportTimingTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Base class for request to fetch event list.
 */
public abstract class BaseEventListRequest extends JsonRequest<List<Event>>  {
    protected final BaseActivity activity;
    protected final EventsContext eventsContext;
    private final Priority priority;
    protected final EventsMarkerManager eventsMarkerManager;
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
    public BaseEventListRequest(BaseActivity activity, String url, EventsContext eventsContext,
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

    protected List<Event> parseEventsFromNetworkResponse(NetworkResponse response)
            throws UnsupportedEncodingException, JSONException {
        new ReportTimingTask(activity, "events").execute(response.networkTimeMs);

        String jsonString = new String(response.data, "UTF-8");
        JSONObject eventsJson = new JSONObject(jsonString);
        List<Event> events = Event.parseUpcomingEvents(eventsContext.city, eventsJson,
                includeWithoutLocation);

        // Filter out the events which are dismissed.
        eventsMarkerManager.waitForLoading();
        eventsMarkerManager.removeDismissed(events);

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
        if (eventsContext.location != null) {
            Collections.sort(events, new EventComparator(eventsContext.location, eventsMarkerManager));
        }

        return events;
    }
}
