package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.network.EventRequest;
import com.eventshigh.nearme.app.network.MovieDetailRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;

public class WriteReviewActivity extends BaseActivity implements View.OnTouchListener {

    private LinearLayout verifyPhnLayout;
    Account account;

    public static final String REVIEW_ENTITY_ID = "review_entity_id";
    public static final String FROM_NOTIFICATION_PARAM = "from notification";
    public static final String REVIEW_ENTITY_IMAGE = "review_entity_image";
    public static final String REVIEW_ENTITY_NAME = "review_entity_name";


    WriteReviewRatingFragment writeReviewRatingFragment;
    WriteReviewDescriptionFragment writeReviewDescriptionFragment;
    MovieDetailObject movieDetailObject;
    Event event;
    protected int movie_rated;

    String type = "";
    boolean isFromNotification;
    Bundle bundle;
    String reviewEntityId;
    String reviewEntityImage;
    String reviewEntityName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        //phone verify
        account = new Account(this);
        verifyPhnLayout = (LinearLayout) findViewById(R.id.verify_phn_layout);
        (findViewById(R.id.verify_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyClicked();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        bundle = this.getIntent().getExtras();
        if (bundle != null) {
            isFromNotification = getIntent().getBooleanExtra(FROM_NOTIFICATION_PARAM, false);
            type = getIntent().getStringExtra(MovieDetailActivity.OBJECT_TYPE);
        }
        if (isFromNotification) {
            reviewEntityId = getIntent().getStringExtra(REVIEW_ENTITY_ID);
            reviewEntityImage = getIntent().getStringExtra(REVIEW_ENTITY_IMAGE);
            reviewEntityName = getIntent().getStringExtra(REVIEW_ENTITY_NAME);
        } else{
            if (type.equals("movie")) {
                movieDetailObject = bundle.getParcelable(MovieDetailActivity.MOVIE_DETAIL_OBJECT);
            } else {
                event = bundle.getParcelable(EventDetailActivity.EVENT_OBJECT);
            }
        }

        if (savedInstanceState == null) {
            writeReviewRatingFragment = WriteReviewRatingFragment.newInstance(this);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container, writeReviewRatingFragment)
                    .commit();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (account != null && !(account.getUserInfo().isVerified)) {
            verifyPhnLayout.setClickable(true);
            verifyPhnLayout.setVisibility(View.VISIBLE);
        } else {
            verifyPhnLayout.setVisibility(View.GONE);
        }
    }


    //on ratingbar touch inside rating review fragment
    float xDown;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // save down X coordinate
            xDown = event.getX();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            // if user moves do not move the finger, update RatingBar value
            if (Math.abs(xDown - event.getX()) < 5) {
                onMovieRated();
                return false;
            }
        }
        return true;
    }

    //util method
    public void onMovieRated() {
        writeReviewDescriptionFragment =
                WriteReviewDescriptionFragment.newInstance(this);
        Bundle args = new Bundle();
        writeReviewDescriptionFragment.setArguments(args);
        // Commit the transaction
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.enter_from_bottom, R.anim.exit_to_top);
                transaction.replace(R.id.fragment_container, writeReviewDescriptionFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        }, 500);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void verifyClicked() {
        startActivity(new Intent(this, PhoneLoginActivity.class));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

