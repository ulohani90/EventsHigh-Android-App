package com.eventshigh.nearme.app.ui;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;

import java.util.List;

/**
* A {@link android.support.v4.view.PagerAdapter} which can be used to show Featured Events.
*/
public class FeaturedEventsAdapter extends PagerAdapter {
    private final BaseContextActivity activity;
    private final List<Event> events;

    public FeaturedEventsAdapter(BaseContextActivity activity, List<Event> events) {
        this.activity = activity;
        this.events = events;
    }

    @Override
    public int getCount() {
        return events.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View view = EventsAdapter.getEventCard(events.get(position), activity, null, container);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}
