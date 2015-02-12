package com.eventshigh.nearme.app.ui;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class EventsAdapterWrapper extends RecyclerView.Adapter<EventsAdapter.EventCard> {
    private final EventsAdapter eventsAdapter;

    public EventsAdapterWrapper(EventsAdapter eventsAdapter) {
        this.eventsAdapter = eventsAdapter;
    }

    @Override
    public EventsAdapter.EventCard onCreateViewHolder(ViewGroup parent, int viewType) {
        return eventsAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(EventsAdapter.EventCard holder, int position) {
        if (position < eventsAdapter.getItemCount()) {
            eventsAdapter.onBindViewHolder(holder, position);
        } else {
            holder.cardView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return eventsAdapter.getItemCount() + 1;
    }
}
