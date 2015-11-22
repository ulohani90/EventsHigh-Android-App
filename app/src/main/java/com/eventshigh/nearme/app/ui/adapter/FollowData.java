package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseContextActivity;

public class FollowData implements AdapterData {
    public final String title;
    public final int numEvents;
    public final int numFollowers;
    public final BaseContextActivity activity;

    public FollowData(String title, int numEvents, int numFollowers, BaseContextActivity activity) {
        this.title = title;
        this.numEvents = numEvents;
        this.numFollowers = numFollowers;
        this.activity = activity;
    }

    @Override
    public DataType getType() {
        return DataType.FOLLOW;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        ((FollowCard) card).populate(this);
    }

    @Override
    public String getId() {
        return title;
    }
}
