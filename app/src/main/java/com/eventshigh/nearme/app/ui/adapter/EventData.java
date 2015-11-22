package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;

public class EventData implements AdapterData {
    private final String header;
    private final Event event;
    private final boolean isFirstEvent;
    private BaseContextActivity activity;

    public EventData(String header, Event event, boolean isFirstEvent,
                     BaseContextActivity activity) {
        this.header = header;
        this.event = event;
        this.isFirstEvent = isFirstEvent;
        this.activity = activity;
    }

    @Override
    public DataType getType() {
        return DataType.EVENT;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        ((EventCard) card).bindEventView(event, isFirstEvent, position, activity);
    }

    public String getId() {
        return header + ":" + event.id;
    }
}
