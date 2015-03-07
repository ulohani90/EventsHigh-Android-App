package com.eventshigh.nearme.app.ui;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;

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
        new ExploreEventCard(eventCard).attachTo(event, activity);
        container.addView(eventCard);
        return eventCard;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    private static class ExploreEventCard {
        public final View root;
        public final NetworkImageView imageView;
        public final TextView categoryView;
        public final TextView titleView;
        public final TextView venueView;
        public final TextView dateView;
        public final TextView timeView;
        public final ImageView favouriteView;
        public final ImageView favouritedView;

        public ExploreEventCard(View root) {
            this.root = root;
            this.imageView = (NetworkImageView) root.findViewById(R.id.event_bg);
            this.categoryView = (TextView) root.findViewById(R.id.event_category);
            this.titleView = (TextView) root.findViewById(R.id.event_title);
            this.venueView = (TextView) root.findViewById(R.id.event_venue);
            this.dateView = (TextView) root.findViewById(R.id.event_date);
            this.timeView = (TextView) root.findViewById(R.id.event_time);
            this.favouriteView = (ImageView) root.findViewById(R.id.action_favourite);
            this.favouritedView = (ImageView) root.findViewById(R.id.action_favourited);
        }

        public void attachTo(final Event event, final BaseActivity activity) {
            if (event.imgUrl != null) {
                imageView.setImageUrl(event.imgUrl, VolleyHelper.getImageLoader(activity));
            }

            titleView.setText(event.title);
            venueView.setText(event.getShortAddress());

            String tagToShow = null;
            for (String tag : event.tags) {
                if (EventCategory.parseCategory(tag) != null) {
                    tagToShow = tag;
                    break;
                }
            }
            if (tagToShow == null && event.tags.length > 0) {
                tagToShow = event.tags[0];
            }
            if (tagToShow != null) {
                categoryView.setText(tagToShow);
            }

            EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
            if (eventTime != null) {
                dateView.setText(eventTime.day + ", " + eventTime.date);
                if (eventTime.time != null) {
                    timeView.setText(eventTime.time);
                }
            }

            final EventsMarkerManager eventsMarkerManager = EventsMarkerManager.getInstance(activity);
            setFavouriteView(eventsMarkerManager.isFavourite(event.id));
            favouriteView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportEventAction(event, "addFavourite");
                    eventsMarkerManager.getEditor().recordEventMark(event, EventMark.FAVOURITE).close();
                    setFavouriteView(true);
                }
            });
            favouritedView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportEventAction(event, "removeFavourite");
                    eventsMarkerManager.getEditor().removeEventMark(event).close();
                    setFavouriteView(false);
                }
            });

            root.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportEventAction(event, "featuredClick");
                    activity.showEventDetails(event);
                }
            });
        }

        public void setFavouriteView(boolean isFavourite) {
            favouriteView.setVisibility(isFavourite ? View.GONE : View.VISIBLE);
            favouritedView.setVisibility(isFavourite ? View.VISIBLE : View.GONE);
        }
    }
}
