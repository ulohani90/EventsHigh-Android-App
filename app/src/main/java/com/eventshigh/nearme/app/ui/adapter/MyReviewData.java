package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;

/**
 * @author shubham
 * @since 27/6/16.
 */
public class MyReviewData implements AdapterData{

    MovieUserReviewObject movieUserReviewObject;
    BaseContextActivity baseContextActivity;

    public MyReviewData(MovieUserReviewObject movieUserReviewObject, BaseContextActivity baseContextActivity){
        this.movieUserReviewObject = movieUserReviewObject;
        this.baseContextActivity = baseContextActivity;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((MyReviewCard)card).bindData(movieUserReviewObject);
    }

    @Override
    public String getId() {
        return movieUserReviewObject.getReviewId();
    }

    @Override
    public DataType getType() {
        return DataType.MY_REVIEW_CARD;
    }
}
