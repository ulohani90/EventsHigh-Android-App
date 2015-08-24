package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseEventsFragment;
import com.eventshigh.nearme.app.network.MyEventsRequest;

public class HeaderData implements AdapterData {
    public final String header;
    public final int numEvents;
    public final BaseEventsFragment eventsFragment;

    public HeaderData(BaseEventsFragment eventsFragment, String header, int numEvents) {
        this.header = header;
        this.numEvents = numEvents;
        this.eventsFragment = eventsFragment;
    }

    public boolean showMore() {
        return numEvents > 0 && !MyEventsRequest.isSpecialTag(header);
    }

    @Override
    public DataType getType() {
        return DataType.HEADER;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        ((HeaderCard) card).bindHeaderView(eventsFragment, this);
    }

    @Override
    public String getId() {
        return header;
    }
}
