package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.network.EventInvitationsRequest.EventInvitation;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;

public class EventInvitationData implements AdapterData {
    private final EventInvitation invite;
    private final SocialInvitationsRequest.SpecialCoupons special;
    private final BaseContextActivity activity;

    public EventInvitationData(EventInvitation invite, BaseContextActivity activity) {
        this.invite = invite;
        this.activity = activity;
        this.special = null;
    }
    public EventInvitationData(SocialInvitationsRequest.SpecialCoupons special, BaseContextActivity activity) {
        this.special = special;
        this.activity = activity;
        this.invite = null;
    }

    @Override
    public DataType getType() {
        return DataType.EVENT_INVITATION;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        if(invite!=null) {
            ((EventInvitationCard) card).bindView(invite, activity);
        }else{
            ((EventInvitationCard) card).bindSpecialView(special,activity);
        }
    }

    @Override
    public String getId() {
        if(invite!=null) {
            return "invite: " + invite.event.id;
        }else{
            return "special" +special.title;
        }
    }
}
