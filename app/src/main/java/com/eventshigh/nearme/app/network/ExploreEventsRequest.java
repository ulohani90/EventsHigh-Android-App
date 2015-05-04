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
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.TrendingTopic;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A request to fetch the explore events.
 */
public class ExploreEventsRequest extends JsonRequest<MyEvents>  {
    /**
     * Helper method to submit a volley request to fetch Events information.
     *
     * @param context an application eventsContext to initiate the volley.
     * @param eventsContext EventsContext representing the request.
     * @param listener callback on success.
     * @param errorListener callback on failures.
     */
    public static void submit(Context context, EventsContext eventsContext,
                              Priority priority, boolean shouldBypassCache,
                              Listener<MyEvents> listener, ErrorListener errorListener) {
        if (eventsContext.city == null) {
            errorListener.onErrorResponse(new VolleyError("No City for: " + eventsContext.toString()));
            return;
        }

        String url = EventsHighEndpoints.getExploreEventsEndpoint(eventsContext.city);
        ExploreEventsRequest request = new ExploreEventsRequest(
                context, url, eventsContext, priority, shouldBypassCache, listener, errorListener);
        request.setTag(context);
        VolleyHelper.addToRequestQueue(context, request);
    }

    protected final Context context;
    protected final EventsContext eventsContext;
    private final Priority priority;
    protected final EventsMarkerManager eventsMarkerManager;

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
    public ExploreEventsRequest(Context context, String url, EventsContext eventsContext,
                                Priority priority, boolean shouldBypassCache,
                                Listener<MyEvents> listener,  ErrorListener errorListener) {
        super(Method.GET, url, null, listener, errorListener);
        setShouldBypassCache(shouldBypassCache);
        setShouldAllowStaleResponse(true);

        this.context = context;
        this.eventsContext = eventsContext;
        this.priority = priority;
        this.eventsMarkerManager = EventsMarkerManager.getInstance(context);
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    @Override
    protected Response<MyEvents> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data, "UTF-8");
            JSONObject exploreEventsJson = new JSONObject(jsonString);

            // Parse the topic events.
            List<TopicEvents> topicEvents = new ArrayList<>();
            JSONObject countsJSON = exploreEventsJson.getJSONObject("counts");
            JSONArray exploreTags = exploreEventsJson.getJSONArray("explore");
            for (int i = 0; i < exploreTags.length(); i++) {
                // Parse events.
                JSONObject tagEventsJSON = exploreTags.getJSONObject(i);
                String tag = tagEventsJSON.keys().next();
                List<Event> events = Event.fromJSON(eventsContext.city,
                        tagEventsJSON.getJSONArray(tag), true);
                BaseEventListRequest.filterOldEvents(events, false);

                // Sort the event list to user.
                eventsMarkerManager.waitForLoading();
                Collections.sort(events, new EventComparator(eventsContext.location, eventsMarkerManager));

                int count = events.size();
                if (countsJSON != null) {
                    count = countsJSON.optInt(tag.toLowerCase(), events.size());
                }
                topicEvents.add(new TopicEvents(tag, events, count));
            }

            // Parse Trending topics.
            List<TrendingTopic> trendingTopics = new ArrayList<>();
            JSONArray trendingTopicsJSON = exploreEventsJson.optJSONArray("topics");
            if (eventsContext.city == City.BANGALORE && trendingTopicsJSON != null) {
                for (int i = 0; i < trendingTopicsJSON.length(); i++) {
                    TrendingTopic trendingTopic = TrendingTopic.parse(trendingTopicsJSON.optJSONObject(i));
                    if (trendingTopic != null) {
                        trendingTopics.add(trendingTopic);
                    }
                }
            }

            return Response.success(new MyEvents(topicEvents, trendingTopics),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException e) {
            VolleyHelper.getInstance(context).invalidateCache(this);
            return Response.error(new ParseError(e));
        }
    }
}
