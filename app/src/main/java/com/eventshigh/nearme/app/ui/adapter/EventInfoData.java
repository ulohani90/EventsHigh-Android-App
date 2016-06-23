package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;

/**
 * Created by umesh on 20/06/16.
 */
public class EventInfoData implements AdapterData {

    Event event;

    BaseContextActivity activity;

    public EventInfoData(Event event, BaseContextActivity activity) {
        this.event = event;
        this.activity = activity;
    }

    @Override
    public DataType getType() {
        return DataType.EVENT_INFO_CARD;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((EventInfoCard) card).onBindData(event, activity);
    }

    @Override
    public String getId() {
        return event.id;
    }
}
