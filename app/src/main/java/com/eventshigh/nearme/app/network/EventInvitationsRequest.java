package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.EventInvitationsRequest.EventInvitation;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest.SocialInvite;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class EventInvitationsRequest extends AsyncTask<Void, Void, List<EventInvitation>> {
    public static class EventInvitation {
        public final Event event;

        public EventInvitation(Event event) {
            this.event = event;
        }
    }

    public static void submit(Context context, EventsContext eventsContext, Priority priority,
            Object tag, boolean shouldBypassCache, Listener<List<EventInvitation>> listener,
            ErrorListener errorListener) {
        new EventInvitationsRequest(context, eventsContext, priority, tag, shouldBypassCache,
                listener, errorListener).execute();
    }

    private final Context context;
    private final EventsContext eventsContext;
    private final Priority priority;
    private final Object tag;
    private final boolean shouldBypassCache;
    private final Listener<List<EventInvitation>> listener;
    private final ErrorListener errorListener;

    public EventInvitationsRequest(Context context, EventsContext eventsContext, Priority priority,
           Object tag, boolean shouldBypassCache, Listener<List<EventInvitation>> listener,
           ErrorListener errorListener) {
        this.context = context;
        this.eventsContext = eventsContext;
        this.priority = priority;
        this.tag = tag;
        this.shouldBypassCache = shouldBypassCache;
        this.listener = listener;
        this.errorListener = errorListener;
    }

    @Override
    protected List<EventInvitation> doInBackground(Void... params) {
        RequestFuture<Map<String, SocialInvite>> future = RequestFuture.newFuture();
        SocialInvitationsRequest.submit(context, priority, tag, shouldBypassCache, future, future);

        try {
            Collection<SocialInvite> invites = future.get().values();

            List<String> eventIds = new ArrayList<>(invites.size());
            for (SocialInvite invite : invites) {
                eventIds.add(invite.eventId);
            }

            RequestFuture<List<Event>> future2 = RequestFuture.newFuture();
            MultiEventsRequest.submit(context, eventsContext, eventIds, priority, tag,
                    shouldBypassCache, true, future2, future2);
            Map<String, Event> eventsMap = Utils.getMap();
            for (Event event: future2.get()) {
                eventsMap.put(event.id, event);
            }

            List<EventInvitation> eventInvitations = new ArrayList<>(invites.size());
            for (SocialInvite invite : invites) {
                Event event = eventsMap.get(invite.eventId);
                if (event != null) {
                    eventInvitations.add(new EventInvitation(event));
                }
            }
            return eventInvitations;
        } catch (InterruptedException|ExecutionException e) {
            // Ignore.
        }

        return null;
    }

    protected void onPostExecute(@Nullable List<EventInvitation> result) {
        if (result != null) {
            listener.onResponse(result, false);
        } else {
            errorListener.onErrorResponse(new VolleyError("no response"));
        }
    }
}
