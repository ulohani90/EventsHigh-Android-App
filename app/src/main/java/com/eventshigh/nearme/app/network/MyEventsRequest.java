package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.TrendingTopic;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import java.util.ArrayList;
import java.util.List;

/**
 * Supports fetching of MyEvents for user -- it includes upcoming events which are marked as
 * favourite and few events for all follow.
 */
public class MyEventsRequest {
    public static String FAVOURITES_NAME = "favourites";

    public static class TopicEvents {
        public final String topicName;
        public final List<Event> events;
        public final int numEvents;

        public TopicEvents(String topicName, List<Event> events) {
            this(topicName, events, events.size());
        }

        public TopicEvents(String topicName, List<Event> events, int numEvents) {
            this.topicName = topicName;
            this.events = events;
            this.numEvents = numEvents;
        }
    }

    public static class MyEvents {
        public final List<TopicEvents> topicEvents;
        public final List<TrendingTopic> trendingTopics;

        public MyEvents(List<TopicEvents> topicEvents, List<TrendingTopic> trendingTopics) {
            this.topicEvents = topicEvents;
            this.trendingTopics = trendingTopics;
        }

        public boolean isEmpty() {
            return topicEvents.isEmpty();
        }
    }

    private final Context context;
    private final EventsContext eventsContext;
    private final Priority priority;
    private final boolean shouldBypassCache;
    private final boolean includeWithoutLocation;
    private final Listener<MyEvents> listener;
    private final ErrorListener errorListener;

    private int numPendingRequests;
    private final List<TopicEvents> result = new ArrayList<>();

    public MyEventsRequest(Context context, EventsContext eventsContext, Priority priority,
                           boolean shouldBypassCache, boolean includeWithoutLocation,
                           Listener<MyEvents> listener, ErrorListener errorListener) {
        this.context = context;
        this.eventsContext = eventsContext;
        this.priority = priority;
        this.shouldBypassCache = shouldBypassCache;
        this.includeWithoutLocation = includeWithoutLocation;
        this.listener = listener;
        this.errorListener = errorListener;
    }

    public void execute() {
        if (eventsContext.city == null) {
            errorListener.onErrorResponse(new VolleyError("No City for: " + eventsContext.toString()));
            return;
        }

        List<String> interests = new Account(context).getFollowingInterests();
        numPendingRequests = interests.size() + 1;
        InternalErrorListener errorListener = new InternalErrorListener();

        // Favourites event requests.
        EventCollectionRequest.submit(context,
                new EventsContext(eventsContext.location, EventsHighEndpoints.QUERY_MY_EVENT),
                priority, shouldBypassCache, includeWithoutLocation, new FavouritedEventsListener(),
                errorListener);

        // Interest based requests.
        for (String interest : interests) {
            EventCollectionRequest.submit(context, new EventsContext(eventsContext.location, interest),
                    priority, shouldBypassCache, includeWithoutLocation, new EventsListener(interest),
                    errorListener);
        }
    }

    private synchronized void reportResult() {
        numPendingRequests --;

        if (numPendingRequests == 0) {
            listener.onResponse(new MyEvents(result, new ArrayList<TrendingTopic>()), false);
        }
    }

    private class FavouritedEventsListener extends EventsListener {
        public FavouritedEventsListener() {
            super(FAVOURITES_NAME);
        }

        public void addToResult(List<Event> events) {
            result.add(0, new TopicEvents(FAVOURITES_NAME, events));
        }
    }

    private class EventsListener implements Listener<List<Event>> {
        private final String title;

        public EventsListener(String title) {
            this.title = title;
        }

        public void addToResult(List<Event> events) {
            synchronized (result) {
                result.add(new TopicEvents(title, events));
            }
        }

        @Override
        public void onResponse(List<Event> events, boolean intermediate) {
            if (intermediate) {
                return;
            }

            if (!events.isEmpty()) {
                addToResult(events);
            }
            reportResult();
        }
    }

    private class InternalErrorListener implements ErrorListener {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            reportResult();
        }
    }
}
