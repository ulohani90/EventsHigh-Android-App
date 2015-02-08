package com.eventshigh.nearme.app.view;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.ui.EventsAdapter;

public class GridViewAnimator extends DefaultItemAnimator {
    private final EventsAdapter eventsAdapter;

    public GridViewAnimator(EventsAdapter eventsAdapter) {
        this.eventsAdapter = eventsAdapter;
    }

    public boolean animateRemove (RecyclerView.ViewHolder holder) {
        eventsAdapter.onRemove();
        return super.animateRemove(holder);
    }
}
