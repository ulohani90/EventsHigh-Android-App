package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MovieReviewObject;

/**
 * Created by umesh on 05/05/16.
 */
public class MovieReviewData implements AdapterData{

    MovieReviewObject reviewObj;
    BaseContextActivity activity;

    public MovieReviewData(MovieReviewObject reviewObj,BaseContextActivity activity){
        this.reviewObj = reviewObj;
        this.activity = activity;
    }

    @Override
    public DataType getType() {
        return DataType.MOVIE_REVIEW;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((MovieReviewCard)card).bindData(activity, reviewObj);
    }

    @Override
    public String getId() {
        return reviewObj.getId()+"";
    }
}
