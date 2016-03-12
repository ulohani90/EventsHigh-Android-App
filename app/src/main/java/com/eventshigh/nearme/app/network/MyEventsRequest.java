package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.network.EventCollectionRequest.EventsCollection;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest.SocialInvite;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Supports fetching of MyEvents for user -- it includes upcoming events which are marked as
 * favourite and few events for all follow.
 */
public class MyEventsRequest extends AsyncTask<Void, Void, List<TopicEvents>> {
    private static final String LOG_TAG = MyEventsRequest.class.getSimpleName();

    private static String FAVOURITES_NAME = "favourites";
    private static String INVITATIONS_NAME = "invitations";

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

    public static boolean isSpecialTag(String name) {
        name = name.toLowerCase();
        return name.equals(FAVOURITES_NAME) || name.equals(INVITATIONS_NAME);
    }

    public static void submit(Context context, EventsContext eventsContext, Priority priority,
           Object tag, boolean shouldBypassCache, boolean includeWithoutLocation,
           Listener<List<TopicEvents>> listener, ErrorListener errorListener) {
        new MyEventsRequest(context, eventsContext, priority, tag, shouldBypassCache,
                includeWithoutLocation, listener, errorListener).execute();
    }

    private final Context context;
    private final EventsContext eventsContext;
    private final Priority priority;
    private final boolean shouldBypassCache;
    private final boolean includeWithoutLocation;
    private final Listener<List<TopicEvents>> listener;
    private final ErrorListener errorListener;
    private final Object tag;
    private boolean isRequestCancelled = false;

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

    public @Nullable TopicEvents getNonEmptyInterest() {
        List<TopicEvents> topicEventsList = doInBackground();
        for (TopicEvents events : topicEventsList) {
            if (!isSpecialTag(events.topicName) && events.numEvents > 0) {
                return events;
            }
        }

        return null;
     }

    @Override
    protected List<TopicEvents> doInBackground(Void... params) {
        if (eventsContext.city == null) {
            Log.w(LOG_TAG, "No City for: " + eventsContext.toString());
            return null;
        }

        List<TopicEvents> result = new ArrayList<>();
        RequestFuture<Map<String, SocialInvite>> socialInvites = RequestFuture.newFuture();
        SocialInvitationsRequest.submit(context, priority, tag, shouldBypassCache,
                socialInvites, socialInvites);

        // Interest based requests.
        List<String> interests = new Account(context).getFollowingInterests();
        Map<String, RequestFuture<EventsCollection>> interestsEvents = Utils.getMap();
        for (String interest : interests) {
            RequestFuture<EventsCollection> eventsFuture = RequestFuture.newFuture();
            EventCollectionRequest.submit(context, new EventsContext(eventsContext.location, interest),
                priority, tag, shouldBypassCache, includeWithoutLocation, eventsFuture, eventsFuture);
            interestsEvents.put(interest, eventsFuture);
        }

        // Favourites event requests.
        EventsMarkerManager markerManager = EventsMarkerManager.getInstance(context);
        markerManager.waitForLoading();
        RequestFuture<List<Event>> favEvents = RequestFuture.newFuture();
        MultiEventsRequest.submit(context, eventsContext, markerManager.getFavouritedEvents(),
                priority, tag, shouldBypassCache, includeWithoutLocation, favEvents, favEvents);

        // Look at invites and send the request for sent invitations.
        List<String> eventIds = new ArrayList<>();
        try {
            eventIds.addAll(socialInvites.get().keySet());
        } catch (InterruptedException | ExecutionException e) {
            Crashlytics.getInstance().core.logException(e);
        }

        RequestFuture<List<Event>> invitedEvents = RequestFuture.newFuture();
        MultiEventsRequest.submit(context, eventsContext, eventIds, priority, tag,
                shouldBypassCache, true, invitedEvents, invitedEvents);

        // Build Result.
        try {
            addEventsToResults(result, INVITATIONS_NAME, invitedEvents);
            addEventsToResults(result, FAVOURITES_NAME, favEvents);
            for (Entry<String, RequestFuture<EventsCollection>> interestEvents : interestsEvents.entrySet()) {
                addCollectionToResults(result, interestEvents.getKey(), interestEvents.getValue());
            }
            return result;
        } catch (RequestCancelledException e) {
            isRequestCancelled = true;
            return null;
        }
    }

    protected void onPostExecute(@Nullable List<TopicEvents> result) {
        if (isRequestCancelled) {
            return;
        }

        if (result != null) {
            listener.onResponse(result, false);
        } else {
            errorListener.onErrorResponse(new VolleyError("no response"));
        }
    }

    private static void addToResults(List<TopicEvents> result, String name, List<Event> events) {
        if (! events.isEmpty()) {
            result.add(new TopicEvents(name, events));
        }
    }

    private static void addEventsToResults(List<TopicEvents> result, String name,
            RequestFuture<List<Event>> eventsFuture) throws RequestCancelledException {
        try {
            addToResults(result, name, eventsFuture.get(10, TimeUnit.SECONDS));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (eventsFuture.isCancelled()) {
                throw new RequestCancelledException();
            }
            Crashlytics.getInstance().core.logException(e);
        }
    }

    private static void addCollectionToResults(List<TopicEvents> result, String name,
            RequestFuture<EventsCollection> eventsFuture) throws RequestCancelledException {
        try {
            addToResults(result, name, eventsFuture.get(10, TimeUnit.SECONDS).events);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            if (eventsFuture.isCancelled()) {
                throw new RequestCancelledException();
            }
            Crashlytics.getInstance().core.logException(e);
        }
    }

    private static class RequestCancelledException extends Exception {

    }
}
