package com.eventshigh.nearme.app.ui;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseEventsActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An adapter which can be used to populate the Event card.
 */
public class EventsAdapter extends RecyclerView.Adapter<ViewHolder> {
    private static final int NUM_MAX_EVENTS_PER_INTEREST = 3;
    private static final int HEADER_BG_RESOURCES[] = new int [] {
            R.drawable.eh_myevents_header1,
            R.drawable.eh_myevents_header2,
            R.drawable.eh_myevents_header3,
            R.drawable.eh_myevents_header4,
            R.drawable.eh_myevents_header5,
            R.drawable.eh_myevents_header6
    };

    private final BaseEventsActivity activity;
    private final Map<String, Integer> eventIdToItemIdMap = new HashMap<>();
    private final Set<Integer> usedItemIds = new HashSet<>();
    private List<Data> dataToShow;

    public EventsAdapter(BaseEventsActivity activity) {
        this.activity = activity;

        dataToShow = new ArrayList<>();
        setHasStableIds(true);
    }

    public void removeEvent(Event event) {
        boolean changed = false;
        for (Iterator<Data> it =  dataToShow.iterator(); it.hasNext(); ) {
            Data data = it.next();
            if (data instanceof EventData &&
                event.id.equals(((EventData) data).event.id)) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            notifyDataSetChanged();
        }
    }

    public void setEvents(List<Event> events) {
        dataToShow.clear();
        for (Event event: events) {
            dataToShow.add(new EventData("", event));
        }
        notifyDataSetChanged();
    }

    public void addOffer(Offer offer) {
        if (dataToShow.size() > 10 && dataToShow.get(10) instanceof EventData) {
            dataToShow.add(10, new OfferData(offer));
            notifyDataSetChanged();
        }
    }

    public void setMyEvents(MyEvents myEvents) {
        dataToShow.clear();

        for (int i = 0; i < myEvents.size(); i++) {
            Pair<String, List<Event>> myEventEntry = myEvents.get(i);
            boolean isFavourite = myEventEntry.first.equals(MyEventsRequest.FAVOURITES_NAME);
            dataToShow.add(new HeaderData(myEventEntry.first,
                    HEADER_BG_RESOURCES[i % HEADER_BG_RESOURCES.length]));
            List<Event> events = isFavourite ? myEventEntry.second :
                    myEventEntry.second.subList(0,
                            Math.min(NUM_MAX_EVENTS_PER_INTEREST, myEventEntry.second.size()));
            for (Event event : events) {
                dataToShow.add(new EventData(myEventEntry.first, event));
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return dataToShow.get(position).getType().typeId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
        return DataType.onCreateViewHolder(activity, viewGroup, type);
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        dataToShow.get(position).onBindViewHolder(card, position);
    }

    @Override
    public int getItemCount() {
        return dataToShow.size();
    }

    @Override
    public long getItemId(int position) {
        return getItemId(dataToShow.get(position).getId());
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
    public static View getEventCard(final Event event, final BaseEventsActivity activity,
                                    @Nullable View reuseView, ViewGroup parent) {
        // Build the view, reuse existing if possible.
        final EventCard eventCard = reuseView == null ? EventCard.newInstance(activity, parent) :
                new EventCard(reuseView);
        eventCard.bindEventView(event, activity, 0);
        return eventCard.itemView;
    }

    private enum DataType {
        HEADER(0),
        EVENT(1),
        OFFER(2);

        public final int typeId;
        DataType (int typeId) {
            this.typeId = typeId;
        }

        public static ViewHolder onCreateViewHolder(BaseActivity activity, ViewGroup parent, int typeId) {
            if (typeId == HEADER.typeId) {
                return HeaderCard.newInstance(activity, parent);
            }

            if (typeId == EVENT.typeId) {
                return EventCard.newInstance(activity, parent);
            }

            if (typeId == OFFER.typeId) {
                return OfferCard.newInstance(activity, parent);
            }

            throw new IllegalArgumentException("invalid typeid");
        }
    }

    private interface Data {
        DataType getType();
        void onBindViewHolder(ViewHolder card, int position);
        String getId();
    }

    // Header Data.
    private class HeaderData implements Data {
        private final String header;
        private final int bgResourceId;

        private HeaderData(String header, int bgResourceId) {
            this.header = header;
            this.bgResourceId = bgResourceId;
        }

        @Override
        public DataType getType() {
            return DataType.HEADER;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            ((HeaderCard) card).bindHeaderView(activity, this);
        }

        @Override
        public String getId() {
            return header;
        }
    }

    private static class HeaderCard extends ViewHolder {
        private final ImageView headerBg;
        private final TextView titleView;
        private final ImageView arrowView;

        private static HeaderCard newInstance(Activity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.my_event_header, parent, false);
            return new HeaderCard(view);
        }

        private HeaderCard(View cardView) {
            super(cardView);
            this.headerBg = (ImageView) cardView.findViewById(R.id.header_bg);
            this.titleView = (TextView) cardView.findViewById(R.id.header);
            this.arrowView = (ImageView) cardView.findViewById(R.id.header_arrow);
        }

        private void bindHeaderView(final BaseEventsActivity activity, final HeaderData header) {
            headerBg.setImageResource(header.bgResourceId);
            titleView.setText(Utils.capitalize(header.header));
            boolean isFavourite = header.header.equals(MyEventsRequest.FAVOURITES_NAME);
            arrowView.setVisibility(isFavourite ? View.GONE : View.VISIBLE);
            if (!isFavourite) {
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.showSearchView(header.header);
                    }
                });
            }
        }
    }

    // EventData.
    private class EventData implements Data {
        private final String header;
        private final Event event;

        public EventData(String header, Event event) {
            this.header = header;
            this.event = event;
        }

        @Override
        public DataType getType() {
            return DataType.EVENT;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            ((EventCard) card).bindEventView(event, activity, position);
        }

        public String getId() {
            return header + ":" + event.id;
        }
    }

    private static class EventCard extends ViewHolder {
        private final NetworkImageView bgView;
        private final ImageView recommendedImageView;
        private final ImageView offerView;
        private final TextView titleView;
        private final TextView eventTimeView;
        private final TextView venueView;
        private final TextView travelTimeView;
        private final TextView numPeopleInterestedView;
        private final View favouriteView;
        private final View favouritedView;
        private final FrameLayout shareView;

        private static EventCard newInstance(Activity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.big_event_card, parent, false);
            return new EventCard(view);
        }

        public EventCard(View cardView) {
            super(cardView);
            bgView = (NetworkImageView) cardView.findViewById(R.id.event_bg);
            recommendedImageView = (ImageView) cardView.findViewById(R.id.event_recommended);
            offerView = (ImageView) cardView.findViewById(R.id.event_offer_marker);
            titleView = (TextView) cardView.findViewById(R.id.event_title);
            eventTimeView = (TextView) cardView.findViewById(R.id.event_time);
            venueView = (TextView) cardView.findViewById(R.id.event_venue);
            travelTimeView = (TextView) cardView.findViewById(R.id.event_travel_time);
            numPeopleInterestedView = (TextView) cardView.findViewById(R.id.num_people_interested);
            favouriteView = cardView.findViewById(R.id.action_favourite);
            favouritedView = cardView.findViewById(R.id.action_favourited);
            shareView = (FrameLayout) cardView.findViewById(R.id.share);
        }

        public void setFavouriteView(@Nullable EventMark eventMark) {
            boolean isFavourite = EventMark.isFavourite(eventMark);
            favouritedView.setVisibility(isFavourite ? View.VISIBLE : View.GONE);
            favouriteView.setVisibility(isFavourite ? View.GONE : View.VISIBLE);
        }

        private void bindEventView(final Event event, final BaseEventsActivity activity,
                                   final int position) {
            itemView.setTag(position);
            itemView.setVisibility(View.VISIBLE);
            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.showEventDetails(event, position);
                }
            });

            // Set the background image.
            bgView.setDefaultImageResId(R.drawable.eh_default_event_list);
            bgView.setErrorImageResId(R.drawable.eh_default_event_list);
            bgView.setImageUrl(event.imgUrl, VolleyHelper.getImageLoader(activity));

            // Check if its a recommended event or not.
            recommendedImageView.setVisibility(event.ehRecommended ? View.VISIBLE : View.INVISIBLE);

            // Set the title.
            titleView.setText(event.title);

            // Event Time.
            EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
            if (eventTime == null) {
                eventTimeView.setVisibility(View.INVISIBLE);
            } else {
                eventTimeView.setVisibility(View.VISIBLE);
                eventTimeView.setText(eventTime.toString());
            }

            // Set the venue.
            venueView.setText(event.getShortAddress());

            // Set the time.
            String travelTime = LocationUtils.getTravelTime(activity, activity.getUserLocation(),
                    event.location);
            if (travelTime != null) {
                travelTimeView.setText(travelTime);
                travelTimeView.setVisibility(View.VISIBLE);
            } else {
                travelTimeView.setVisibility(View.GONE);
            }

            // Set num people interested.
            if (event.numPeopleInterested <= 0) {
                numPeopleInterestedView.setVisibility(View.INVISIBLE);
            } else {
                numPeopleInterestedView.setVisibility(View.VISIBLE);
                numPeopleInterestedView.setText(Integer.toString(event.numPeopleInterested));
            }

            // Offer ?
            offerView.setVisibility(event.offerTitle != null ? View.VISIBLE : View.GONE);

            // Set actions handlers.
            setFavouriteView(activity.getEventMark(event));
            favouriteView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportEventAction(event, "addFavourite", position);
                    activity.recordEventMark(event, EventMark.FAVOURITE);
                    setFavouriteView(EventMark.FAVOURITE);
                }
            });

            favouritedView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportEventAction(event, "removeFavourite", position);
                    activity.recordEventMark(event, null);
                    setFavouriteView(null);
                }
            });

            if (shareView != null) {
                shareView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.shareEvent(event);
                    }
                });
            }
        }
    }

    private class OfferData implements Data {
        private final Offer offer;

        private OfferData(Offer offer) {
            this.offer = offer;
        }

        @Override
        public DataType getType() {
            return DataType.OFFER;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            offer.populateOfferCard(card.itemView, activity);
        }

        @Override
        public String getId() {
            return getType().toString();
        }
    }

    static class OfferCard extends ViewHolder {

        static OfferCard newInstance(final BaseActivity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(
                    R.layout.offer_card, parent, false);
            return new OfferCard(view);
        }

        public OfferCard(View itemView) {
            super(itemView);
        }
    }
}
