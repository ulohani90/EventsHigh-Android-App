package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.app.Activity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;

import org.w3c.dom.Text;

public class TicketReviewActivity extends Activity {

    TextView tvEventsName,tvEventsLocation,tvNoOfTickets;
    TextView tvDate,tvTime,tvSeats;
    TextView tvPrevSubtotal, tvNowSubtotal, tvNowTotal;
    TextView tvInternetHandingFee, tvSavedAmt;
    LinearLayout llTicketCardList;


    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        mapViews();

    }

    private void mapViews(){
        tvEventsName = (TextView)findViewById(R.id.tv_event_name);
        tvEventsLocation = (TextView)findViewById(R.id.tv_event_location);
        tvNoOfTickets = (TextView)findViewById(R.id.tv_no_of_tickets);
        tvDate = (TextView)findViewById(R.id.tv_event_date);
        tvTime = (TextView)findViewById(R.id.tv_event_time);
        tvSeats = (TextView)findViewById(R.id.tv_ticket_description);
        tvPrevSubtotal = (TextView)findViewById(R.id.tv_subtotal_amt_before);
        tvNowSubtotal = (TextView)findViewById(R.id.tv_subtotal_amt_new);
        tvNowTotal = (TextView)findViewById(R.id.tv_total_amt);
        tvInternetHandingFee = (TextView)findViewById(R.id.tv_internet_fee_amt);
        tvSavedAmt = (TextView)findViewById(R.id.tv_amt_save);
        llTicketCardList = (LinearLayout)findViewById(R.id.ll_guest_ticket_list);
    }

}
