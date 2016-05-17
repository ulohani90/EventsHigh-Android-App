package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.MovieDetailObject;

/**
 * Created by umesh on 09/05/16.
 */
public class MovieListData implements AdapterData {
    MovieDetailObject movie;
    BaseContextActivity activity;

    @Override
    public DataType getType() {
        return DataType.MOVIE_LIST_CARD;
    }

    public MovieListData(MovieDetailObject movie, BaseContextActivity activity) {
        this.movie = movie;
        this.activity = activity;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((MovieListCard) card).bindData(movie, activity);
    }

    @Override
    public String getId() {
        return movie.getMovieInfo().getId() + "";
    }
}
