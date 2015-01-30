package com.eventshigh.nearme.app.ui;

import android.support.annotation.Nullable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseEventsActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.Editor;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * An {@link android.widget.ListAdapter} which can be used to populate the Event card.
 */
public class EventsAdapter extends ArrayAdapter<Event> {

    private final BaseEventsActivity activity;
    private final Editor eventsMarkerEditor;
    private final Map<String, Integer> eventIdToItemIdMap = new HashMap<>();
    private final Set<Integer> usedItemIds = new HashSet<>();

    public EventsAdapter(BaseEventsActivity activity, Editor eventsMarkerEditor) {
        super(activity, R.layout.event_card, R.id.event_title);

        this.activity = activity;
        this.eventsMarkerEditor = eventsMarkerEditor;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getView(getItem(position), activity, convertView, parent, eventsMarkerEditor);
    }

    @Override
    public void addAll(Collection<? extends Event> collection) {
        EventsMarkerManager.getInstance(activity).filterDismissed(collection);
        super.addAll(collection);
    }

    public boolean hasStableIds() {
        return true;
    }

    public long getItemId(int position) {
        return getItemId(getItem(position).id);
    }

    private int getItemId(String eventId) {
        Integer itemID = eventIdToItemIdMap.get(eventId);
        if (itemID != null) {
            return itemID;
        }

        for (int id = eventId.hashCode(); ; id++) {
            if (usedItemIds.add(id)) {
                eventIdToItemIdMap.put(eventId, id);
                return id;
            }
        }
    }

    // Build the view, reuse existing if possible.
    public static View getView(final Event event, final BaseEventsActivity activity,
                               @Nullable View reuseView, ViewGroup parent, final Editor eventsMarkerEditor) {
        // Build the view, reuse existing if possible.
        final View view = reuseView == null ?
                activity.getLayoutInflater().inflate(R.layout.event_card, parent, false) :
                reuseView;
        final EventCard eventCard = new EventCard(view);

        // Set the background image.
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

        // Check if its a recommended event or not.
        eventCard.recommendedImageView.setVisibility(event.ehRecommended ? View.VISIBLE :
                View.INVISIBLE);

        // Set the title, time etc.
        eventCard.titleView.setText(event.title);
        EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
        if (eventTime == null) {
            eventCard.timeView.setVisibility(View.GONE);
        } else {
            eventCard.timeView.setVisibility(View.VISIBLE);
            eventCard.timeView.setText(eventTime.toString());
        }

        // Set the venue.
        eventCard.venueView.setText(Utils.capitalize(
                event.venue == null ? event.city.toString() : event.venue));

        // Set num people interested.
        if (event.numPeopleInterested <= 0) {
            eventCard.numPeopleInterestedView.setVisibility(View.GONE);
        } else {
            eventCard.numPeopleInterestedView.setVisibility(View.VISIBLE);
            eventCard.numPeopleInterestedView.setText(
                    Integer.toString(event.numPeopleInterested));
        }

        // Set actions handlers.
        setFavouriteView(eventCard, eventsMarkerEditor.getEventsMarkerManager().getEventMark(event.id));
        eventCard.favouriteView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("addFavourite");
                eventsMarkerEditor.recordPref(event.id, EventMark.FAVOURITE);
                setFavouriteView(eventCard, EventMark.FAVOURITE);
            }
        });

        eventCard.favouritedView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("removeFavourite");
                eventsMarkerEditor.recordPref(event.id, null);
                setFavouriteView(eventCard, null);
            }
        });

        eventCard.dismissView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("dismiss");
                Animation anim = AnimationUtils.loadAnimation(activity, android.R.anim.fade_out);
                anim.setDuration(500);
                anim.setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        // do nothing.
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // Report the dismiss and let list refresh.
                        eventsMarkerEditor.recordPref(event.id, EventMark.DISMISSED);
                        Toast.makeText(activity, R.string.message_dismiss, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        // do nothing.
                    }
                });
                view.startAnimation(anim);
            }
        });

        return view;
    }

    private static class EventCard {
        private final NetworkImageView bgView;
        private final ImageView recommendedImageView;
        private final TextView titleView;
        private final TextView timeView;
        private final TextView venueView;
        private final TextView numPeopleInterestedView;
        private final FrameLayout favouriteView;
        private final FrameLayout favouritedView;
        private final FrameLayout dismissView;

        private EventCard(View cardView) {
            bgView = (NetworkImageView) cardView.findViewById(R.id.event_bg);
            recommendedImageView = (ImageView) cardView.findViewById(R.id.event_recommended);
            titleView = (TextView) cardView.findViewById(R.id.event_title);
            timeView = (TextView) cardView.findViewById(R.id.event_time);
            venueView = (TextView) cardView.findViewById(R.id.event_venue);
            numPeopleInterestedView = (TextView) cardView.findViewById(R.id.num_people_interested);
            favouriteView = (FrameLayout) cardView.findViewById(R.id.action_favourite);
            favouritedView = (FrameLayout) cardView.findViewById(R.id.action_favourited);
            dismissView = (FrameLayout) cardView.findViewById(R.id.action_dismiss);
        }
    }

    public static void setFavouriteView(EventCard eventCard, @Nullable EventMark pref) {
        boolean isFavourite = EventMark.isFavourite(pref);
        eventCard.favouritedView.setVisibility(isFavourite ? View.VISIBLE : View.GONE);
        eventCard.favouriteView.setVisibility(isFavourite ? View.GONE : View.VISIBLE);
    }
}
