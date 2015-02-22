package com.eventshigh.nearme.app.ui;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.List;

/**
* A {@link android.support.v4.view.PagerAdapter} which can be used to show Featured Events.
*/
public class FeaturedEventsAdapter extends PagerAdapter {
    private final BaseActivity activity;
    private final List<Event> events;

    public FeaturedEventsAdapter(BaseActivity activity, List<Event> events) {
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
        View eventCard = activity.getLayoutInflater().inflate(
                R.layout.explore_event_card, container, false);
        final Event event = events.get(position);

        NetworkImageView imageView = (NetworkImageView) eventCard.findViewById(R.id.event_bg);
        if (event.imgUrl != null) {
            imageView.setImageUrl(event.imgUrl, VolleyHelper.getImageLoader(activity));
        } else {
            imageView.setImageBitmap(null);
        }

        ((TextView)eventCard.findViewById(R.id.event_title)).setText(event.title);
        ((TextView)eventCard.findViewById(R.id.event_venue)).setText(Utils.capitalize(
                event.venue == null ? event.city.toString() : event.venue));

        EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
        if (eventTime != null) {
            ((TextView)eventCard.findViewById(R.id.event_date)).setText(eventTime.day + ", " + eventTime.date);
            if (eventTime.time != null) {
                ((TextView) eventCard.findViewById(R.id.event_time)).setText(eventTime.time);
            }
        }

        eventCard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportEventAction(event, "featuredClick");
                activity.showEventDetails(event);
            }
        });

        container.addView(eventCard);
        return eventCard;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}
