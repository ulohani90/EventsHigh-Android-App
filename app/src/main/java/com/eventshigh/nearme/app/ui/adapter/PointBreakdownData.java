package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.network.MyPointsBreakdownRequest.PointBreakDown;

/**
 * Created by umesh on 22/04/16.
 */
public class PointBreakdownData implements AdapterData{

    PointBreakDown obj;
    BaseContextActivity activity;

    public PointBreakdownData(PointBreakDown obj,BaseContextActivity activity){
        this.obj = obj;
        this.activity = activity;
    }

    @Override
    public DataType getType() {
        return DataType.POINT_BREAKDWON;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((PointBreakdownCard)card).bindView(obj,activity);
    }

    @Override
    public String getId() {
        return obj.message;
    }
}
