package com.eventshigh.nearme.app.ui;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseEventsActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.Utils;

/**
 * An {@link android.widget.ListAdapter} which can be used to populate the Event card.
 */
public class EventsAdapter extends ArrayAdapter<Event> {
    private final BaseEventsActivity activity;

    public EventsAdapter(BaseEventsActivity activity) {
        super(activity, R.layout.event_card, R.id.event_title);
        this.activity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getView(getItem(position), activity, convertView, parent);
    }

    // Build the view, reuse existing if possible.
    public static View getView(final Event event, final BaseEventsActivity activity,
                               @Nullable View reuseView, ViewGroup parent) {
        // Build the view, reuse existing if possible.
        final View view = reuseView == null ?
                activity.getLayoutInflater().inflate(R.layout.event_card, parent, false) :
                reuseView;
        final EventCard eventCard = new EventCard(view);

        // Set the background image.
        eventCard.bgView.setVisibility(View.VISIBLE);
        Utils.waitForViewVisible(eventCard.bgView, new Runnable() {
            @Override
            public void run() {
                RelativeLayout.LayoutParams params =
                        (RelativeLayout.LayoutParams) eventCard.bgView.getLayoutParams();
                params.width = eventCard.bgView.getHeight();
                eventCard.bgView.setLayoutParams(params);
                if (event.imgUrl != null) {
                    eventCard.bgView.setImageUrl(event.imgUrl,
                            VolleyHelper.getImageLoader(activity.getApplicationContext()));
                } else {
                    eventCard.bgView.setVisibility(View.INVISIBLE);
                    eventCard.bgView.setImageBitmap(null);
                }
            }
        }, 100);

        // Set the title, time etc.
        eventCard.titleView.setText(event.title);
        EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
        if (eventTime == null) {
            eventCard.timeView.setVisibility(View.GONE);
        } else {
            eventCard.timeView.setVisibility(View.VISIBLE);
            eventCard.timeView.setText(eventTime.toString());
        }

        if (event.numPeopleInterested <= 0) {
            eventCard.numPeopleInterestedView.setVisibility(View.GONE);
        } else {
            eventCard.numPeopleInterestedView.setVisibility(View.VISIBLE);
            eventCard.numPeopleInterestedView.setText(
                    Integer.toString(event.numPeopleInterested));
        }

        // Set the venue.
        eventCard.venueView.setText(Utils.capitalize(
                event.venue == null ? event.city.toString() : event.venue));

        // Check if its recommended event.
        eventCard.recommendedImageView.setVisibility(event.ehRecommended ? View.VISIBLE :
                View.INVISIBLE);

        return view;
    }

    public static class EventCard {
        private final NetworkImageView bgView;
        private final ImageView recommendedImageView;
        private final TextView titleView;
        private final TextView venueView;
        private final TextView timeView;
        private final TextView numPeopleInterestedView;

        private EventCard(View cardView) {
            bgView = (NetworkImageView) cardView.findViewById(R.id.event_bg);
            recommendedImageView = (ImageView) cardView.findViewById(R.id.event_recommended);
            titleView = (TextView) cardView.findViewById(R.id.event_title);
            venueView = (TextView) cardView.findViewById(R.id.event_venue);
            timeView = (TextView) cardView.findViewById(R.id.event_time);
            numPeopleInterestedView = (TextView) cardView.findViewById(R.id.num_people_interested);
        }
    }
}
