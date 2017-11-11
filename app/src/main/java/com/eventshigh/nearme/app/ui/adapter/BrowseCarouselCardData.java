package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;

import java.util.List;

/**
 * Created by umesh on 09/11/17.
 */

public class BrowseCarouselCardData implements AdapterData {

    List<Event> events;
    BaseContextActivity activity;

    public BrowseCarouselCardData(BaseContextActivity activity, List<Event> events) {
        this.activity = activity;
        this.events = events;
    }

    @Override
    public DataType getType() {
        return DataType.BROWSE_CAROUSEL_CARD;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((BrowseCarouselCard) card).populate(activity, events);
    }

    @Override
    public String getId() {
        return "carousel_card";
    }
}
