package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseContextActivity;

public class SmallHeaderData implements AdapterData {
    public final String header;

    public final boolean isEditAllowed;

    public final int drawableLeftResourceId;

    private BaseContextActivity activity;

    public SmallHeaderData(String header) {
        this.header = header;
        isEditAllowed =false;
        drawableLeftResourceId = 0;
    }
    public SmallHeaderData(BaseContextActivity activity,String header,boolean isEditAllowed,int drawableLeftResourceId) {
        this.header = header;
        this.isEditAllowed = isEditAllowed;
        this.activity = activity;
        this.drawableLeftResourceId = drawableLeftResourceId;
    }

    @Override
    public DataType getType() {
        return DataType.SMALL_HEADER;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {

    }


    public void onBindViewHolder(ViewHolder card, int position,EventsAdapter.OnEditClickListener listener) {
        ((SmallHeaderCard) card).bindHeaderView(activity,this,listener);
    }

    @Override
    public String getId() {
        return header;
    }
}
