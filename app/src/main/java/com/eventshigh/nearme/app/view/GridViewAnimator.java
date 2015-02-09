package com.eventshigh.nearme.app.view;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.ui.EventsAdapter;

public class GridViewAnimator extends DefaultItemAnimator {
    private final EventsAdapter eventsAdapter;

    public GridViewAnimator(EventsAdapter eventsAdapter) {
        this.eventsAdapter = eventsAdapter;
    }

    @Override
    public boolean animateAdd (ViewHolder holder) {
        eventsAdapter.onResumeAnimation();
        return super.animateAdd(holder);
    }

    @Override
    public boolean animateRemove (ViewHolder holder) {
        eventsAdapter.onResumeAnimation();
        return super.animateRemove(holder);
    }
}
