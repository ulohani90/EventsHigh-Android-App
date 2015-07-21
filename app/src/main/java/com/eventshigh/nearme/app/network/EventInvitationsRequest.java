package com.eventshigh.nearme.app.network;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.eventshigh.nearme.app.network.EventInvitationsRequest.EventInvitation;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest.SocialFriend;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest.SocialInvite;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class EventInvitationsRequest extends AsyncTask<Void, Void, List<EventInvitation>> {
    public static class EventInvitation {
        public final String eventId;
        public final SocialFriend invitedBy;

        public EventInvitation(String eventId, SocialFriend invitedBy) {
            this.eventId = eventId;
            this.invitedBy = invitedBy;
        }
    }

    public static void submit(Context context, Priority priority, Object tag, boolean shouldBypassCache,
            Listener<List<EventInvitation>> listener, ErrorListener errorListener) {
        new EventInvitationsRequest(context, priority, tag, shouldBypassCache, listener, errorListener).execute();
    }

    private final Context context;
    private final Priority priority;
    private final Object tag;
    private final boolean shouldBypassCache;
    private final Listener<List<EventInvitation>> listener;
    private final ErrorListener errorListener;

    public EventInvitationsRequest(Context context, Priority priority, Object tag, boolean shouldBypassCache,
            Listener<List<EventInvitation>> listener, ErrorListener errorListener) {
        this.context = context;
        this.priority = priority;
        this.tag = tag;
        this.shouldBypassCache = shouldBypassCache;
        this.listener = listener;
        this.errorListener = errorListener;
    }

    @Override
    protected List<EventInvitation> doInBackground(Void... params) {
        RequestFuture<List<SocialInvite>> future = RequestFuture.newFuture();
        SocialInvitationsRequest.submit(context, priority, tag, shouldBypassCache, future, future);

        try {
            List<SocialInvite> invites = future.get();
            List<EventInvitation> eventInvitations = new ArrayList<>(invites.size());
            for (SocialInvite invite : invites) {
                SocialFriend invitedBy = invite.getInvitedBy();
                if (invitedBy != null) {
                    eventInvitations.add(new EventInvitation(invite.eventId, invitedBy));
                }
            }
            return eventInvitations;
        } catch (InterruptedException|ExecutionException e) {
            errorListener.onErrorResponse(new VolleyError(e));
        }

        return null;
    }

    protected void onPostExecute(@Nullable List<EventInvitation> result) {
        if (result != null) {
            listener.onResponse(result, false);
        }
    }
}
