package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;

public class FollowData implements AdapterData {
    public final String title;
    public final int numEvents;
    public final int numFollowers;
    public final BaseContextActivity activity;
    public final SocialDataProvider socialDataProvider;


    public FollowData(String title, int numEvents, int numFollowers, BaseContextActivity activity,
                      SocialDataProvider socialDataProvider) {
        this.title = title;
        this.numEvents = numEvents;
        this.numFollowers = numFollowers;
        this.activity = activity;
        this.socialDataProvider = socialDataProvider;

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
