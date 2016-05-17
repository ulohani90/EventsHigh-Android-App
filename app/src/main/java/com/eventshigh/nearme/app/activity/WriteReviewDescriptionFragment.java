package com.eventshigh.nearme.app.activity;


import android.content.Context;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.CircularImageView;

public class WriteReviewDescriptionFragment extends Fragment implements View.OnClickListener{

    static String RATING_COUNT = "rating_count";

    AppCompatActivity mAppCompatActivity;
    EditText etWriteReviewTitle,etWriteReviewDescription;
    Button btnReviewSubmit;
    CircularImageView ivMoviePicture;
    TextView tvMovieName;
    RatingBar rbMovieRating;
    WriteReviewActivity writeReviewActivity;

    public static WriteReviewDescriptionFragment newInstance(AppCompatActivity appCompatActivity){
        return new WriteReviewDescriptionFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        writeReviewActivity = (WriteReviewActivity)context;
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
        if(writeReviewActivity.movieDetailObject != null){
            Glide.with(writeReviewActivity).load(writeReviewActivity.movieDetailObject.getMovieInfo().getImg_url())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                    .into(ivMoviePicture);
            tvMovieName.setText(writeReviewActivity.movieDetailObject.getMovieInfo().getName());
        }
        writeReviewActivity.movie_rated =
                (int)writeReviewActivity.writeReviewRatingFragment.rbMovieRating.getRating();
        rbMovieRating.setRating(writeReviewActivity.movie_rated);
        btnReviewSubmit.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.btn_write_review:
                MovieUserReviewObject movieUserReviewObject = new MovieUserReviewObject();
                movieUserReviewObject.setReviewTitle(etWriteReviewTitle.getText().toString());
                movieUserReviewObject.setReviewText(etWriteReviewDescription.getText().toString());
                movieUserReviewObject.setReviewRating(writeReviewActivity.movie_rated);
                movieUserReviewObject.setReviewPlatform("Android");
                movieUserReviewObject.setReviewDeviceId(Utils.getAndroidId(writeReviewActivity));
                movieUserReviewObject.setReviewFor(writeReviewActivity.movieDetailObject.getMovieInfo().getName());

                break;
            case R.id.rb_write_rating_description:
                writeReviewActivity.onBackPressed();
                break;
        }
    }
}
