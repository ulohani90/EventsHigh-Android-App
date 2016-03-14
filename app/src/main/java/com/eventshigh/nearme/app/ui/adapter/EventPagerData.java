package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;

import java.util.List;

public class EventPagerData implements AdapterData {
    public final List<Event> events;
    public final boolean showReferralOffer;
    public final BaseContextActivity activity;

    EventPagerData(BaseContextActivity activity, boolean showReferralOffer, List<Event> events) {
        this.events = events;
        this.showReferralOffer = showReferralOffer;
        this.activity = activity;
    }

    @Override
    public DataType getType() {
        return DataType.EVENT_PAGER;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        ((EventPagerCard) card).bindHeaderView(this);
    }

    @Override
    public String getId() {
        return events.get(0).id;
    }
}
