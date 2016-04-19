package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.stream.PointsObject;

/**
 * Created by umesh on 16/04/16.
 */
public class PointsData implements AdapterData{

    PointsObject point;
    BaseContextActivity activity;
    @Override
    public DataType getType() {
        return DataType.POINTS;
    }

    public PointsData (PointsObject point,BaseContextActivity activity){
        this.point = point;
        this.activity = activity;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((PointsCard)card).bindView(point,activity);
    }

    @Override
    public String getId() {
        return point.pName;
    }
}
