package com.eventshigh.nearme.app.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MyTicketObject;
import com.eventshigh.nearme.app.network.MovieReviewSubmitRequest;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.view.CircularImageView;

import org.json.JSONException;
import org.json.JSONObject;

public class WriteRecentEventReviewActivity extends AppCompatActivity{

    private static String JSON_KEY_REVIEWER_ID = "reviewer_id";//mobi no
    private static String JSON_KEY_REVIEW_FOR = "review_for";
    private static String JSON_KEY_REVIEW_ENTITY = "reviewed_entity";
    private static String JSON_KEY_REVIEW_ENTITY_ID = "reviewed_entity_id";
    private static String JSON_KEY_REVIEW_TEXT = "review_text";
    private static String JSON_KEY_REVIEW_BY = "review_by";
    private static String JSON_KEY_REVIEW_PLATFORM = "review_platform";
    private static String JSON_KEY_REVIEW_DEVICE_ID = "review_device_id";
    private static String JSON_KEY_REVIEW_RATINGS = "ratings";
    private static String JSON_KEY_CITY = "reviewed_entity_city";

    ProgressDialog progress;
    TextView reviewHeader, btnWriteReview;
    RatingBar ratingBar;
    EditText etDesc;

    CircularImageView circularImageView;
    Bundle bundle;
    MyTicketObject myTicketObject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_write_review_description);


        reviewHeader = (TextView)findViewById(R.id.tv_write_review_description_movie_name);
        btnWriteReview = (TextView)findViewById(R.id.btn_write_review);
        ratingBar = (RatingBar)findViewById(R.id.rb_write_rating_description);
        circularImageView = (CircularImageView)findViewById(R.id.civ_movie_pic);
        Intent intent = getIntent();
        bundle = intent.getExtras();

        myTicketObject = bundle.getParcelable(ReviewEventAttendedDialog.MY_TICKET);
        circularImageView.setVisibility(View.GONE);
        if(myTicketObject != null){
            reviewHeader.setText(myTicketObject.getEventName());
        }
        Bundle bundle2 = intent.getBundleExtra(ReviewEventAttendedDialog.MY_TICKET_RATING_BUNDLE);
        ratingBar.setRating(bundle2.getFloat(ReviewEventAttendedDialog.MY_TICKET_RATING,0));
        etDesc = (EditText)findViewById(R.id.et_write_review_description);
        btnWriteReview = (TextView)findViewById(R.id.btn_write_review);
        btnWriteReview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placeReviewActionIfEvent();
            }
        });
    }


    public void placeReviewActionIfEvent() {
        try {
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put(JSON_KEY_REVIEWER_ID, (new Account(this)).getUserInfo().phoneNo);
            jsonObject.put(JSON_KEY_REVIEW_FOR, "event");
            jsonObject.put(JSON_KEY_REVIEW_ENTITY_ID, myTicketObject.getEventId());
            jsonObject.put(JSON_KEY_REVIEW_ENTITY, myTicketObject.getEventName());
            jsonObject.put(JSON_KEY_REVIEW_RATINGS, (int) ratingBar.getRating());
            jsonObject.put(JSON_KEY_REVIEW_TEXT, etDesc.getText().toString());
            jsonObject.put(JSON_KEY_REVIEW_BY, (new Account(this)).getUserInfo().name);
            jsonObject.put(JSON_KEY_CITY, (new Account(this)).getLastCity().name());
            jsonObject.put(JSON_KEY_REVIEW_PLATFORM, "android");
            jsonObject.put(JSON_KEY_REVIEW_DEVICE_ID, Settings.Secure.getString
                    (getContentResolver(), Settings.Secure.ANDROID_ID));

            progress = ProgressDialog.show(this, null, "Submitting Review. Please Wait...");
            MovieReviewSubmitRequest.submit(this,
                    jsonObject, Request.Priority.HIGH, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject, boolean b) {
                            if (this != null) {
                                if (progress != null)
                                    progress.dismiss();
                                Preferences.getInstance(getApplicationContext()).setIsReviewAdded(true);
                                Log.i("Message Success", "true");
                                Toast.makeText(getApplicationContext(), "Your review has been added successfully", Toast.LENGTH_SHORT).show();
                                closeParentActivity();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            if (progress != null)
                                progress.dismiss();
                            Log.i("Message failure", "true" + jsonObject.toString());
                        }
                    });
        } catch (JSONException e) {
            Crashlytics.getInstance().core.logException(e);
        }
    }

    public void closeParentActivity(){
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setResult(RESULT_OK, getIntent());
                finish();
        }
        }, 1000);
    }

}