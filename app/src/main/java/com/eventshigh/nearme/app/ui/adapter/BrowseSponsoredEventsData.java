package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.SponsoredEventObj;

import java.util.List;

/**
 * Created by umesh on 10/11/17.
 */

public class BrowseSponsoredEventsData implements AdapterData {

    List<SponsoredEventObj> events;
    BaseContextActivity activity;
    int width;

    public BrowseSponsoredEventsData(BaseContextActivity activity, List<SponsoredEventObj> events, int width) {
        this.activity = activity;
        this.events = events;
        this.width = width;
    }

    @Override
    public DataType getType() {
        return DataType.BROWSE_SPONSORED_EVENTS;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((BrowseSponsoredEventsCard) card).populate(activity, events, width);
    }

    @Override
    public String getId() {
        return "sponsored_events";
    }
}
