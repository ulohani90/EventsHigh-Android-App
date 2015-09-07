package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseContextActivity;

public class SeeAllData implements AdapterData {
    public final String category;
    public final BaseContextActivity activity;

    SeeAllData(BaseContextActivity activity, String category) {
        this.category = category;
        this.activity = activity;
    }

    @Override
    public DataType getType() {
        return DataType.SEE_ALL;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        ((SeeAllCard) card).bindHeaderView(activity, this);
    }

    @Override
    public String getId() {
        return category;
    }
}
