package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;


public class WriteReviewRatingFragment extends Fragment{


    ImageView ivMoviePicture;
    TextView tvMovieName;
    RatingBar rbMovieRating;

    // Container Activity must implement this interface
    public interface OnMovieRatedListener {
        public void onMovieRated(int rating);
    }

    WriteReviewActivity writeReviewActivity;
    OnMovieRatedListener mCallback;

    public static WriteReviewRatingFragment newInstance(WriteReviewActivity writeReviewActivity){
        return new WriteReviewRatingFragment();
    }

    @Override
    public void onAttach(Context context) {
        writeReviewActivity = (WriteReviewActivity)context;
        super.onAttach(context);
        try {
            mCallback = (OnMovieRatedListener) writeReviewActivity;
        } catch (ClassCastException e) {
            throw new ClassCastException(writeReviewActivity.toString()
                    + " must implement OnMovieRatedListener");
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_write_review_rating, container, false);
        ivMoviePicture = (ImageView)rootView.findViewById(R.id.iv_write_review_movie_pic);
        tvMovieName = (TextView)rootView.findViewById(R.id.tv_write_review_description_movie_name);
        rbMovieRating = (RatingBar)rootView.findViewById(R.id.rb_write_rating);
        rbMovieRating.setOnTouchListener(writeReviewActivity);

        return rootView;
    }

}
