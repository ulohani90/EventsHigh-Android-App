package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.MyTicketsFragment;
import com.eventshigh.nearme.app.data.MyTicketObject;

/**
 * @author shubham
 * @since 15/6/16.
 */
public class MyTicketData implements AdapterData {
    MyTicketObject myticket;
    BaseContextActivity activity;
    MyTicketsFragment myTicketsFragment;

    @Override
    public DataType getType() {
        return DataType.MY_TICKETS_CARD;
    }

    public MyTicketData(MyTicketObject myTicket, BaseContextActivity activity,MyTicketsFragment myTicketsFragment) {
        this.myticket = myTicket;
        this.activity = activity;
        this.myTicketsFragment = myTicketsFragment;
    }

    public void onBindViewHolder(RecyclerView.ViewHolder card, int position,EventsAdapter.OnMyTicketItemClickedListener myTicketItemClickedListener) {
        ((MyTicketCard) card).bindData(myticket, activity, position, myTicketItemClickedListener, myTicketsFragment);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
    }


    @Override
    public String getId() {
        return myticket.getBookingId();
    }
}
