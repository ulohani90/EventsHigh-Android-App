package com.eventshigh.nearme.app.utils;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.Event;

/**
 * An {@link android.widget.ListAdapter} which can be used to populate the
 * Event card.
 */
public class EventListAdapter extends ArrayAdapter<Event> {
    private final BaseActivity mBaseActivity;
    private boolean showDate = false;

    public EventListAdapter(BaseActivity activity) {
        super(activity, R.layout.event_card, R.id.event_title);
        mBaseActivity = activity;
    }

    public void setShowDate(boolean showDate) {
        this.showDate = showDate;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getView(getItem(position), mBaseActivity, convertView, parent, showDate);
    }

    // Build the view, reuse existing if possible.
    public static View getView(final Event event, final BaseActivity activity,
                               @Nullable View reuseView, ViewGroup parent, boolean showDate) {
        // Build the view, reuse existing if possible.
        final View view = reuseView == null ?
                activity.getLayoutInflater().inflate(R.layout.event_card, parent, false) :
                reuseView;
        EventCard eventCard = new EventCard(view);

        // Set the background image.
        int height = findOptimalHeight(parent);
        eventCard.bgView.setLayoutParams(new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, height));
        DownloadImageTask.setImage(eventCard.bgView, event.imgUrl, event.category.getInfographResourceId(),
                parent.getMeasuredWidth(), height);

        // Set the title, time etc.
        eventCard.titleView.setText(event.title);
        String eventTime = Utils.getEventTime(event, showDate);
        if (eventTime == null) {
            eventCard.timeView.setVisibility(View.GONE);
        } else {
            eventCard.timeView.setVisibility(View.VISIBLE);
            eventCard.timeView.setText(eventTime);
        }

        if (event.numPeopleInterested <= 0) {
            eventCard.numPeopleInterestedView.setVisibility(View.INVISIBLE);
        } else {
            eventCard.numPeopleInterestedView.setVisibility(View.VISIBLE);
            eventCard.numPeopleInterestedView.setText(
                    Integer.toString(event.numPeopleInterested));
        }

        // Show tagsWhiteList.
        showTag(eventCard.tag0View, event, 0);
        showTag(eventCard.tag1View, event, 1);
        showTag(eventCard.tag2View, event, 2);

        // Set the venue.
        if (event.venue == null) {
            eventCard.venueView.setVisibility(View.INVISIBLE);
        } else {
            eventCard.venueView.setVisibility(View.VISIBLE);
            eventCard.venueView.setText(event.venue);
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
        eventCard.timeView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.addToCalendar(event, null);
            }
        });
        eventCard.venueView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showDirections(event);
            }
        });

        return view;
    }

    public static class EventCard {
        private final ImageView bgView;
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
            bgView = (ImageView) cardView.findViewById(R.id.event_bg);
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

    // What would be optimal height for event card? Width of event card is same as
    // width of parent. In landscape mode, we want event card height to match the parent
    // height so that one event card is fully visible. In portrait mode, we want event cards
    // to be 9:16 shape.
    private static int findOptimalHeight(ViewGroup parent) {
        int parentWidth = parent.getMeasuredWidth();
        int parentHeight = parent.getMeasuredHeight();

        int cardWidth = parentWidth;
        if (parent instanceof GridView) {
            cardWidth = parentWidth / ((GridView) parent).getNumColumns();
        }

        return Math.min(9 * cardWidth / 16, parentHeight);
    }

    public static void showTag(TextView tagView, Event event, int tagNo) {
        if (tagNo >= event.tagsWhiteList.length) {
            tagView.setVisibility(View.GONE);
            return;
        }

        tagView.setVisibility(View.VISIBLE);
        tagView.setText(event.tagsWhiteList[tagNo]);
    }
}
