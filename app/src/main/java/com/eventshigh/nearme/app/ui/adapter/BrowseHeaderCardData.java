package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseActivity;

/**
 * Created by umesh on 09/11/17.
 */

public class BrowseHeaderCardData implements AdapterData {

    BaseActivity activity;
    String text;

    public BrowseHeaderCardData(BaseActivity activity, String text) {
        this.activity = activity;
        this.text = text;
    }


    @Override
    public DataType getType() {
        return DataType.BROWSE_HEADER_CARD;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((BrowseHeaderCardLayout) card).populate(activity, text);
    }

    @Override
    public String getId() {
        return text;
    }
}
