package com.eventshigh.nearme.app.network;

import android.content.Context;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.network.EventCollectionRequest.EventsCollection;
import com.eventshigh.nearme.app.user.Account;

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

    private final Context context;
    private final EventsContext eventsContext;
    private final Priority priority;
    private final boolean shouldBypassCache;
    private final boolean includeWithoutLocation;
    private final Listener<List<TopicEvents>> listener;
    private final ErrorListener errorListener;
    private final Object tag;

    private int numPendingRequests;
    private final List<TopicEvents> result = new ArrayList<>();

    public MyEventsRequest(Context context, EventsContext eventsContext, Priority priority,
                           Object tag, boolean shouldBypassCache, boolean includeWithoutLocation,
                           Listener<List<TopicEvents>> listener, ErrorListener errorListener) {
        this.context = context;
        this.eventsContext = eventsContext;
        this.priority = priority;
        this.shouldBypassCache = shouldBypassCache;
        this.includeWithoutLocation = includeWithoutLocation;
        this.listener = listener;
        this.errorListener = errorListener;
        this.tag = tag;
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
        EventsMarkerManager markerManager = EventsMarkerManager.getInstance(context);
        markerManager.waitForLoading();
        MultiEventsRequest.submit(context, eventsContext, markerManager.getFavouritedEvents(),
                priority, tag, shouldBypassCache, includeWithoutLocation,
                new FavouritedEventsListener(), errorListener);

        // Interest based requests.
        for (String interest : interests) {
            EventCollectionRequest.submit(context, new EventsContext(eventsContext.location, interest),
                    priority, tag, shouldBypassCache, includeWithoutLocation,
                    new EventsListener(interest), errorListener);
        }
    }

    private synchronized void reportResult() {
        numPendingRequests --;

        if (numPendingRequests == 0) {
            listener.onResponse(result, false);
        }
    }

    private class FavouritedEventsListener extends BaseEventsListener implements Listener<List<Event>> {
        public FavouritedEventsListener() {
            super(FAVOURITES_NAME);
        }

        @Override
        public void onResponse(List<Event> events, boolean intermediate) {
            addToResult(events, 0, intermediate);
        }
    }

    private class EventsListener extends BaseEventsListener implements Listener<EventsCollection> {

        public EventsListener(String title) {
            super(title);
        }

        @Override
        public void onResponse(EventsCollection eventsCollection, boolean intermediate) {
            appendToResult(eventsCollection.events, intermediate);
        }
    }

    private class BaseEventsListener {
        private final String title;

        public BaseEventsListener(String title) {
            this.title = title;
        }

        public void appendToResult(List<Event> events, boolean intermediate) {
            addToResult(events, -1, intermediate);
        }

        public void addToResult(List<Event> events, int index, boolean intermediate) {
            if (intermediate) {
                return;
            }
            if (!events.isEmpty()) {
                synchronized (result) {
                    if (index >= 0) {
                        result.add(index, new TopicEvents(title, events));
                    } else {
                        result.add(new TopicEvents(title, events));
                    }
                }
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
