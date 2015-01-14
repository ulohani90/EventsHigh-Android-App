package com.eventshigh.nearme.app.ui;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.network.Helper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.Utils;

/**
 * An {@link android.widget.ListAdapter} which can be used to populate the
 * Event card.
 */
public class EventsAdapter extends ArrayAdapter<Event> {
    private final BaseActivity activity;

    public EventsAdapter(BaseActivity activity) {
        super(activity, R.layout.event_card, R.id.event_title);
        this.activity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getView(getItem(position), activity, convertView, parent);
    }

    // Build the view, reuse existing if possible.
    public static View getView(final Event event, final BaseActivity activity,
                               @Nullable View reuseView, ViewGroup parent) {
        // Build the view, reuse existing if possible.
        final View view = reuseView == null ?
                activity.getLayoutInflater().inflate(R.layout.event_card, parent, false) :
                reuseView;
        EventCard eventCard = new EventCard(view);

        // Set the background image.
        if (event.imgUrl != null) {
            eventCard.bgView.setImageUrl(event.imgUrl,
                    Helper.getImageLoader(activity.getApplicationContext()));
        }

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

        // Show tagsWhiteList.
        showTag(eventCard.tag0View, event, 0, activity);
        showTag(eventCard.tag1View, event, 1, activity);
        showTag(eventCard.tag2View, event, 2, activity);

        // Set the venue.
        if (event.venue == null) {
            eventCard.venueView.setVisibility(View.INVISIBLE);
        } else {
            eventCard.venueView.setVisibility(View.VISIBLE);
            eventCard.venueView.setText(Utils.capitalize(event.venue));
        }

        // Check if its recommended event.
        eventCard.recommendedImageView.setVisibility(event.ehRecommended ? View.VISIBLE :
                View.INVISIBLE);

        // Set Callbacks.
        eventCard.shareView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                View shareView = view.findViewById(R.id.event_share);
                shareView.setVisibility(View.INVISIBLE);
                activity.shareEvent(view, event);
                shareView.setVisibility(View.VISIBLE);
            }
        });

        return view;
    }

    public static class EventCard {
        private final NetworkImageView bgView;
        private final ImageView shareView;
        private final ImageView recommendedImageView;
        private final TextView titleView;
        private final TextView venueView;
        private final TextView timeView;
        private final TextView numPeopleInterestedView;
        private final TextView tag0View;
        private final TextView tag1View;
        private final TextView tag2View;

        private EventCard(View cardView) {
            bgView = (NetworkImageView) cardView.findViewById(R.id.event_bg);
            shareView = (ImageView) cardView.findViewById(R.id.event_share);
            recommendedImageView = (ImageView) cardView.findViewById(R.id.event_recommended);
            titleView = (TextView) cardView.findViewById(R.id.event_title);
            venueView = (TextView) cardView.findViewById(R.id.event_venue);
            timeView = (TextView) cardView.findViewById(R.id.event_time);
            numPeopleInterestedView = (TextView) cardView.findViewById(R.id.num_people_interested);
            tag0View = (TextView) cardView.findViewById(R.id.event_tag0);
            tag1View = (TextView) cardView.findViewById(R.id.event_tag1);
            tag2View = (TextView) cardView.findViewById(R.id.event_tag2);
        }
    }

    private static void showTag(TextView tagView, Event event, int tagNo,
                                final BaseActivity activity) {
        if (tagNo >= event.tagsWhiteList.length) {
            tagView.setVisibility(View.GONE);
            return;
        }

        final String tagText = event.tagsWhiteList[tagNo];
        tagView.setVisibility(View.VISIBLE);
        tagView.setText(tagText);
        tagView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showSearchView(tagText.toLowerCase());
            }
        });
    }
}
