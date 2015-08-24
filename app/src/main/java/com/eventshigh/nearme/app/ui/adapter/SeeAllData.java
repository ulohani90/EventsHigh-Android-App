package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseEventsFragment;

public class SeeAllData implements AdapterData {
    public final String category;
    public final BaseEventsFragment eventsFragment;

    SeeAllData(BaseEventsFragment eventsFragment, String category) {
        this.category = category;
        this.eventsFragment = eventsFragment;
    }

    @Override
    public DataType getType() {
        return DataType.SEE_ALL;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        ((SeeAllCard) card).bindHeaderView(eventsFragment, this);
    }

    @Override
    public String getId() {
        return category;
    }
}
