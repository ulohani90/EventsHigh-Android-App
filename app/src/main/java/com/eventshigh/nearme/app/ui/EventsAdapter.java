package com.eventshigh.nearme.app.ui;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseEventsActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsMarkerManager.Editor;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.EventsAdapter.EventCard;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An {@link android.widget.ListAdapter} which can be used to populate the Event card.
 */
public class EventsAdapter extends RecyclerView.Adapter<EventCard> {
    // We show the dismiss toast once per session.
    private static boolean showDismissToast = true;

    private final BaseEventsActivity activity;
    private final Editor eventsMarkerEditor;
    private final Map<String, Integer> eventIdToItemIdMap = new HashMap<>();
    private final Set<Integer> usedItemIds = new HashSet<>();
    private final List<Event> events;

    private final int animationOffset;
    private boolean enableAnimation = true;
    private int lastPosition = -1;
    private String lastChangedEventId;

    public EventsAdapter(BaseEventsActivity activity, Collection<Event> events, Editor eventsMarkerEditor) {
        this.activity = activity;
        this.eventsMarkerEditor = eventsMarkerEditor;
        this.events = new ArrayList<>(events);
        animationOffset = Utils.dpToPx(activity, 100);

        setHasStableIds(true);
    }

    public void remove(Event event) {
        this.events.remove(event);
        notifyDataSetChanged();
    }

    public void insert(Event event, int insertAt) {
        this.events.add(insertAt, event);
        enableAnimation = false;
        notifyDataSetChanged();
    }

    public void notifyDataSetChanged(String eventId) {
        if (lastChangedEventId == null || !eventId.equals(lastChangedEventId)) {
            notifyDataSetChanged();
        }
        lastChangedEventId = null;
    }

    public void onResumeAnimation() {
        enableAnimation = true;
    }

    @Override
    public EventCard onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(activity).inflate(R.layout.event_card, viewGroup, false);
        return new EventCard(view);
    }

    @Override
    public void onBindViewHolder(EventCard eventCard, int i) {
        bindView(eventCard, events.get(i), activity, i, eventsMarkerEditor, this);

        if (enableAnimation) {
            eventCard.cardView.setTranslationY(i > lastPosition ? animationOffset : -animationOffset);
            lastPosition = i;
            eventCard.cardView.animate().translationY(0).setDuration(500).start();
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    @Override
    public long getItemId(int position) {
        return getItemId(getItem(position).id);
    }

    public Event getItem(int i) {
        return events.get(i);
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
                               @Nullable View reuseView, ViewGroup parent,
                               final Editor eventsMarkerEditor) {
        // Build the view, reuse existing if possible.
        final View view = reuseView == null ?
                activity.getLayoutInflater().inflate(R.layout.event_card, parent, false) :
                reuseView;
        final EventCard eventCard = new EventCard(view);
        bindView(eventCard, event, activity, 0, eventsMarkerEditor, null);
        return view;
    }

    private static void bindView(final EventCard eventCard, final Event event,
                                 final BaseEventsActivity activity, final int position,
                                 final Editor eventsMarkerEditor,
                                 @Nullable final EventsAdapter adapter) {
        eventCard.cardView.setVisibility(View.VISIBLE);
        eventCard.cardView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showEventDetails(event, position);
            }
        });

        // Set the background image.
        eventCard.bgView.setDefaultImageResId(R.drawable.eh_default_event_list);
        if (event.imgUrl != null) {
            eventCard.bgView.setImageUrl(event.imgUrl,
                    VolleyHelper.getImageLoader(activity.getApplicationContext()));
        }

        // Check if its a recommended event or not.
        eventCard.recommendedImageView.setVisibility(event.ehRecommended ? View.VISIBLE :
                View.INVISIBLE);

        // Set the title, time etc.
        eventCard.titleView.setText(event.title);
        EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
        if (eventTime == null) {
            eventCard.timeView.setVisibility(View.INVISIBLE);
        } else {
            eventCard.timeView.setVisibility(View.VISIBLE);
            eventCard.timeView.setText(eventTime.toString());
        }

        // Set the venue.
        eventCard.venueView.setText(Utils.capitalize(
                event.venue == null ? event.city.toString() : event.venue));

        // Set num people interested.
        if (event.numPeopleInterested <= 0) {
            eventCard.numPeopleInterestedView.setVisibility(View.INVISIBLE);
        } else {
            eventCard.numPeopleInterestedView.setVisibility(View.VISIBLE);
            eventCard.numPeopleInterestedView.setText(
                    Integer.toString(event.numPeopleInterested));
        }

        // Set actions handlers.
        setFavouriteView(eventCard, eventsMarkerEditor.getEventsMarkerManager().getEventMark(
                event.id));
        eventCard.favouriteView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("addFavourite");
                if (adapter != null) {
                    adapter.lastChangedEventId = event.id;
                }
                eventsMarkerEditor.recordPref(event.id, EventMark.FAVOURITE);
                setFavouriteView(eventCard, EventMark.FAVOURITE);
            }
        });

        eventCard.favouritedView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("removeFavourite");
                if (adapter != null) {
                    adapter.lastChangedEventId = event.id;
                }
                eventsMarkerEditor.recordPref(event.id, null);
                setFavouriteView(eventCard, null);
            }
        });

        eventCard.dismissView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("dismiss");
                if (adapter != null) {
                    adapter.enableAnimation = false;
                }
                eventsMarkerEditor.recordPref(event.id, EventMark.DISMISSED);
                if (showDismissToast) {
                    Toast.makeText(activity, R.string.message_dismiss, Toast.LENGTH_SHORT).show();
                    showDismissToast = false;
                }
            }
        });
    }

    public static class EventCard extends RecyclerView.ViewHolder {
        public final View cardView;
        private final NetworkImageView bgView;
        private final ImageView recommendedImageView;
        private final TextView titleView;
        private final TextView timeView;
        private final TextView venueView;
        private final TextView numPeopleInterestedView;
        private final FrameLayout favouriteView;
        private final FrameLayout favouritedView;
        private final FrameLayout dismissView;

        public EventCard(View cardView) {
            super(cardView);
            this.cardView = cardView;
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
