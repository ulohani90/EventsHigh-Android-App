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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.user.Account;

public class WriteReviewActivity extends AppCompatActivity implements View.OnTouchListener{

    private LinearLayout verifyPhnLayout;
    Account account;


    WriteReviewRatingFragment writeReviewRatingFragment;
    WriteReviewDescriptionFragment writeReviewDescriptionFragment;
    MovieDetailObject movieDetailObject;
    protected int movie_rated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        //phone verify
        account = new Account(this);
        verifyPhnLayout = (LinearLayout)findViewById(R.id.verify_phn_layout);
        (findViewById(R.id.verify_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyClicked();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Bundle bundle = this.getIntent().getExtras();
        if(bundle !=null){
            movieDetailObject = bundle.getParcelable(MovieDetailActivity.MOVIE_DETAIL_OBJECT);
        }else{
            //Do something here if data not received
        }

        if(savedInstanceState == null){
            writeReviewRatingFragment = WriteReviewRatingFragment.newInstance(this);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container,writeReviewRatingFragment)
                    .commit();
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if(account!=null && !(account.getUserInfo().isVerified)){
            verifyPhnLayout.setClickable(true);
            verifyPhnLayout.setVisibility(View.VISIBLE);
        }else{
            verifyPhnLayout.setVisibility(View.GONE);
        }
    }


    public void onMovieRated(){

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

    float xDown;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            // save down X coordinate
            xDown = event.getX();
        } else if(event.getAction() == MotionEvent.ACTION_UP){
            // if user moves do not move the finger, update RatingBar value
            if(Math.abs(xDown - event.getX()) < 5){
                onMovieRated();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void verifyClicked(){
        startActivity(new Intent(this, PhoneLoginActivity.class));
    }

    @Override
    public void onBackPressed() {
       super.onBackPressed();
       overridePendingTransition(R.anim.animate_slide_down, R.anim.animate_slide_up);

    }
}

