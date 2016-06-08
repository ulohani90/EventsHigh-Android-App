package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class TicketConfirmationActivity extends AppCompatActivity implements View.OnClickListener {

    TextView tvEventName, tvEventLocation, tvNoOfTickets;
    TextView tvEventBookingId, tvEventGuestName, tvEventGuestEmailId, tvEventGuestPhone;
    TextView tvEventDate, tvEventTime, tvEventSeatDescription;
    TextView tvEventMapLocation;
    LinearLayout llShareTicketBtn;

    Bundle bundle;
    Event event;
    String dateString;
    DateTimeUtils.EventTime eventTime;
    ArrayList<EhPrices> prices;
    double total = 0;
    int noOfTickets = 0;
    String arrayDetailCards;
    double discount = 0;
    StringBuilder seatDetails = new StringBuilder("");
    JSONArray arrayGuestDetails;
    String ticketLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_confirmation);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mapViews();
        bundle = getIntent().getExtras();
        mapIntentData();
        noOfTickets = (int) bundle.getDouble(EventBookingDetailActivity.EVENT_TOTAL_TICKETS);

    }

    void mapViews() {
        tvEventName = (TextView) findViewById(R.id.tv_event_name);
        tvEventLocation = (TextView) findViewById(R.id.tv_event_location);
        tvNoOfTickets = (TextView) findViewById(R.id.tv_no_of_tickets);
        tvEventGuestPhone = (TextView) findViewById(R.id.tv_event_guest_phone);
        tvEventGuestEmailId = (TextView) findViewById(R.id.tv_event_guest_email);
        tvEventGuestName = (TextView) findViewById(R.id.tv_event_guest_name);
        tvEventBookingId = (TextView) findViewById(R.id.tv_event_booking_id);
        tvEventDate = (TextView) findViewById(R.id.tv_event_date);
        tvEventTime = (TextView) findViewById(R.id.tv_event_time);
        tvEventSeatDescription = (TextView) findViewById(R.id.tv_ticket_description);
        llShareTicketBtn = (LinearLayout) findViewById(R.id.ll_share_ticket_btn);
        llShareTicketBtn.setOnClickListener(this);
        tvEventMapLocation = (TextView) findViewById(R.id.tv_event_map_location);
        tvEventMapLocation.setOnClickListener(this);
    }

    private void mapIntentData() {
        event = bundle.getParcelable(EventDetailActivity.EVENT_OBJECT);
        total = bundle.getDouble(EventBookingDetailActivity.EVENT_TOTAL_PRICE);
        dateString = bundle.getString(EventBookingDetailActivity.EVENT_DATE_SELECTED);
        eventTime = bundle.getParcelable(EventBookingDetailActivity.EVENT_TIME_SELECTED);
        prices = bundle.getParcelableArrayList(EventBookingDetailActivity.EVENT_TICKETS_DESCRIPTION);
        arrayDetailCards = bundle.getString(GuestDetailActivity.GUEST_DETAIL_ARRAY);

        noOfTickets = (int) bundle.getDouble(EventBookingDetailActivity.EVENT_TOTAL_TICKETS);
        ticketLink = bundle.getString(TicketReviewActivity.TICKETING_LINK);

        tvEventName.setText(event.title);
        tvEventLocation.setText(event.venue);
        tvNoOfTickets.setText(noOfTickets + " Ticket(s)");

        tvEventDate.setText(eventTime.date);
        tvEventTime.setText(eventTime.time);
        for (EhPrices ehp : prices) {
            discount += (ehp.discountValue * ehp.count);
            if (ehp.count > 0)
                seatDetails.append(ehp.count + " " + ehp.name + "\n");
        }
        tvEventSeatDescription.setText(seatDetails);

        tvEventBookingId.setText(getIntent().getStringExtra(TicketReviewActivity.BOOKING_ID));
        try {
            arrayGuestDetails = new JSONArray(arrayDetailCards);
            JSONObject jsonObject = arrayGuestDetails.getJSONObject(0);
            tvEventGuestName.setText(jsonObject.getString("firstName"));
            tvEventGuestEmailId.setText(jsonObject.getString("email"));
            tvEventGuestPhone.setText(jsonObject.getString("mobile"));
        } catch (JSONException e) {
        }

        findViewById(R.id.tv_explore_more_booking).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventsContext param = new EventsContext(null, "eventshigh specials");
                Intent intent = new Intent(TicketConfirmationActivity.this, EventsGridActivity.class)
                        .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);

                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, LaunchActivity.class);
        startActivity(intent);
        //NavUtils.navigateUpFromSameTask(this);
        this.finish();
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_event_map_location:
                startMapActivity();
                break;
            case R.id.ll_share_ticket_btn:
                startTicketShareActivity();
                break;
        }
    }


    void startMapActivity() {
        if (event.location != null) {
            Intent intent = event.getShowDirectionsOnMapIntent();
            startActivity(intent);
        } else {
            Toast.makeText(TicketConfirmationActivity.this, "Map location not available", Toast.LENGTH_SHORT).show();
        }
    }


    void startTicketShareActivity() {
        String shareText = "Hey, I just book " + noOfTickets + " tickets for " + event.title + " on EventsHigh. It'll be fun.\n" + ticketLink;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        this.startActivity(shareIntent);

    }


}
