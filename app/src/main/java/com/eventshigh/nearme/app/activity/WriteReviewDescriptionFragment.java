package com.eventshigh.nearme.app.activity;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.view.CircularImageView;

public class WriteReviewDescriptionFragment extends Fragment {

    static String RATING_COUNT = "rating_count";

    AppCompatActivity mAppCompatActivity;
    EditText etWriteReviewTitle,etWriteReviewDescription;
    Button btnReviewSubmit;
    CircularImageView ivMoviePicture;
    TextView tvMovieName;
    RatingBar rbMovieRating;


    public static WriteReviewDescriptionFragment newInstance(AppCompatActivity appCompatActivity){
        return new WriteReviewDescriptionFragment();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_write_review_description, container, false);

        ivMoviePicture = (CircularImageView)rootView.findViewById(R.id.civ_movie_pic);
        tvMovieName = (TextView)rootView.findViewById(R.id.tv_write_review_description_movie_name);
        rbMovieRating = (RatingBar)rootView.findViewById(R.id.rb_write_rating_description);


        etWriteReviewTitle = (EditText)rootView.findViewById(R.id.et_write_review_title);
        etWriteReviewDescription = (EditText)rootView.findViewById(R.id.et_write_review_description);

        btnReviewSubmit = (Button)rootView.findViewById(R.id.btn_write_review);

        return rootView;
    }


}
