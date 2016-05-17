package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.MovieReviewObject;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;

/**
 * @author shubham
 * @since 16/5/16.
 */

public class MovieUserReviewData implements AdapterData{

    MovieUserReviewObject reviewObj;
    BaseContextActivity activity;

    public MovieUserReviewData(MovieUserReviewObject reviewObj,BaseContextActivity activity){
        this.reviewObj = reviewObj;
        this.activity = activity;
    }

    @Override
    public DataType getType() {
        return DataType.MOVIE_REVIEW;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((MovieUserReviewCard)card).bindData(activity, reviewObj);
    }

    @Override
    public String getId() {
        return reviewObj.getId()+"";
    }
}
