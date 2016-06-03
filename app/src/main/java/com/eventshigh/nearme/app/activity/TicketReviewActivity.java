package com.eventshigh.nearme.app.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.network.GatewayUrlRequest;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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

    public static final String IS_PAYMENT =  "is_payment";
    public static final String BOOKING_ID =  "booking";
    public static final String IS_PAYMENT_SUCCESS =  "is_success";
    public static final int PAYMENT_REQ_CODE = 100;

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
                //prepareGatewayReq();
                //makeGatewayReq();
                (new AsyncCaller()).execute();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (resultCode == RESULT_OK && requestCode == PAYMENT_REQ_CODE) {
            if (data.hasExtra(IS_PAYMENT_SUCCESS)){
                Toast.makeText(this, "Payment successful",
                        Toast.LENGTH_SHORT).show();
                callConfirmationActivity(data.getStringExtra(BOOKING_ID));
            }else{
                Toast.makeText(this, "You payment was unccessful. Please try again",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void callConfirmationActivity(String bookingId){
            Intent iNext = new Intent(this, TicketConfirmationActivity.class);
            bundle.putString(BOOKING_ID, bookingId);
            iNext.putExtras(bundle);
            startActivity(iNext);
    }

        /*

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

        Log.e("Gateway Req", params.toString());

    }

    private void makeGatewayReq(){

        GatewayUrlRequest.submit(this, params, Request.Priority.HIGH, new Response.Listener<String>(){
                    @Override
                    public void onResponse(String string, boolean b) {
                        Log.i("Gateway Url Req Success", "true");
                            //callGatewayBrowser(string);
                        }
                }, new Response.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.i("Gateway Url Req failure", "true" + volleyError.toString());
                    }

                });
   }



    private void callGatewayBrowser(String url){
        Toast.makeText(this, url, Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, CustomUrlActivity.class);
        //Uri myUri = Uri.parse(url);
        //intent.setData(myUri);
        //startActivity(intent);
    }
    */


    private class AsyncCaller extends AsyncTask<Void, Void, Void> {
        ProgressDialog pdLoading = new ProgressDialog(TicketReviewActivity.this);
        String location = "";
        boolean isSuccess = false;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //this method will be running on UI thread
            pdLoading.setMessage("\tLoading...");
            pdLoading.show();
        }
        @Override
        protected Void doInBackground(Void... params) {
            //doPostRequest();
            doPost();
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            super.onPostExecute(result);
            pdLoading.dismiss();
                if(!Utils.checkIfStringEmpty(location)){
                Intent intent = new Intent(TicketReviewActivity.this, CustomUrlActivity.class);
                Uri myUri = Uri.parse(EventsHighEndpoints.GATEWAY_URI_BASE+location);
                intent.putExtra(TicketReviewActivity.IS_PAYMENT,true);
                intent.setData(myUri);
                startActivityForResult(intent, PAYMENT_REQ_CODE);
            }else{
                Toast.makeText(getApplicationContext(),"Try Again",Toast.LENGTH_SHORT).show();
            }

        }

        /*
        private void doPostRequest(){
            OkHttpClient client = new OkHttpClient();
            client.setFollowRedirects(false);
            client.setFollowSslRedirects(false);

            RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"),
                    params.toString());
            try {
                Request request = new Request.Builder()
                        .addHeader("Content-Type","application/x-www-form-urlencoded")
                        .url(EventsHighEndpoints.GATEWAY_URI_BASE)
                        .post(body)
                        .build();
                Response response = client.newCall(request).execute();
                Log.e("Gateway URL ",response.headers().toString());
            }catch (IOException ioe){
                Log.e("Gateway URL ",ioe.toString());
            }
        }
        */

        private void doPost(){
            try {
                URL url = new URL(EventsHighEndpoints.GATEWAY_URI_BASE+"gateway?cmode=override");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setFollowRedirects(false);

                // Send request
                OutputStream os = connection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));

                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("eid", event.id));
                final SimpleDateFormat ISO_8601_DATE_FORMAT = new SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mmZ");
                TimeZone ist = TimeZone.getTimeZone("GMT+0530");
                ISO_8601_DATE_FORMAT.setTimeZone(ist);
                String nowAsISO = ISO_8601_DATE_FORMAT.format(new Date(eventTime.longtime));

                params.add(new BasicNameValuePair("datetime", nowAsISO));

                for (EhPrices ehp : prices)params.add(new BasicNameValuePair("ticketName", ehp.name + ""));
                for (EhPrices ehp : prices)params.add(new BasicNameValuePair("ticketValue", ehp.value + ""));
                for (EhPrices ehp : prices)params.add(new BasicNameValuePair("ticketNum", ehp.count + ""));



                int noOfCards = arrayGuestDetails.length();
                    for (int i = 0; i < noOfCards; i++)
                        params.add(new BasicNameValuePair("firstName", arrayGuestDetails.getJSONObject(i).getString("name")));
                    for (int i = 0; i < noOfCards; i++)
                        params.add(new BasicNameValuePair("mobile", arrayGuestDetails.getJSONObject(i).getString("phone")));
                    for (int i = 0; i < noOfCards; i++)
                        params.add(new BasicNameValuePair("email", arrayGuestDetails.getJSONObject(i).getString("email")));


                params.add(new BasicNameValuePair("paymode", "online"));
                params.add(new BasicNameValuePair("isMobile", "true"));
                params.add(new BasicNameValuePair("src", "eh-android"));

                Log.e("Server Respo", params.toString());

                writer.write(getQuery(params));
                writer.flush();
                writer.close();

                // Get Response
                int status = connection.getResponseCode();
                location = connection.getHeaderField("location");
                Log.e("location gateway",connection.getHeaderFields().toString());
            }catch (Exception ioe){
                Log.e("Server Respo", ioe.toString());
            }

            /*
            HttpClient httpclient = new DefaultHttpClient();
            HttpContext localContext = new BasicHttpContext();

            // HTTP parameters stores header etc.
            HttpParams httpParams = new BasicHttpParams();
            httpParams.setParameter("http.protocol.handle-redirects", false);


            // connect and receive
            HttpPost httpPost = new HttpPost(EventsHighEndpoints.GATEWAY_URI_BASE);
            httpPost.setParams(httpParams);
            HttpUriRequest httpUriRequest = new BasicHttpRequest();
            HttpResponse response = httpclient.execute(httpPost,localContext);

            // obtain redirect target
            Header locationHeader = response.getFirstHeader("location");
            if (locationHeader != null) {
                String redirectLocation = locationHeader.getValue();
                System.out.println("loaction: " + redirectLocation);
            } else {
                // The response is invalid and did not provide the new location for
                // the resource.  Report an error or possibly handle the response
                // like a 404 Not Found error.
            }
            */

        }

        private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
        {
            StringBuilder result = new StringBuilder();
            boolean first = true;

            for (NameValuePair pair : params)
            {
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
            }

            return result.toString();
        }

    }

}
