package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RatingBar;

import com.eventshigh.nearme.app.R;

public class WriteReviewActivity extends AppCompatActivity implements WriteReviewRatingFragment.OnMovieRatedListener,
        View.OnTouchListener{

    WriteReviewRatingFragment writeReviewRatingFragment;
    WriteReviewDescriptionFragment writeReviewDescriptionFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        if(savedInstanceState == null){
            writeReviewRatingFragment = WriteReviewRatingFragment.newInstance(this);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.fragment_container,writeReviewRatingFragment)
                    .commit();
        }

    }


    @Override
    public void onMovieRated(int rating_count){

        writeReviewDescriptionFragment =
                WriteReviewDescriptionFragment.newInstance(this);


            Bundle args = new Bundle();
            args.putInt(WriteReviewDescriptionFragment.RATING_COUNT, rating_count);
            writeReviewDescriptionFragment.setArguments(args);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, writeReviewDescriptionFragment);
            transaction.addToBackStack(null);
            // Commit the transaction
            transaction.commit();

    }

    float xDown;
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            // save down X coordinate
            xDown = event.getX();
        } else if(event.getAction() == MotionEvent.ACTION_UP){
            // if user moves do not move the finger, update RatingBar value
            if(Math.abs(xDown - event.getX()) < 5) {
                onMovieRated((int)((RatingBar)v).getRating());
                return false;
            }
        }
        return true;
    }

}

