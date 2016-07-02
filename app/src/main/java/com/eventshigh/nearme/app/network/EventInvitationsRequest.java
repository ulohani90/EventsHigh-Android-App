package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.network.EventInvitationsRequest.EventInvitation;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest.SocialInvite;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class EventInvitationsRequest extends AsyncTask<Void, Void, EventInvitationsRequest.InvitaionData> {
    public static class EventInvitation {
        public final Event event;
        public final SocialFriend invitedBy;

        public EventInvitation(Event event, SocialFriend invitedBy) {
            this.event = event;
            this.invitedBy = invitedBy;
        }
    }

    public static class InvitaionData{
        public final List<EventInvitation> invitations;
        public final List<SocialInvitationsRequest.SpecialCoupons> specials;

        public InvitaionData(List<EventInvitation> invitations ,List<SocialInvitationsRequest.SpecialCoupons> specials ){
            this.invitations = invitations;
            this.specials = specials;
        }

    }

    public static void submit(Context context, EventsContext eventsContext, Priority priority,
            Object tag, boolean shouldBypassCache, Listener<InvitaionData> listener,
            ErrorListener errorListener) {
        new EventInvitationsRequest(context, eventsContext, priority, tag, shouldBypassCache,
                listener, errorListener).execute();
    }

    private final Context context;
    private final EventsContext eventsContext;
    private final Priority priority;
    private final Object tag;
    private final boolean shouldBypassCache;
    private final Listener<InvitaionData> listener;
    private final ErrorListener errorListener;

    public EventInvitationsRequest(Context context, EventsContext eventsContext, Priority priority,
           Object tag, boolean shouldBypassCache, Listener<InvitaionData> listener,
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
    protected InvitaionData doInBackground(Void... params) {
        RequestFuture<SocialInvitationsRequest.CommonInviteObject> future = RequestFuture.newFuture();
        SocialInvitationsRequest.submit(context, priority, tag, shouldBypassCache, future, future);

        try {

            Collection<SocialInvite> invites = future.get().getInvites().values();

            List<String> eventIds = new ArrayList<>(invites.size());
            for (SocialInvite invite : invites) {
                SocialFriend invitedBy = invite.getInvitedBy();
                if (invitedBy != null) {
                    eventIds.add(invite.eventId);
                }
            }

            RequestFuture<List<Event>> future2 = RequestFuture.newFuture();
            MultiEventsRequest.submit(context, eventsContext, eventIds, priority, tag,
                    shouldBypassCache, true,true,future2, future2);
            Map<String, Event> eventsMap = Utils.getMap();
            for (Event event: future2.get()) {
                eventsMap.put(event.id, event);
            }

            List<EventInvitation> eventInvitations = new ArrayList<>(invites.size());
            for (SocialInvite invite : invites) {
                Event event = eventsMap.get(invite.eventId);
                SocialFriend invitedBy = invite.getInvitedBy();
                if (event != null && invitedBy != null) {
                    eventInvitations.add(new EventInvitation(event, invitedBy));
                }
            }
            return new InvitaionData(eventInvitations ,future.get().getSpecials());

        } catch (InterruptedException|ExecutionException e) {
            Crashlytics.getInstance().core.logException(e);
        }

        return null;
    }

    protected void onPostExecute(@Nullable InvitaionData result) {
        if (result != null) {
            listener.onResponse(result, false);
        } else {
            errorListener.onErrorResponse(new VolleyError("no response"));
        }
    }
}
