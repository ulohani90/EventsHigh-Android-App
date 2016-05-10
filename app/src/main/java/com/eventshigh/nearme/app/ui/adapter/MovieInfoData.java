package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MovieInfoObject;

/**
 * Created by umesh on 06/05/16.
 */
public class MovieInfoData implements AdapterData {

  MovieInfoObject obj;

    BaseActivity activity;

    public MovieInfoData(MovieInfoObject obj, BaseActivity activity) {
        this.obj = obj;
        this.activity = activity;
    }


    @Override
    public DataType getType() {
        return DataType.MOVIE_INFO;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((MovieInfoCard) card).bindData(obj, activity);
    }

    @Override
    public String getId() {
        return obj.getId() + "";
    }
}
