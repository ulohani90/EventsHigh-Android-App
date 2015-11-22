package com.eventshigh.nearme.app.ui.adapter;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.network.EventInvitationsRequest.EventInvitation;

public class EventInvitationCard extends ViewHolder {
    private final TextView titleView;
    private final TextView subtitleView;

    static EventInvitationCard newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_invitation, parent, false);
        return new EventInvitationCard(view);
    }

    public EventInvitationCard(View itemView) {
        super(itemView);

        titleView = (TextView) itemView.findViewById(R.id.invite_title);
        subtitleView = (TextView) itemView.findViewById(R.id.subtitle);
    }

    @SuppressLint("SetTextI18n")
    public void bindView(final EventInvitation invite, final BaseContextActivity activity) {
        titleView.setText("You have been invited to an event!");
        subtitleView.setText(invite.event.title + " @ " + invite.event.getShortAddress());

        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showEventDetails(invite.event, "invitation", null);
            }
        });
    }
}
