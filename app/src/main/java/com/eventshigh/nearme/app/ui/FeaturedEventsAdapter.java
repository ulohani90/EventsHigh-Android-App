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
import com.eventshigh.nearme.app.activity.LaunchActivity;
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
    private final LaunchActivity activity;
    private final List<Event> events;

    public FeaturedEventsAdapter(LaunchActivity activity, List<Event> events) {
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
        public final TextView numPeopleInterestedView;
        public final TextView timeView;
        public final ImageView favouriteView;
        public final ImageView favouritedView;
        public final ImageView offerMarker;

        public ExploreEventCard(View root) {
            this.root = root;
            this.imageView = (NetworkImageView) root.findViewById(R.id.event_bg);
            this.categoryView = (TextView) root.findViewById(R.id.event_category);
            this.titleView = (TextView) root.findViewById(R.id.event_title);
            this.venueView = (TextView) root.findViewById(R.id.event_venue);
            this.numPeopleInterestedView = (TextView) root.findViewById(R.id.num_people_interested);
            this.timeView = (TextView) root.findViewById(R.id.event_time);
            this.favouriteView = (ImageView) root.findViewById(R.id.action_favourite);
            this.favouritedView = (ImageView) root.findViewById(R.id.action_favourited);
            this.offerMarker = (ImageView) root.findViewById(R.id.offer_marker);
        }

        public void attachTo(final Event event, final BaseActivity activity) {
            if (event.imgUrl != null) {
                imageView.setImageUrl(event.imgUrl, VolleyHelper.getImageLoader(activity));
            }

            titleView.setText(event.title);
            venueView.setText(event.getShortAddress());

            // Category.
            String tagToShow = event.category != EventCategory.OTHER ? event.category.categoryName :
                    (event.tags.length > 0 ? event.tags[0] : null);
            if (tagToShow != null) {
                categoryView.setText(tagToShow);
            }

            // Num people interested.
            numPeopleInterestedView.setText(Integer.toString(event.numPeopleInterested));
            numPeopleInterestedView.setVisibility(
                    event.numPeopleInterested > 0 ? View.VISIBLE : View.INVISIBLE);

            // Event Time.
            EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
            if (eventTime != null) {
                timeView.setVisibility(View.VISIBLE);
                timeView.setText(eventTime.toString());
            } else {
                timeView.setVisibility(View.INVISIBLE);
            }

            // Offer ?
            offerMarker.setVisibility(event.offerTitle != null ? View.VISIBLE : View.GONE);

            // Favourite ?
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
