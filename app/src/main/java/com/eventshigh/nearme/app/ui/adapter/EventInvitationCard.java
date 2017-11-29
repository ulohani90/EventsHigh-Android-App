package com.eventshigh.nearme.app.ui.adapter;

import android.content.Intent;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.EventsGridActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.EventInvitationsRequest.EventInvitation;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.IntentUtils;

import java.util.Date;
import java.util.TimeZone;

public class EventInvitationCard extends ViewHolder {

    private final String TARGET_CONSTANT_EVENTSHIGHSPECIAL = "EventsHigh Specials";
    private final ImageView imageView;
    private final TextView titleView;
    private final TextView subtitleView;
    private final TextView validBeforeText;

    static EventInvitationCard newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_invitation, parent, false);
        return new EventInvitationCard(view);
    }

    public EventInvitationCard(View itemView) {
        super(itemView);

        imageView = (ImageView) itemView.findViewById(R.id.invite_image);
        titleView = (TextView) itemView.findViewById(R.id.invite_title);
        subtitleView = (TextView) itemView.findViewById(R.id.subtitle);
        validBeforeText = (TextView)itemView.findViewById(R.id.valid_before);
    }

    public void bindView(final EventInvitation invite, final BaseContextActivity activity) {
        String invitedByName = invite.invitedBy.getName();
        titleView.setText(invitedByName + " has invited you to an event!");
        subtitleView.setText(invite.event.title + " @ " + invite.event.getShortAddress());

        int size = imageView.getLayoutParams().height;
        imageView.setImageDrawable(invite.invitedBy.getDrawable(activity, size));

        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showEventDetails(invite.event, "invitation", null);
            }
        });
        validBeforeText.setVisibility(View.GONE);
    }

    public void bindSpecialView(final SocialInvitationsRequest.SpecialCoupons special, final BaseContextActivity activity) {

        titleView.setText(special.title);
        subtitleView.setText(special.message);

        //int size = imageView.getLayoutParams().height;
        imageView.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_launcher));

        if(special.coupon!=null){
            validBeforeText.setVisibility(View.VISIBLE);
            validBeforeText.setText("* Valid before " + DateTimeUtils.dateToEventTime(new Date(special.coupon.validTillTimestamp), TimeZone.getTimeZone(Event.DEFAULT_TIME_ZONE)));
        }

        itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(special.target.equalsIgnoreCase(TARGET_CONSTANT_EVENTSHIGHSPECIAL)){
                    EventsContext param = new EventsContext(null, "eventshigh specials");
                    Intent intent = new Intent(activity, EventsGridActivity.class)
                            .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
                    intent.putExtra("special_obj",special);
                    activity.startActivity(intent);
                }else{
                  //  Intent intent = new Intent(activity, ReferralActivity.class);
                   // activity.startActivity(intent);
                }

            }
        });

    }
}
