package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Locality;

/**
 * Created by umesh on 11/03/16.
 */
public class LocalityData implements AdapterData{

    public final Locality locality;
    public final BaseContextActivity activity;
    public final int color;
    @Override
    public DataType getType() {
        return DataType.EXPLORE_LOCALITY;
    }

    public  LocalityData(Locality locality,BaseContextActivity activity,int color){
        this.locality = locality;
        this.activity = activity;
        this.color = color;

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((LocalityCard) card).populateTrendingCategoryData(activity,this,color);
    }

    @Override
    public String getId() {
        return locality.name;
    }
}
