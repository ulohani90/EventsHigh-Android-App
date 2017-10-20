package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
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
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.network.EventCollectionRequest.EventsCollection;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Volley Request to fetch Events collections.
 */
public class EventCollectionRequest extends JsonRequest<EventsCollection> {

    public static class EventsCollection {
        public final List<Event> events;
        public final int numFollowers;

        public EventsCollection(List<Event> events, int numFollowers) {
            this.events = events;
            this.numFollowers = numFollowers;
        }
    }

    /**
     * Helper method to submit a volley request to fetch Events information.
     *
     * @param context       an application eventsContext to initiate the volley.
     * @param eventsContext EventsContext representing the request.
     * @param listener      callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, EventsContext eventsContext, Priority priority,
                              Object tag, boolean shouldBypassCache, boolean includeWithoutLocation,
                              Listener<EventsCollection> listener, ErrorListener errorListener) {
        if (eventsContext.city == null) {
            errorListener.onErrorResponse(new VolleyError("No City for: " + eventsContext.toString()));
            return;
        }

        String url;
        try {
            url = EventsHighEndpoints.getApiEndpoint(eventsContext);
            System.out.println("Api hit: " + url);
        } catch (IllegalArgumentException e) {
            errorListener.onErrorResponse(new VolleyError("Invalid Query", e));
            return;
        }

        EventCollectionRequest request = new EventCollectionRequest(
                context, url, eventsContext, priority, shouldBypassCache, includeWithoutLocation,
                listener, errorListener, null);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }

    public static void submit(Context context, EventsContext eventsContext, Priority priority,
                              Object tag, String dateString, boolean shouldBypassCache, boolean includeWithoutLocation,
                              Listener<EventsCollection> listener, ErrorListener errorListener, OnDataProcessComplete pListener) {
        if (eventsContext.city == null) {
            errorListener.onErrorResponse(new VolleyError("No City for: " + eventsContext.toString()));
            return;
        }

        String url;
        try {
            url = EventsHighEndpoints.getApiEndpointForDate(eventsContext, dateString);
        } catch (IllegalArgumentException e) {
            errorListener.onErrorResponse(new VolleyError("Invalid Query", e));
            return;
        }

        EventCollectionRequest request = new EventCollectionRequest(
                context, url, eventsContext, priority, shouldBypassCache, includeWithoutLocation,
                listener, errorListener, pListener);
        request.setRetryPolicy(new DefaultRetryPolicy(60_000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final Context context;
    private final EventsContext eventsContext;
    private final Priority priority;
    private final boolean includeWithoutLocation;

    OnDataProcessComplete mListener;

    public EventCollectionRequest(Context context, String url, EventsContext eventsContext,
                                  Priority priority, boolean shouldBypassCache, boolean includeWithoutLocation,
                                  Listener<EventsCollection> listener, ErrorListener errorListener, OnDataProcessComplete pListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(true);
        this.context = context;
        this.eventsContext = eventsContext;
        this.priority = priority;
        this.includeWithoutLocation = includeWithoutLocation;
        this.mListener = pListener;

    }


    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<EventsCollection> parseNetworkResponse(NetworkResponse response) {
        try {
            // Parse the response.
            EventsCollection eventsCollection = parseEventsFromNetworkResponse(response, context,
                    eventsContext, includeWithoutLocation, mListener);
            return Response.success(eventsCollection, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            Crashlytics.getInstance().core.logException(e);
            return Response.error(new ParseError(e));
        }
    }

    public static EventsCollection parseEventsFromNetworkResponse(
            NetworkResponse response, final Context context, final EventsContext eventsContext,
            boolean includeWithoutLocation, final OnDataProcessComplete listener) throws UnsupportedEncodingException, JSONException {
        ReportTimingTask.report(context, "events", response.networkTimeMs);

        String jsonString = new String(response.data, "UTF-8");
        final JSONObject eventsJson = new JSONObject(jsonString);

        if (listener != null) {
            Event.parseUpcomingEvents(eventsJson, includeWithoutLocation, new Event.OnPartialDataLoadingComplete() {
                @Override
                public void onPartialLoadingComplete(List<Event> events) {
                    filterOldEvents(context, events);
                    // Sort the event list to user.
                    Collections.sort(events, new EventComparator(eventsContext.location));
                    listener.onDataProcessComplete(new EventsCollection(events, eventsJson.optInt("num_followers")), false);


                }

                @Override
                public void onFullDataLoadingComplete(List<Event> events) {
                    filterOldEvents(context, events);
                    // Sort the event list to user.
                    Collections.sort(events, new EventComparator(eventsContext.location));
                    listener.onDataProcessComplete(new EventsCollection(events, eventsJson.optInt("num_followers")), true);
                }
            });
        } else {
            List<Event> events = Event.parseUpcomingEvents(eventsJson, includeWithoutLocation, null);
            filterOldEvents(context, events);

            // Sort the event list to user.
            Collections.sort(events, new EventComparator(eventsContext.location));

            return new EventsCollection(events, eventsJson.optInt("num_followers"));

        }
        return null;

    }


    // Filter out the events which has started more than three hours back.
    public static void filterOldEvents(Context context, List<Event> events) {
        long threeHoursBack = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(3);
        long aDayBack = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);

        for (Iterator<Event> it = events.iterator(); it.hasNext(); ) {
            Event event = it.next();
            boolean isPastEvent;
            if (event.eventTimings == null || event.eventTimings.size() == 0) {
                isPastEvent = false;
            } else {
                isPastEvent = true;
            }
            for (long eventTiming : event.eventTimings) {
                if (eventTiming < aDayBack) {
                    continue;
                }

                EventTime eventTime = DateTimeUtils.dateToEventTime(new Date(eventTiming),
                        TimeZone.getTimeZone(event.timezone));
                if (eventTime.time != null && eventTiming < threeHoursBack) {
                    continue;
                }

                isPastEvent = false;
                break;
            }

            if (isPastEvent) {
                it.remove();
                EventsMarkerManager.Editor eventsMarkerEditor =
                        EventsMarkerManager.getInstance(context).getEditor();
                eventsMarkerEditor.recordEventMark(event, null, false);
                eventsMarkerEditor.close();

            }
        }
    }


    public interface OnDataProcessComplete {
        void onDataProcessComplete(EventsCollection collection, boolean isLoadingFinished);
    }


}
