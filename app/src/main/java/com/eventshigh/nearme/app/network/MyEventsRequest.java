package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Supports fetching of MyEvents for user -- it includes upcoming events which are marked as
 * favourite and few events for all follow.
 */

public class MyEventsRequest extends AsyncTask<Void, Void, MyEventsRequest.MeEventFavouriteObject> {
    private static final String LOG_TAG = MyEventsRequest.class.getSimpleName();

    public static String FAVOURITES_NAME = "my events";
    private static String INVITATIONS_NAME = "invitations";
    public static String MOVIES_NAME = "my movies";
    public static String MY_UPCOMING_FAVOURITES_NAME = "my upcoming favourites";
    public static String MY_PAST_FAVOURITES_NAME = "my past favourites";


    public static class MeEventFavouriteObject implements Parcelable {
        public final List<TopicEvents> topicEvents;

        public MeEventFavouriteObject(List<TopicEvents> topicEvents) {
            this.topicEvents = topicEvents;

        }

        public MeEventFavouriteObject(Parcel in) {
            this.topicEvents = new ArrayList<>();
            in.readTypedList(this.topicEvents, TopicEvents.CREATOR);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeTypedList(topicEvents);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Parcelable.Creator<MeEventFavouriteObject> CREATOR =
                new Parcelable.Creator<MeEventFavouriteObject>() {
                    public MeEventFavouriteObject createFromParcel(Parcel in) {
                        return new MeEventFavouriteObject(in);
                    }

                    public MeEventFavouriteObject[] newArray(int size) {
                        return new MeEventFavouriteObject[size];
                    }
                };


    }

    public static class TopicEvents implements Parcelable {
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

        public TopicEvents(Parcel in) {
            this.topicName = in.readString();
            events = new ArrayList<>();
            in.readTypedList(events, Event.CREATOR);
            numEvents = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(topicName);
            dest.writeTypedList(events);
            dest.writeFloat(numEvents);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Parcelable.Creator<TopicEvents> CREATOR =
                new Parcelable.Creator<TopicEvents>() {
                    public TopicEvents createFromParcel(Parcel in) {
                        return new TopicEvents(in);
                    }

                    public TopicEvents[] newArray(int size) {
                        return new TopicEvents[size];
                    }
                };

    }

    public static boolean isSpecialTag(String name) {
        name = name.toLowerCase();
        return name.equals(FAVOURITES_NAME) || name.equals(INVITATIONS_NAME) || name.equals(MOVIES_NAME) || name.equalsIgnoreCase(MY_UPCOMING_FAVOURITES_NAME) || name.equalsIgnoreCase(MY_PAST_FAVOURITES_NAME);
    }

    public static void submit(Context context, EventsContext eventsContext, Priority priority,
                              Object tag, boolean shouldBypassCache, boolean includeWithoutLocation,
                              Listener<MeEventFavouriteObject> listener, ErrorListener errorListener) {
        new MyEventsRequest(context, eventsContext, priority, tag, shouldBypassCache,
                includeWithoutLocation, listener, errorListener).execute();
    }

    private final Context context;
    private final EventsContext eventsContext;
    private final Priority priority;
    private final boolean shouldBypassCache;
    private final boolean includeWithoutLocation;
    private final Listener<MeEventFavouriteObject> listener;
    private final ErrorListener errorListener;
    private final Object tag;
    private boolean isRequestCancelled = false;

    public MyEventsRequest(Context context, EventsContext eventsContext, Priority priority,
                           Object tag, boolean shouldBypassCache, boolean includeWithoutLocation,
                           Listener<MeEventFavouriteObject> listener, ErrorListener errorListener) {
        this.context = context;
        this.eventsContext = eventsContext;
        this.priority = priority;
        this.shouldBypassCache = shouldBypassCache;
        this.includeWithoutLocation = includeWithoutLocation;
        this.listener = listener;
        this.errorListener = errorListener;
        this.tag = tag;
    }

    @Override
    protected MeEventFavouriteObject doInBackground(Void... params) {
        if (eventsContext.city == null) {
            Log.w(LOG_TAG, "No City for: " + eventsContext.toString());
            return null;
        }


        List<TopicEvents> events = new ArrayList<>();


        // Favourites event requests.
        EventsMarkerManager markerManager = EventsMarkerManager.getInstance(context);
        markerManager.waitForLoading();
        RequestFuture<List<Event>> favEvents = RequestFuture.newFuture();
        MultiEventsRequest.submit(context, eventsContext, markerManager.getFavouritedEvents(),
                priority, tag, shouldBypassCache, includeWithoutLocation, true, favEvents, favEvents);

        // Look at invites and send the request for sent invitations.


        // Build Result.
        try {
            // addEventsToResults(events, INVITATIONS_NAME, invitedEvents);
            addEventsToResults(events, FAVOURITES_NAME, favEvents);


            return new MeEventFavouriteObject(events);
        } catch (RequestCancelledException e) {
            isRequestCancelled = true;
            return null;
        }
    }

    protected void onPostExecute(@Nullable MeEventFavouriteObject result) {
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
        if (!events.isEmpty()) {
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
