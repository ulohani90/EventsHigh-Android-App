package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RatingBar;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MovieDetailObject;

public class WriteReviewActivity extends AppCompatActivity implements View.OnTouchListener{

    WriteReviewRatingFragment writeReviewRatingFragment;
    WriteReviewDescriptionFragment writeReviewDescriptionFragment;
    MovieDetailObject movieDetailObject;
    protected int movie_rated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);
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


    public void onMovieRated(){

        writeReviewDescriptionFragment =
                WriteReviewDescriptionFragment.newInstance(this);
        Bundle args = new Bundle();
            args.putInt(WriteReviewDescriptionFragment.RATING_COUNT,
                    (int)writeReviewRatingFragment.rbMovieRating.getRating() );
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
}

