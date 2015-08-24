package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.network.EventInvitationsRequest.EventInvitation;

public class EventInvitationData implements AdapterData {
    private final EventInvitation invite;
    private final BaseContextActivity activity;

    public EventInvitationData(EventInvitation invite, BaseContextActivity activity) {
        this.invite = invite;
        this.activity = activity;
    }

    @Override
    public DataType getType() {
        return DataType.EVENT_INVITATION;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        ((EventInvitationCard) card).bindView(invite, activity);
    }

    @Override
    public String getId() {
        return "invite: " + invite.event.id;
    }
}
