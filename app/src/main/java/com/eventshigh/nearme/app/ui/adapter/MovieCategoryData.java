package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;

/**
 * Created by umesh on 14/05/16.
 */
public class MovieCategoryData implements AdapterData {

    public final String tag;
    public final BaseContextActivity activity;


    public MovieCategoryData(String tag, BaseContextActivity activity) {
        this.tag = tag;
        this.activity = activity;

    }

    @Override
    public DataType getType() {
        return DataType.MOVIE_CATEGORY;
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, final int position) {
        ((TrendingCategoryCard) card).populateMovieCategoryData(this);
    }

    public String getId() {
        return tag;
    }
}
