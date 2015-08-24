package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseEventsFragment;
import com.eventshigh.nearme.app.data.Event;

public class EventData implements AdapterData {
    private final String header;
    private final Event event;
    private final boolean isFirstEvent;
    private BaseEventsFragment eventsFragment;
    private final SocialDataProvider socialDataProvider;

    public EventData(String header, Event event, boolean isFirstEvent,
                     BaseEventsFragment eventsFragment, SocialDataProvider socialDataProvider) {
        this.header = header;
        this.event = event;
        this.isFirstEvent = isFirstEvent;
        this.eventsFragment = eventsFragment;
        this.socialDataProvider = socialDataProvider;
    }

    @Override
    public DataType getType() {
        return DataType.EVENT;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        ((EventCard) card).bindEventView(event, isFirstEvent, position, eventsFragment,
                socialDataProvider.getSocialInvite(event.id));
    }

    public String getId() {
        return header + ":" + event.id;
    }
}
