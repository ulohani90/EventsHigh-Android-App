package com.eventshigh.nearme.app.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.BaseEventsActivity;
import com.eventshigh.nearme.app.activity.EventDetailActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.data.TrendingTopic;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvent;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.PaletteImageView;

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
    private static final String TRENDING_TOPIC_TITLE = "What's Trending";

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

    public void setMyEvents(MyEvents myEvents, boolean addMyEventHeader, int maxPerCategory) {
        dataToShow.clear();
        boolean showTrendingTopics = !myEvents.trendingTopics.isEmpty();

        for (TopicEvent topicEvent : myEvents.topicEvents) {
            if (addMyEventHeader && !dataToShow.isEmpty()) {
                dataToShow.add(new MyEventHeaderData());
                addMyEventHeader = false;
            }

            if (showTrendingTopics && dataToShow.size() > 10) {
                dataToShow.add(new HeaderData(TRENDING_TOPIC_TITLE, false));
                Iterator<TrendingTopic> trendingTopicIterator = myEvents.trendingTopics.iterator();
                while (trendingTopicIterator.hasNext()) {
                    TrendingTopic topic1 = trendingTopicIterator.next();
                    if (trendingTopicIterator.hasNext()) {
                        TrendingTopic topic2 = trendingTopicIterator.next();
                        dataToShow.add(new TrendingCategoryData(topic1, topic2));
                    }
                }
                showTrendingTopics = false;
            }

            List<Event> events = topicEvent.events;
            if (events.isEmpty()) {
                continue;
            }

            boolean isFavourite = topicEvent.topicName.equals(MyEventsRequest.FAVOURITES_NAME);
            boolean showMore = false;
            if (!isFavourite && events.size() > maxPerCategory) {
                showMore = true;
                events = events.subList(0, maxPerCategory);
            }

            dataToShow.add(new HeaderData(topicEvent.topicName, showMore));
            for (Event event : events) {
                dataToShow.add(new MyEventData(topicEvent.topicName, event));
            }
        }

        notifyDataSetChanged();
    }

    public void addFollowCard(String title, int numEvents) {
        dataToShow.add(0, new FollowData(title, numEvents));
    }

    public boolean spanAllColumns(int position) {
        return DataType.spanAllColumns(getItemViewType(position));
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
        View view = reuseView != null ? reuseView :
                activity.getLayoutInflater().inflate(R.layout.card_event_small, parent, false);
        new EventCard(view, true).bindEventView(event, activity, -1);
        return view;
    }

    private enum DataType {
        HEADER(0),
        EVENT(1),
        OFFER(2),
        FOLLOW(3),
        MY_EVENT(4),
        MY_EVENT_HEADER(5),
        TRENDING_CATEGORY(6);

        public final int typeId;
        DataType (int typeId) {
            this.typeId = typeId;
        }

        public static boolean spanAllColumns (int typeId) {
            return  typeId == HEADER.typeId || typeId == MY_EVENT_HEADER.typeId;
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

            if (typeId == MY_EVENT.typeId) {
                return EventCard.newInstance(activity, parent, false);
            }

            if (typeId == MY_EVENT_HEADER.typeId) {
                View view = activity.getLayoutInflater().inflate(
                        R.layout.card_my_events_header, parent, false);
                return new HeaderCard(view);
            }

            if (typeId == TRENDING_CATEGORY.typeId) {
                return TrendingCategoryCard.newInstance(activity, parent);
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
        private final boolean showMore;

        private HeaderData(String header, boolean showMore) {
            this.header = header;
            this.showMore = showMore;
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
            View view = activity.getLayoutInflater().inflate(R.layout.card_header, parent, false);
            return new HeaderCard(view);
        }

        private HeaderCard(View cardView) {
            super(cardView);
            this.titleView = (TextView) cardView.findViewById(R.id.header);
            this.moreView = cardView.findViewById(R.id.header_more);
        }

        private void bindHeaderView(final BaseContextActivity activity, final HeaderData header) {
            titleView.setText(Utils.capitalize(header.header));
            moreView.setVisibility(header.showMore ? View.VISIBLE: View.GONE);
            if (header.showMore) {
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.showSearchView(header.header);
                    }
                });
            } else {
                itemView.setClickable(false);
            }
        }
    }

    // Event Data.
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
        private final boolean shouldAdjustImageHeight;
        private final TextView headerView;
        private final NetworkImageView bgView;
        private final ImageView recommendedView;
        private final ImageView offerView;
        private final TextView titleView;
        private final TextView eventTimeView;
        private final TextView priceView;
        private final TextView venueView;
        private final TextView travelTimeView;
        private final ImageView favouriteView;
        private final View fbShareView;
        private final View whatsappShareView;

        private static EventCard newInstance(Activity activity, ViewGroup parent, boolean bigLayout) {
            View view = activity.getLayoutInflater().inflate(
                    bigLayout ? R.layout.card_event_big : R.layout.card_event_small, parent, false);
            return new EventCard(view, false);
        }

        public EventCard(View cardView, boolean shouldAdjustImageHeight) {
            super(cardView);

            this.shouldAdjustImageHeight = shouldAdjustImageHeight;
            headerView = (TextView) cardView.findViewById(R.id.event_header);
            bgView = (NetworkImageView) cardView.findViewById(R.id.event_bg);
            recommendedView = (ImageView) cardView.findViewById(R.id.event_recommended);
            offerView = (ImageView) cardView.findViewById(R.id.event_offer_marker);
            titleView = (TextView) cardView.findViewById(R.id.event_title);
            eventTimeView = (TextView) cardView.findViewById(R.id.event_time);
            priceView = (TextView) cardView.findViewById(R.id.event_price);
            venueView = (TextView) cardView.findViewById(R.id.event_venue);
            travelTimeView = (TextView) cardView.findViewById(R.id.event_travel_time);
            favouriteView = (ImageView) cardView.findViewById(R.id.action_favourite);
            fbShareView = cardView.findViewById(R.id.share_fb);
            whatsappShareView = cardView.findViewById(R.id.share_whatsapp);

            if (bgView instanceof PaletteImageView) {
                ((PaletteImageView) bgView).setHeaderView(headerView);
            }
        }

        public void setFavouriteView(@Nullable EventMark eventMark) {
            favouriteView.setTag(eventMark);
            favouriteView.setImageResource(EventMark.isFavourite(eventMark) ?
                    R.drawable.ic_favorite_red_24dp : R.drawable.ic_favorite_grey600_24dp);
        }

        private void bindEventView(final Event event, final BaseContextActivity activity,
                                   final int position) {
            itemView.setTag(position);
            itemView.setVisibility(View.VISIBLE);
            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Pair<View, String> pair1 = Pair.<View, String>create(bgView, "event_bg");
                    Pair<View, String> pair2 = Pair.<View, String>create(titleView, "event_title");
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            activity, pair1, pair2).toBundle();
                    activity.showEventDetails(event, bundle);
                }
            });

            // Set the background image.
            bgView.setDefaultImageResId(R.drawable.eh_default_event);
            bgView.setErrorImageResId(R.drawable.eh_default_event);
            bgView.setImageUrl(event.imgUrl, VolleyHelper.getImageLoader(activity));

            if (shouldAdjustImageHeight) {
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
            if (recommendedView != null) {
                recommendedView.setVisibility(event.ehRecommended ? View.VISIBLE : View.INVISIBLE);
            }
            if (offerView != null) {
                offerView.setVisibility(event.offerTitle != null ? View.VISIBLE : View.GONE);
            }

            // Set the title.
            titleView.setText(event.title);

            // Event Time.
            if (eventTimeView != null) {
                EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
                if (eventTime == null) {
                    eventTimeView.setVisibility(View.INVISIBLE);
                } else {
                    eventTimeView.setVisibility(View.VISIBLE);
                    if (headerView == null) {
                        eventTimeView.setText(eventTime.toString());
                    } else {
                        headerView.setText(eventTime.getDate());
                        eventTimeView.setText(eventTime.time);
                    }
                }
            }

            // Set the price.
            if (priceView != null) {
                String priceString = event.getPriceString();
                if (priceString == null) {
                    priceView.setVisibility(View.GONE);
                } else {
                    priceView.setVisibility(View.VISIBLE);
                    priceView.setText(priceString);
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

            // Set actions handlers.
            if (favouriteView != null) {
                setFavouriteView(activity.getEventMark(event));
                favouriteView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EventMark oldMark = (EventMark) favouriteView.getTag();
                        EventMark newMark = EventMark.isFavourite(oldMark) ? null : EventMark.FAVOURITE;
                        activity.reportEventAction(event,
                            EventMark.isFavourite(newMark) ? "addFavourite" : "removeFavourite",
                            position);
                        activity.recordEventMark(event, newMark);
                        setFavouriteView(newMark);
                    }
                });
            }

            if (fbShareView != null) {
                fbShareView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.shareEvent(event, EventDetailActivity.PACKAGE_NAME_FACEBOOK);
                    }
                });
            }
            if (whatsappShareView != null) {
                whatsappShareView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.shareEvent(event, EventDetailActivity.PACKAGE_NAME_WHATSAPP);
                    }
                });
            }
        }
    }

    // Offer Data.
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
                    R.layout.card_offer, parent, false);
            return new OfferCard(view);
        }

        public OfferCard(View itemView) {
            super(itemView);
        }
    }


    // Follow Data.
    private class FollowData implements Data {
        private final String title;
        private final int numEvents;

        private FollowData(String title, int numEvents) {
            this.title = title;
            this.numEvents = numEvents;
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
        private TextView subtitleView;
        private View followButton;
        private View followingButton;

        static FollowCard newInstance(final BaseActivity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_follow, parent, false);
            return new FollowCard(view);
        }

        public FollowCard(View itemView) {
            super(itemView);

            titleView = (TextView) itemView.findViewById(R.id.title);
            subtitleView = (TextView) itemView.findViewById(R.id.subtitle);
            followButton = itemView.findViewById(R.id.follow_button);
            followingButton = itemView.findViewById(R.id.following_button);
        }

        public void populate(final FollowData data, final BaseContextActivity activity) {
            titleView.setText(data.title);
            subtitleView.setText(
                String.format(activity.getString(R.string.num_events), data.numEvents));

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

    // My Event Data.
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
            ((EventCard) card).bindEventView(event, activity, position);
        }

        public String getId() {
            return header + ":" + event.id;
        }
    }

    // My Event Header Data.
    private class MyEventHeaderData implements Data {

        @Override
        public DataType getType() {
            return DataType.MY_EVENT_HEADER;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            card.itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.showSearchView(EventsHighEndpoints.QUERY_MY_EVENT);
                }
            });
        }

        public String getId() {
            return MyEventHeaderData.class.getSimpleName();
        }
    }

    // Trending Category data.
    private class TrendingCategoryData implements Data {
        private final TrendingTopic trendingTopic1;
        private final TrendingTopic trendingTopic2;

        private TrendingCategoryData(TrendingTopic trendingTopic1, TrendingTopic trendingTopic2) {
            this.trendingTopic1 = trendingTopic1;
            this.trendingTopic2 = trendingTopic2;
        }

        @Override
        public DataType getType() {
            return DataType.TRENDING_CATEGORY;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, final int position) {
            ((TrendingCategoryCard) card).populate(this, activity);
        }

        public String getId() {
            return MyEventHeaderData.class.getSimpleName();
        }
    }

    private static class TrendingCategoryCard extends ViewHolder {
        private View trending1Card;
        private NetworkImageView trending1Image;
        private TextView trending1Title;
        private View trending2Card;
        private NetworkImageView trending2Image;
        private TextView trending2Title;

        static TrendingCategoryCard newInstance(final BaseActivity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_trending, parent, false);
            return new TrendingCategoryCard(view);
        }

        public TrendingCategoryCard(View itemView) {
            super(itemView);

            trending1Card = itemView.findViewById(R.id.trending1_card);
            trending1Image = (NetworkImageView) itemView.findViewById(R.id.trending1_image);
            trending1Title = (TextView) itemView.findViewById(R.id.trending1_title);

            trending2Card = itemView.findViewById(R.id.trending2_card);
            trending2Image = (NetworkImageView) itemView.findViewById(R.id.trending2_image);
            trending2Title = (TextView) itemView.findViewById(R.id.trending2_title);
        }

        public void populate(final TrendingCategoryData data, final BaseContextActivity activity) {
            trending1Image.setImageUrl(data.trendingTopic1.imgUrl,
                    VolleyHelper.getImageLoader(activity));
            trending1Title.setText(data.trendingTopic1.tagName);
            trending1Card.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.showSearchView(data.trendingTopic1.tagName);
                }
            });

            trending2Image.setImageUrl(data.trendingTopic2.imgUrl,
                    VolleyHelper.getImageLoader(activity));
            trending2Title.setText(data.trendingTopic2.tagName);
            trending2Card.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.showSearchView(data.trendingTopic2.tagName);
                }
            });
        }
    }
}
