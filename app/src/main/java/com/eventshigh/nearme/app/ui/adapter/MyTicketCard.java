package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.MyTicketsFragment;
import com.eventshigh.nearme.app.data.MyTicketObject;

import java.util.Arrays;
import java.util.List;


/**
 * @author shubham
 * @since 15/6/16.
 */

public class MyTicketCard extends RecyclerView.ViewHolder{

    TextView tvEventName, tvEventLocation, tvNoTickets;
    TextView tvBookingId, tvGuestName, tvGuestEmail, tvGuestPhone;
    TextView tvDate, tvTime, tvSeat;

    TextView tvShowMoreLess;
    LinearLayout llHidingMask;

    LinearLayout maskingRowView;
    TextView rowDate, rowBookingId;

    EventsAdapter.OnMyTicketItemClickedListener myTicketItemClickedListener;

    public static MyTicketCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_my_ticket, parent, false);
        return new MyTicketCard(view);
    }

    public MyTicketCard(View itemView) {
        super(itemView);
        tvEventName = (TextView)itemView.findViewById(R.id.tv_event_name);
        tvEventLocation = (TextView)itemView.findViewById(R.id.tv_event_location);
        tvNoTickets = (TextView)itemView.findViewById(R.id.tv_no_of_tickets);

        tvBookingId = (TextView)itemView.findViewById(R.id.tv_event_booking_id);
        tvGuestName = (TextView)itemView.findViewById(R.id.tv_event_guest_name);
        tvGuestEmail = (TextView)itemView.findViewById(R.id.tv_event_guest_email);
        tvGuestPhone = (TextView)itemView.findViewById(R.id.tv_event_guest_phone);

        tvDate = (TextView)itemView.findViewById(R.id.tv_event_date);
        tvTime = (TextView)itemView.findViewById(R.id.tv_event_time);
        tvSeat = (TextView)itemView.findViewById(R.id.tv_ticket_description);

        llHidingMask = (LinearLayout)itemView.findViewById(R.id.ll_hiding_mask);
        tvShowMoreLess = (TextView) itemView.findViewById(R.id.tv_show_more_less);

        maskingRowView = (LinearLayout)itemView.findViewById(R.id.row_view_ticket);
        rowDate = (TextView)itemView.findViewById(R.id.row_date);
        rowBookingId = (TextView)itemView.findViewById(R.id.row_booking_id);
    }


    public void bindData(final MyTicketObject myTicketObject, final BaseContextActivity activity, final int position, final EventsAdapter.OnMyTicketItemClickedListener myTicketItemClickedListener, MyTicketsFragment myTicketsFragment){

        tvShowMoreLess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myTicketItemClickedListener.onItemClicked(position);
            }
        });
        this.myTicketItemClickedListener = myTicketItemClickedListener;
        tvNoTickets.setText(myTicketObject.getNoTicket() + " Ticket(s)");
        tvEventName.setText(myTicketObject.getEventName());
        tvEventLocation.setVisibility(View.GONE);
        tvBookingId.setText(myTicketObject.getBookingId());
        tvGuestName.setText(myTicketObject.getUserName());
        tvGuestEmail.setText(myTicketObject.getUserEmail());
        tvGuestPhone.setText(myTicketObject.getUserMobileNo());

        List<String> timeList = Arrays.asList(myTicketObject.getEventTime().split(","));
        tvTime.setText(timeList.get(1));
        tvDate.setText(timeList.get(0));
        tvSeat.setText(myTicketObject.getTicketType());

        rowDate.setText(myTicketObject.getEventTime());
        rowBookingId.setText(myTicketObject.getBookingId());

        if(myTicketsFragment.clickPosition == position){
            llHidingMask.setVisibility(View.VISIBLE);
            maskingRowView.setVisibility(View.GONE);
            tvNoTickets.setVisibility(View.VISIBLE);
            tvShowMoreLess.setText("Show Less");
        }else{
            llHidingMask.setVisibility(View.GONE);
            maskingRowView.setVisibility(View.VISIBLE);
            tvNoTickets.setVisibility(View.GONE);
            tvShowMoreLess.setText("Show More");
        }

    }

}