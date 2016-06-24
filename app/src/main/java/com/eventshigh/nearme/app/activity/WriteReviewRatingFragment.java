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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;


public class WriteReviewRatingFragment extends Fragment {


    TextView tvMovieName;
    RatingBar rbMovieRating;
    ImageView ivMoviePicture;

    WriteReviewActivity writeReviewActivity;

    TextView ratingHeaderText;

    public static WriteReviewRatingFragment newInstance(WriteReviewActivity writeReviewActivity) {
        return new WriteReviewRatingFragment();
    }

    @Override
    public void onAttach(Context context) {
        writeReviewActivity = (WriteReviewActivity) context;
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_write_review_rating, container, false);
        tvMovieName = (TextView) rootView.findViewById(R.id.tv_write_review_movie_name);
        rbMovieRating = (RatingBar) rootView.findViewById(R.id.rb_write_rating);
        rbMovieRating.setOnTouchListener(writeReviewActivity);
        ivMoviePicture = (ImageView) rootView.findViewById(R.id.iv_write_review_movie_pic);
        ratingHeaderText = (TextView) rootView.findViewById(R.id.rate_header);

        if (writeReviewActivity.isFromNotification){
                tvMovieName.setText(writeReviewActivity.reviewEntityName);
                Glide.with(this).load(writeReviewActivity.reviewEntityImage)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                            .into(ivMoviePicture);
                ratingHeaderText.setText("Please rate the Movie!");
        }else{
            if (writeReviewActivity.type.equals("movie")) {

                if (writeReviewActivity.movieDetailObject != null) {
                    tvMovieName.setText(writeReviewActivity.movieDetailObject.getMovieInfo().getName());
                    Glide.with(this).load(writeReviewActivity.movieDetailObject.getMovieInfo().getImg_url())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                            .into(ivMoviePicture);
                }
                ratingHeaderText.setText("Please rate the Movie!");
            } else {
                tvMovieName.setText(writeReviewActivity.event.title);
                Glide.with(this).load(writeReviewActivity.event.imgUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                        .into(ivMoviePicture);
                ratingHeaderText.setText("Please rate the Event!");
            }
        }

        return rootView;
    }
}
