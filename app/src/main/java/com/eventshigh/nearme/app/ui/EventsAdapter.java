package com.eventshigh.nearme.app.ui;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.BaseEventsActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An adapter which can be used to populate the Event card.
 */
public class EventsAdapter extends RecyclerView.Adapter<ViewHolder> {
    private static final int NUM_MAX_EVENTS_PER_INTEREST = 3;

    private final BaseContextActivity activity;
    private final Map<String, Integer> eventIdToItemIdMap = new HashMap<>();
    private final Set<Integer> usedItemIds = new HashSet<>();
    private List<Data> dataToShow;

    public EventsAdapter(BaseContextActivity activity) {
        this.activity = activity;

        dataToShow = new ArrayList<>();
        setHasStableIds(true);
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

        for (Pair<String, List<Event>> myEventEntry : myEvents) {
            boolean isFavourite = myEventEntry.first.equals(MyEventsRequest.FAVOURITES_NAME);
            dataToShow.add(new HeaderData(myEventEntry.first));
            List<Event> events = isFavourite ? myEventEntry.second :
                    myEventEntry.second.subList(0,
                            Math.min(NUM_MAX_EVENTS_PER_INTEREST, myEventEntry.second.size()));
            for (Event event : events) {
                dataToShow.add(new MyEventData(myEventEntry.first, event));
            }
        }
        notifyDataSetChanged();
    }

    public void setExploreEvents(MyEvents myEvents) {
        dataToShow.clear();

        for (Pair<String, List<Event>> myEventEntry : myEvents) {
            if (!myEventEntry.second.isEmpty()) {
                dataToShow.add(new HeaderData(myEventEntry.first));
                dataToShow.add(new EventListData(myEventEntry.first, myEventEntry.second));
            }
        }
        notifyDataSetChanged();
    }

    public void addFollowCard(String title) {
        dataToShow.add(0, new FollowData(title));
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
        final EventCard eventCard = reuseView == null ?
                EventCard.newInstance(activity, parent, false) :
                new EventCard(reuseView, false);
        eventCard.bindEventView(event, activity, 0);
        return eventCard.itemView;
    }

    private enum DataType {
        HEADER(0),
        EVENT(1),
        OFFER(2),
        FOLLOW(3),
        EVENT_LIST(4),
        MY_EVENT(5);

        public final int typeId;
        DataType (int typeId) {
            this.typeId = typeId;
        }

        public static ViewHolder onCreateViewHolder(BaseActivity activity, ViewGroup parent, int typeId) {
            if (typeId == HEADER.typeId) {
                return HeaderCard.newInstance(activity, parent);
            }

            if (typeId == EVENT.typeId) {
                return EventCard.newInstance(activity, parent, true);
            }

            if (typeId == OFFER.typeId) {
                return OfferCard.newInstance(activity, parent);
            }

            if (typeId == FOLLOW.typeId) {
                return FollowCard.newInstance(activity, parent);
            }

            if (typeId == EVENT_LIST.typeId) {
                return EventListCard.newInstance(activity, parent);
            }

            if (typeId == MY_EVENT.typeId) {
                return MyEventCard.newInstance(activity, parent);
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

        private HeaderData(String header) {
            this.header = header;
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
        private final TextView titleView;
        private final View moreView;

        private static HeaderCard newInstance(Activity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.my_event_header, parent, false);
            return new HeaderCard(view);
        }

        private HeaderCard(View cardView) {
            super(cardView);
            this.titleView = (TextView) cardView.findViewById(R.id.header);
            this.moreView = cardView.findViewById(R.id.header_more);
        }

        private void bindHeaderView(final BaseContextActivity activity, final HeaderData header) {
            titleView.setText(Utils.capitalize(header.header));
            boolean isFavourite = header.header.equals(MyEventsRequest.FAVOURITES_NAME);
            moreView.setVisibility(isFavourite ? View.GONE : View.VISIBLE);
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
        private final boolean bigLayout;
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

        private static EventCard newInstance(Activity activity, ViewGroup parent, boolean bigLayout) {
            View view = activity.getLayoutInflater().inflate(
                    bigLayout ? R.layout.big_event_card : R.layout.event_card, parent, false);
            return new EventCard(view, bigLayout);
        }

        public EventCard(View cardView, boolean bigLayout) {
            super(cardView);

            this.bigLayout = bigLayout;
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
        }

        public void setFavouriteView(@Nullable EventMark eventMark) {
            boolean isFavourite = EventMark.isFavourite(eventMark);
            favouritedView.setVisibility(isFavourite ? View.VISIBLE : View.GONE);
            favouriteView.setVisibility(isFavourite ? View.GONE : View.VISIBLE);
        }

        private void bindEventView(final Event event, final BaseContextActivity activity,
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
            bgView.setDefaultImageResId(R.drawable.eh_default_event);
            bgView.setErrorImageResId(R.drawable.eh_default_event);
            bgView.setImageUrl(event.imgUrl, VolleyHelper.getImageLoader(activity));

            if (bigLayout) {
                Utils.waitForViewVisible(bgView, new Runnable() {
                    @Override
                    public void run() {
                        LayoutParams lp = bgView.getLayoutParams();
                        lp.height = 9 * bgView.getWidth() / 16;
                        bgView.setLayoutParams(lp);
                    }
                });
            }

            // recommended ? Offer ?
            recommendedImageView.setVisibility(event.ehRecommended ? View.VISIBLE : View.INVISIBLE);
            offerView.setVisibility(event.offerTitle != null ? View.VISIBLE : View.GONE);

            // Set the title.
            titleView.setText(event.title);

            // Event Time.
            if (eventTimeView != null) {
                EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
                if (eventTime == null) {
                    eventTimeView.setVisibility(View.INVISIBLE);
                } else {
                    eventTimeView.setVisibility(View.VISIBLE);
                    eventTimeView.setText(eventTime.toString());
                }
            }

            // Set the venue.
            if (venueView != null) {
                venueView.setText(event.getShortAddress());
            }

            // Set the travel time.
            if (travelTimeView != null) {
                String travelTime = LocationUtils.getTravelTime(activity, activity.getUserLocation(),
                        event.location);
                if (travelTime != null) {
                    travelTimeView.setText(travelTime);
                    travelTimeView.setVisibility(View.VISIBLE);
                } else {
                    travelTimeView.setVisibility(View.GONE);
                }
            }

            // Set num people interested.
            if (numPeopleInterestedView != null) {
                if (event.numPeopleInterested <= 0) {
                    numPeopleInterestedView.setVisibility(View.INVISIBLE);
                } else {
                    numPeopleInterestedView.setVisibility(View.VISIBLE);
                    numPeopleInterestedView.setText(Integer.toString(event.numPeopleInterested));
                }
            }

            // Set actions handlers.
            if (favouriteView != null) {
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
            }
        }
    }

    private class OfferData implements Data {
        private final Offer offer;

        private OfferData(com.eventshigh.nearme.app.data.Offer offer) {
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
            return offer.id;
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


    private class FollowData implements Data {
        private final String title;

        private FollowData(String title) {
            this.title = title;
        }

        @Override
        public DataType getType() {
            return DataType.FOLLOW;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            ((FollowCard) card).populate(this, activity);
        }

        @Override
        public String getId() {
            return title;
        }
    }

    static class FollowCard extends ViewHolder {
        private TextView titleView;
        private View followButton;
        private View followingButton;

        static FollowCard newInstance(final BaseActivity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.follow_card, parent, false);
            return new FollowCard(view);
        }

        public FollowCard(View itemView) {
            super(itemView);

            titleView = (TextView) itemView.findViewById(R.id.title);
            followButton = itemView.findViewById(R.id.follow_button);
            followingButton = itemView.findViewById(R.id.following_button);
        }

        public void populate(final FollowData data, final BaseContextActivity activity) {
            titleView.setText(data.title);

            final Account account = new Account(activity);
            setFollowButtons(account.isFollowing(data.title));
            followButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportActionToAnalytics("addFollowing", data.title);
                    account.setIsFollowing(data.title, true);
                    setFollowButtons(true);
                    activity.showMyEventsClue(null);
                }
            });
            followingButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportActionToAnalytics("removeFollowing", data.title);
                    account.setIsFollowing(data.title, false);
                    setFollowButtons(false);
                    activity.hideMyEventsClue();
                }
            });
        }

        public void setFollowButtons(boolean isFollowing) {
            followButton.setVisibility(isFollowing ? View.GONE : View.VISIBLE);
            followingButton.setVisibility(isFollowing ? View.VISIBLE : View.GONE);
            followingButton.setSelected(true);
        }
    }

    private class EventListData implements Data {
        private final String header;
        private final List<Event> events;

        public EventListData(String header, List<Event> events) {
            this.header = header;
            this.events = events;
        }

        @Override
        public DataType getType() {
            return DataType.EVENT_LIST;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            ((EventListCard) card).bindEventsView(events, activity);
        }

        public String getId() {
            return header + ":" + events.get(0).id;
        }
    }

    private static class EventListCard extends ViewHolder {
        static EventListCard newInstance(final BaseActivity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.explore_events_list, parent, false);
            return new EventListCard(view);
        }

        private LinearLayout eventContainer;
        public EventListCard(View itemView) {
            super(itemView);

            eventContainer = (LinearLayout) itemView.findViewById(R.id.event_container);
        }

        public void bindEventsView(List<Event> events, BaseContextActivity activity) {
            eventContainer.removeAllViews();
            for (Event event : events) {
                View cardView = activity.getLayoutInflater().inflate(R.layout.explore_event_card, eventContainer, false);
                eventContainer.addView(cardView);
                new EventCard(cardView, true).bindEventView(event, activity, 0);
            }
        }
    }

    private class MyEventData implements Data {
        private final String header;
        private final Event event;

        public MyEventData(String header, Event event) {
            this.header = header;
            this.event = event;
        }

        @Override
        public DataType getType() {
            return DataType.MY_EVENT;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            ((MyEventCard) card).bindEventsView(event, activity, position);
        }

        public String getId() {
            return header + ":" + event.id;
        }
    }

    private static class MyEventCard extends ViewHolder {
        static MyEventCard newInstance(final BaseActivity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.event_card, parent, false);
            return new MyEventCard(view);
        }

        public MyEventCard(View itemView) {
            super(itemView);
        }

        public void bindEventsView(Event event, BaseContextActivity activity, int position) {
            new EventCard(itemView, false).bindEventView(event, activity, position);
        }
    }
}
