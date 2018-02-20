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
import com.eventshigh.nearme.app.data.BrowseFilterAttributes;
import com.eventshigh.nearme.app.data.BrowseFilterObject;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.stream.ZoneLocalityMapObject;
import com.eventshigh.nearme.app.network.EventCollectionRequest.EventsCollection;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.facebook.share.model.GameRequestContent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Volley Request to fetch Events collections.
 */
public class EventCollectionRequest extends JsonRequest<EventsCollection> {

    public static class EventsCollection {
        public final List<Event> events;

        public final int numFollowers;

        public final BrowseFilterObject filters;

        public final List<String> zones;

        public final ArrayList<ZoneLocalityMapObject> localities;

        public final List<String> dateFilters;

        public final boolean showCarousel;


        public EventsCollection(List<Event> events, int numFollowers, BrowseFilterObject filters, List<String> zones, ArrayList<ZoneLocalityMapObject> localities, List<String> dateFilters, boolean showCarousel) {
            this.events = events;
            this.numFollowers = numFollowers;
            this.filters = filters;
            this.zones = zones;
            this.localities = localities;
            this.dateFilters = dateFilters;
            this.showCarousel = showCarousel;
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
        setShouldCache(shouldBypassCache);
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


        final BrowseFilterObject filters = BrowseFilterObject.parseFromJson(eventsJson);

        final List<String> zones = readFacetsArray(eventsJson.optJSONObject("facets"), "zones");

        final ArrayList<ZoneLocalityMapObject> localities = readLocalitiesFacets(eventsJson.optJSONObject("facets"), "localities");

        final List<String> dateFilters = readFacetsArray(eventsJson.optJSONObject("facets"), "dates");

        final boolean showCarousel = eventsJson.optBoolean("editor_picks_enabled");

        if (listener != null) {
            Event.parseUpcomingEvents(eventsJson, filters, includeWithoutLocation, new Event.OnPartialDataLoadingComplete() {
                @Override
                public void onPartialLoadingComplete(List<Event> events) {
                    filterOldEvents(context, events);
                    // Sort the event list to user.
                    Collections.sort(events, new EventComparator(eventsContext.location));
                    listener.onDataProcessComplete(new EventsCollection(events, eventsJson.optInt("num_followers"), filters, zones, localities, dateFilters, showCarousel), false);

                }

                @Override
                public void onFullDataLoadingComplete(List<Event> events) {
                    filterOldEvents(context, events);
                    // Sort the event list to user.
                    Collections.sort(events, new EventComparator(eventsContext.location));
                    listener.onDataProcessComplete(new EventsCollection(events, eventsJson.optInt("num_followers"), filters, zones, localities, dateFilters, showCarousel), true);
                }
            });
        } else {
            List<Event> events = Event.parseUpcomingEvents(eventsJson, filters, includeWithoutLocation, null);
            filterOldEvents(context, events);
            // Sort the event list to user.
            //  Collections.sort(events, new EventComparator(eventsContext.location));

            return new EventsCollection(events, eventsJson.optInt("num_followers"), filters, zones, localities, dateFilters, showCarousel);
        }
        return null;

    }

    public static List<String> readFacetsArray(JSONObject jsonObj, String key) {
        List<String> names = new ArrayList<>();
        if (jsonObj != null) {
            JSONArray jsonArray = jsonObj.optJSONArray(key);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    String venueName = jsonArray.optJSONObject(i).optString("name");
                    names.add(venueName);
                }
            }
        }
        return names;
    }

    public static ArrayList<ZoneLocalityMapObject> readLocalitiesFacets(JSONObject jsonObj, String key) {
        ArrayList<ZoneLocalityMapObject> objs = new ArrayList<>();
        HashMap<String, ArrayList<String>> zonesLocalityMap = new HashMap<>();
        if (jsonObj != null) {
            JSONArray jsonArray = jsonObj.optJSONArray(key);
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.optJSONObject(i);

                    if (jsonObject.has("zones") && jsonObject.has("name")) {
                        String zoneName = jsonObject.optString("zones");
                        if (zoneName.equalsIgnoreCase("unknown")) {
                            continue;
                        }
                        String localityName = jsonObject.optString("name");
                        if (localityName.length() == 0) {
                            continue;
                        }
                        if (zonesLocalityMap.containsKey(zoneName)) {
                            if (!localityName.equalsIgnoreCase(zoneName))
                                ((ArrayList<String>) zonesLocalityMap.get(zoneName)).add(localityName);
                        } else {
                            ArrayList<String> localities = new ArrayList<>();
                            if (!localityName.equalsIgnoreCase(zoneName))
                                localities.add(localityName);
                            zonesLocalityMap.put(zoneName, localities);
                        }
                    }
                }
            }
        }
        Iterator it = zonesLocalityMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            objs.add(new ZoneLocalityMapObject((String) pair.getKey(), (ArrayList<String>) pair.getValue()));
        }
        return objs;
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
                        TimeZone.getTimeZone(event.timezone != null ? event.timezone : Event.DEFAULT_TIME_ZONE));
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
