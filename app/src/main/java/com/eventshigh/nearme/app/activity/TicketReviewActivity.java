package com.eventshigh.nearme.app.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.network.GatewayUrlRequest;
import com.eventshigh.nearme.app.network.MovieReviewSubmitRequest;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class TicketReviewActivity extends AppCompatActivity implements View.OnClickListener{

    TextView tvEventsName, tvEventsLocation, tvNoOfTickets;
    TextView tvDate, tvTime, tvSeats;
    TextView tvNowTotal;
    TextView tvSavedAmt;
    LinearLayout llTicketCardList;
    ImageButton ibtnEdit;
    LinearLayout llAmtSave;
    Button btnReviewNext;

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

    Bundle bundle;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_review);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mapViews();
        bundle = getIntent().getExtras();
        mapIntentData();

    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.ibtn_edit_ticket_details:
            onBackPressed();
            break;
            case R.id.btn_ticket_review_next:
                //callGatewayBrowser("https://www.reddit.com//");
                prepareGatewayReq();
                makeGatewayReq();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void mapViews() {
        tvEventsName = (TextView) findViewById(R.id.tv_event_name);
        tvEventsLocation = (TextView) findViewById(R.id.tv_event_location);
        tvNoOfTickets = (TextView) findViewById(R.id.tv_no_of_tickets);
        tvDate = (TextView) findViewById(R.id.tv_event_date);
        tvTime = (TextView) findViewById(R.id.tv_event_time);
        tvSeats = (TextView) findViewById(R.id.tv_ticket_description);
        tvNowTotal = (TextView) findViewById(R.id.tv_total_amt);
        llAmtSave = (LinearLayout)findViewById(R.id.ll_amt_save);
        tvSavedAmt = (TextView) findViewById(R.id.tv_amt_save);
        llTicketCardList = (LinearLayout) findViewById(R.id.ll_guest_ticket_list);
        ibtnEdit = (ImageButton)findViewById(R.id.ibtn_edit_ticket_details);
        ibtnEdit.setOnClickListener(this);
        btnReviewNext = (Button)findViewById(R.id.btn_ticket_review_next);
        btnReviewNext.setOnClickListener(this);
    }

    private void mapIntentData(){

        event = bundle.getParcelable(EventDetailActivity.EVENT_OBJECT);
        noOfTickets = (int)bundle.getDouble(EventBookingDetailActivity.EVENT_TOTAL_TICKETS);
        total = bundle.getDouble(EventBookingDetailActivity.EVENT_TOTAL_PRICE);
        dateString = bundle.getString(EventBookingDetailActivity.EVENT_DATE_SELECTED);
        eventTime = bundle.getParcelable(EventBookingDetailActivity.EVENT_TIME_SELECTED);
        prices = bundle.getParcelableArrayList(EventBookingDetailActivity.EVENT_TICKETS_DESCRIPTION);
        arrayDetailCards = bundle.getString(GuestDetailActivity.GUEST_DETAIL_ARRAY);
        tvEventsName.setText(event.title);
        tvEventsLocation.setText(event.venue);
        tvNoOfTickets.setText(noOfTickets+" Ticket(s)");
        tvNowTotal.setText("₹"+total+"");
        tvDate.setText(eventTime.date);
        tvTime.setText(eventTime.time);
        for(EhPrices ehp:prices){
            discount += (ehp.discountValue*ehp.count);
            if(ehp.count>0)
            seatDetails.append(ehp.count+" "+ehp.name+"\n");
        }
        tvSeats.setText(seatDetails);
        if(discount == 0){
            llAmtSave.setVisibility(View.GONE);
        }else{
            tvSavedAmt.setText(" ₹"+discount);
        }
        addCards(arrayDetailCards);
    }

    private void addCards(String arrayDetailCards) {
        try {
            arrayGuestDetails = new JSONArray(arrayDetailCards);
            int noOfCards = arrayGuestDetails.length();
            for (int i = 0; i < noOfCards; i++) {
                LinearLayout llGuestLayout = (LinearLayout)
                        getLayoutInflater().inflate(R.layout.card_ticket_review_guest, null);
                JSONObject jsonObject = arrayGuestDetails.getJSONObject(i);

                ((TextView) llGuestLayout.findViewById(R.id.tv_guest_name))
                        .setText(jsonObject.getString("name"));
                ((TextView) llGuestLayout.findViewById(R.id.tv_guest_email))
                        .setText(jsonObject.getString("email"));
                ((TextView) llGuestLayout.findViewById(R.id.tv_guest_phone))
                        .setText(jsonObject.getString("phone"));
                llTicketCardList.addView(llGuestLayout);
            }
        }catch (JSONException e){
     }

    }

    Map<String, String> params;

    private void prepareGatewayReq() {
        params = new HashMap<String,String>();
        params.put("eid", event.id);

        final SimpleDateFormat ISO_8601_DATE_FORMAT = new SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mmZ");
        TimeZone ist = TimeZone.getTimeZone("GMT+0530");
        ISO_8601_DATE_FORMAT.setTimeZone(ist);
        String nowAsISO = ISO_8601_DATE_FORMAT.format(new Date(eventTime.longtime));

        params.put("datetime", nowAsISO);


        for (EhPrices ehp : prices) params.put("ticketName", ehp.name+"");
        for (EhPrices ehp : prices) params.put("ticketValue", ehp.value+"");
        for (EhPrices ehp : prices) params.put("ticketNum", ehp.count+"");

        int noOfCards = arrayGuestDetails.length();
        try {
            for (int i = 0; i < noOfCards; i++)
                params.put("firstName", arrayGuestDetails.getJSONObject(i).getString("name"));
            for (int i = 0; i < noOfCards; i++)
                params.put("mobile", arrayGuestDetails.getJSONObject(i).getString("phone"));
            for (int i = 0; i < noOfCards; i++)
                params.put("email", arrayGuestDetails.getJSONObject(i).getString("email"));
        }catch (JSONException e){
        }
        params.put("paymode","online");
        params.put("isMobile","true");
        params.put("src","eh-android");

        Log.e("JSON Gateway Req", params.toString());

    }

    private void makeGatewayReq(){
        GatewayUrlRequest.submit(this, params, Request.Priority.HIGH, new Response.Listener<String>(){
                    @Override
                    public void onResponse(String string, boolean b) {
                        Log.i("Gateway Url Req Success", "true");
                            callGatewayBrowser(string);
                        }
                }, new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.i("Gateway Url Req failure", "true");
                    }

                });
   }

    private void callGatewayBrowser(String url){
        Toast.makeText(this, url, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, CustomUrlActivity.class);
        Uri myUri = Uri.parse(url);
        intent.setData(myUri);
        startActivity(intent);
    }

}