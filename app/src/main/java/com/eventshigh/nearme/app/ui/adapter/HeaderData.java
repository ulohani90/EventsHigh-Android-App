package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.MyEventsRequest;

public class HeaderData implements AdapterData {
    public static final String TYPE_EVENT = "event";
    public static final String TYPE_MOVIE = "movie";
    public final String header;
    public final int numEvents;
    public final BaseContextActivity activity;
    public final EventsContext eventsContext;
    public final String objType;

    public HeaderData(BaseContextActivity activity, EventsContext eventsContext, String header, int numEvents, String objType) {
        this.header = header;
        this.numEvents = numEvents;
        this.activity = activity;
        this.eventsContext = eventsContext;
        this.objType = objType;
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
        ((HeaderCard) card).bindHeaderView(this);
    }

    @Override
    public String getId() {
        return header;
    }
}
