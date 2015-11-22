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
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
import com.eventshigh.nearme.app.task.ReportTimingTask;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Calls the api to get events for a day by categories.
 */
public class DateCategoryRequest extends JsonRequest<List<TopicEvents>>  {
    /**
     * Helper method to submit a volley request to fetch Events information.
     *
     * @param context an application context to initiate the volley.
     * @param eventsContext EventsContext representing the request.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, EventsContext eventsContext,
                              Priority priority, Object tag, boolean shouldBypassCache,
                              Listener<List<TopicEvents>> listener, ErrorListener errorListener) {
        if (eventsContext.city == null) {
            errorListener.onErrorResponse(new VolleyError("No City for: " + eventsContext.toString()));
            return;
        }

        String url = EventsHighEndpoints.getDateCategoryEndpoint(eventsContext.city, eventsContext.dateFilter);
        DateCategoryRequest request = new DateCategoryRequest(
                context, url, shouldBypassCache, priority, listener, errorListener);
        request.setTag(tag);
        VolleyHelper.addToRequestQueue(context, request);
    }

    private final Context context;
    private final Priority priority;

    public DateCategoryRequest(Context context, String url,
                               boolean shouldBypassCache, Priority priority,
                               Listener<List<TopicEvents>> listener, ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(true);

        this.context = context;
        this.priority = priority;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<List<TopicEvents>> parseNetworkResponse(NetworkResponse response) {
        try {
            // Parse the response.
            ReportTimingTask.report(context, "date-category-events", response.networkTimeMs);
            String jsonString = new String(response.data, "UTF-8");
            JSONObject eventsJson = new JSONObject(jsonString);
            JSONObject dateEventsJson = eventsJson.getJSONObject(eventsJson.keys().next());

            List<TopicEvents> topicEvents = new ArrayList<>();
            Iterator<String> categories = dateEventsJson.keys();
            while (categories.hasNext()) {
                String category = categories.next();
                JSONObject categoryEvents = dateEventsJson.getJSONObject(category);
                int numEvents = categoryEvents.getInt("num_events");
                JSONArray upcomingEvents = categoryEvents.getJSONArray("events");
                List<Event> events = Event.fromJSON(upcomingEvents);

                category = category.equals("upcoming") ? IntentUtils.QUERY_ALL : category;
                if (!events.isEmpty()) {
                    topicEvents.add(new TopicEvents(category, events, numEvents));
                }
            }

            return Response.success(topicEvents, HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            return Response.error(new ParseError(e));
        }
    }
}
