package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TicketReviewActivity extends AppCompatActivity{

    TextView tvEventsName, tvEventsLocation, tvNoOfTickets;
    TextView tvDate, tvTime, tvSeats;
    TextView tvPrevSubtotal, tvNowSubtotal, tvNowTotal;
    TextView tvInternetHandingFee, tvSavedAmt;
    LinearLayout llTicketCardList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_review);
        mapViews();
        Bundle b = getIntent().getExtras();
        String arrayDetailCards = b.getString("array_detail_cards");
        addCards(arrayDetailCards);
    }

    private void mapViews() {
        tvEventsName = (TextView) findViewById(R.id.tv_event_name);
        tvEventsLocation = (TextView) findViewById(R.id.tv_event_location);
        tvNoOfTickets = (TextView) findViewById(R.id.tv_no_of_tickets);
        tvDate = (TextView) findViewById(R.id.tv_event_date);
        tvTime = (TextView) findViewById(R.id.tv_event_time);
        tvSeats = (TextView) findViewById(R.id.tv_ticket_description);
        tvPrevSubtotal = (TextView) findViewById(R.id.tv_subtotal_amt_before);
        tvNowSubtotal = (TextView) findViewById(R.id.tv_subtotal_amt_new);
        tvNowTotal = (TextView) findViewById(R.id.tv_total_amt);
        tvInternetHandingFee = (TextView) findViewById(R.id.tv_internet_fee_amt);
        tvSavedAmt = (TextView) findViewById(R.id.tv_amt_save);
        llTicketCardList = (LinearLayout) findViewById(R.id.ll_guest_ticket_list);
    }

    private void addCards(String arrayDetailCards) {
        try {
            JSONArray arrayGuestDetails = new JSONArray(arrayDetailCards);
            int noOfCards = arrayGuestDetails.length();
            for (int i = 0; i < noOfCards; i++) {
                LinearLayout llGuestLayout = (LinearLayout)
                        getLayoutInflater().inflate(R.layout.card_ticket_review_guest, null);
                JSONObject jsonObject = arrayGuestDetails.getJSONObject(i);

                ((TextView) llGuestLayout.findViewById(R.id.tv_guest_name))
                        .setText(jsonObject.getString("name"));
                ((TextView) llGuestLayout.findViewById(R.id.tv_guest_email))
                        .setText(jsonObject.getString("name"));
                ((TextView) llGuestLayout.findViewById(R.id.tv_guest_phone))
                        .setText(jsonObject.getString("name"));
            }
        }catch (JSONException e){
     }

    }
}