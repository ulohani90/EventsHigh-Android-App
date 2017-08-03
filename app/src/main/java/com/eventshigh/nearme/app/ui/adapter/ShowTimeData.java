package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.MovieShowTimeObject;

/**
 * Created by umesh on 05/05/16.
 */
public class ShowTimeData implements AdapterData{

    MovieShowTimeObject showTime;
    BaseActivity activity;
    @Override
    public DataType getType() {
        return DataType.SHOWTIME;
    }

    public  ShowTimeData(MovieShowTimeObject showTime,BaseActivity activity){
        this.showTime = showTime;
        this.activity = activity;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((ShowTimeCard)card).bindData(showTime,activity);
    }

    @Override
    public String getId() {
        return showTime.getVenueName();
    }
}
