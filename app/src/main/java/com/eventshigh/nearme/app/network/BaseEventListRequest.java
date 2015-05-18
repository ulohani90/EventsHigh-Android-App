package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.JsonRequest;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Base class for request to fetch event list.
 */
public abstract class BaseEventListRequest extends JsonRequest<List<Event>>  {
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
                                Listener<List<Event>> listener, ErrorListener errorListener) {
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

    protected List<Event> parseEventsFromNetworkResponse(NetworkResponse response)
            throws UnsupportedEncodingException, JSONException {
        return parseEventsFromNetworkResponse(response, context, eventsContext, eventsMarkerManager,
                includeWithoutLocation);
    }

    public static List<Event> parseEventsFromNetworkResponse(NetworkResponse response,
            Context context, EventsContext eventsContext, EventsMarkerManager eventsMarkerManager,
            boolean includeWithoutLocation) throws UnsupportedEncodingException, JSONException {
        ReportTimingTask.report(context, "events", response.networkTimeMs);

        String jsonString = new String(response.data, "UTF-8");
        JSONObject eventsJson = new JSONObject(jsonString);
        List<Event> events = Event.parseUpcomingEvents(eventsContext.city, eventsJson,
                includeWithoutLocation);
        filterOldEvents(events, !eventsContext.dateFilter.isEmpty());

        // Sort the event list to user.
        eventsMarkerManager.waitForLoading();
        Collections.sort(events, new EventComparator(eventsContext.location, eventsMarkerManager));

        return events;
    }

    // Filter out the events which has started more than three hours back.
    public static void filterOldEvents(List<Event> events, boolean isDateQuery) {
        long threeHoursBack = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3);
        for (Iterator<Event> it =  events.iterator(); it.hasNext(); ) {
            Event event = it.next();
            EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
            if (eventTime != null && eventTime.time != null && event.eventTimings[0] < threeHoursBack) {
                // The event has started more than two hours back. We filter it out either if
                // it has not future occurrences or if its a date query.
                if (event.eventTimings.length == 1 || isDateQuery) {
                    it.remove();
                }
            }
        }
    }
}
