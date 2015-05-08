package com.eventshigh.nearme.app.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
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
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.data.TrendingTopic;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
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
            dataToShow.add(new EventData("", event, false));
        }
        notifyDataSetChanged();
    }

    public void addOffer(Offer offer) {
        if (dataToShow.size() > 10 && dataToShow.get(10) instanceof EventData) {
            dataToShow.add(10, new OfferData(offer));
            notifyDataSetChanged();
        }
    }

    public void setMyEvents(MyEvents myEvents, int maxPerCategory) {
        dataToShow.clear();
        boolean showTrendingTopics = !myEvents.trendingTopics.isEmpty();

        for (int i = 0; i < myEvents.topicEvents.size(); i++) {
            if (showTrendingTopics && i == 3) {
                dataToShow.add(new HeaderData(TRENDING_TOPIC_TITLE, 0));
                for (TrendingTopic trendingTopic : myEvents.trendingTopics) {
                    dataToShow.add(new TrendingCategoryData(trendingTopic));
                }
                showTrendingTopics = false;
            }

            TopicEvents topicEvents = myEvents.topicEvents.get(i);
            List<Event> events = topicEvents.events;
            if (events.isEmpty()) {
                continue;
            }

            boolean isFavourite = topicEvents.topicName.equals(MyEventsRequest.FAVOURITES_NAME);
            if (!isFavourite && events.size() > maxPerCategory) {
                events = events.subList(0, maxPerCategory);
            }

            dataToShow.add(new HeaderData(topicEvents.topicName, topicEvents.numEvents));
            boolean isFirstEvent = true;
            for (Event event : events) {
                dataToShow.add(new EventData(topicEvents.topicName, event, isFirstEvent));
                isFirstEvent = false;
            }
        }

        notifyDataSetChanged();
    }

    public void setExploreCategories(String[] tags) {
        dataToShow.clear();
        for (String tag : tags) {
            dataToShow.add(new ExploreCategoryData(tag));
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
                activity.getLayoutInflater().inflate(R.layout.card_event_maps, parent, false);
        new EventCard(view, true).bindEventView(event, false, activity, -1);
        return view;
    }

    private enum DataType {
        HEADER(0),
        EVENT(1),
        OFFER(2),
        FOLLOW(3),
        TRENDING_CATEGORY(4),
        EXPLORE_CATEGORY(5);

        public final int typeId;
        DataType (int typeId) {
            this.typeId = typeId;
        }

        public static boolean spanAllColumns (int typeId) {
            return  typeId == HEADER.typeId;
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

            if (typeId == TRENDING_CATEGORY.typeId) {
                return TrendingCategoryCard.newInstance(activity, parent);
            }

            if (typeId == EXPLORE_CATEGORY.typeId) {
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
        private final int numEvents;

        private HeaderData(String header, int numEvents) {
            this.header = header;
            this.numEvents = numEvents;
        }

        public boolean showMore() {
            return numEvents > 0 && !header.equals(MyEventsRequest.FAVOURITES_NAME);
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
        private final TextView numEventsView;
        private final View moreView;

        private static HeaderCard newInstance(Activity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_header, parent, false);
            return new HeaderCard(view);
        }

        private HeaderCard(View cardView) {
            super(cardView);
            this.titleView = (TextView) cardView.findViewById(R.id.header);
            this.numEventsView = (TextView) cardView.findViewById(R.id.num_events);
            this.moreView = cardView.findViewById(R.id.header_more);
        }

        private void bindHeaderView(final BaseContextActivity activity, final HeaderData header) {
            titleView.setText(Utils.capitalize(header.header));
            if (header.numEvents <= 0) {
                numEventsView.setVisibility(View.GONE);
            } else {
                numEventsView.setVisibility(View.VISIBLE);
                numEventsView.setText(
                        String.format(activity.getString(R.string.num_events), header.numEvents));
            }

            if (header.showMore()) {
                moreView.setVisibility(View.VISIBLE);
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.showSearchView(header.header);
                    }
                });
            } else {
                moreView.setVisibility(View.GONE);
                itemView.setClickable(false);
            }
        }
    }

    // Event Data.
    private class EventData implements Data {
        private final String header;
        private final Event event;
        private final boolean isFirstEvent;

        public EventData(String header, Event event, boolean isFirstEvent) {
            this.header = header;
            this.event = event;
            this.isFirstEvent = isFirstEvent;
        }

        @Override
        public DataType getType() {
            return DataType.EVENT;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            ((EventCard) card).bindEventView(event, isFirstEvent, activity, position);
        }

        public String getId() {
            return header + ":" + event.id;
        }
    }

    private static class EventCard extends ViewHolder {
        private final boolean shouldAdjustImageHeight;
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
        private final View arrowView;

        private static EventCard newInstance(Activity activity, ViewGroup parent, boolean bigLayout) {
            View view = activity.getLayoutInflater().inflate(
                    bigLayout ? R.layout.card_event_big : R.layout.card_event_maps, parent, false);
            return new EventCard(view, false);
        }

        public EventCard(View cardView, boolean shouldAdjustImageHeight) {
            super(cardView);

            this.shouldAdjustImageHeight = shouldAdjustImageHeight;
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
            arrowView = cardView.findViewById(R.id.arrow);
        }

        public void setFavouriteView(@Nullable EventMark eventMark) {
            favouriteView.setTag(eventMark);
            favouriteView.setImageResource(EventMark.isFavourite(eventMark) ?
                    R.drawable.ic_favorite_red_18dp : R.drawable.ic_favorite_grey600_24dp);
        }

        private void addView(List<Pair<View, String>> sharedElements, @Nullable View view,
                             String shareName) {
            if (view != null && view.getVisibility() == View.VISIBLE) {
                sharedElements.add(Pair.create(view, shareName));
            }
        }

        private void bindEventView(final Event event, boolean isFirstEvent,
                                   final BaseContextActivity activity, final int position) {
            itemView.setTag(position);
            itemView.setVisibility(View.VISIBLE);
            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    List<Pair<View, String>> sharedElements = new ArrayList<>();
                    addView(sharedElements, bgView, "event_bg");
                    addView(sharedElements, titleView, "event_title");
                    addView(sharedElements, eventTimeView, "event_time");
                    addView(sharedElements, venueView, "event_venue");
                    addView(sharedElements, travelTimeView, "event_travel_time");
                    // addView(sharedElements, priceView, "event_price");
                    // addView(sharedElements, favouriteView, "action_favourite");
                    addView(sharedElements, recommendedView, "eh_recommends");
                    addView(sharedElements, fbShareView, "share_fb");
                    addView(sharedElements, whatsappShareView, "share_whatsapp");
                    Pair shareEles[] = new Pair[sharedElements.size()];
                    shareEles = sharedElements.toArray(shareEles);
                    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            activity, shareEles).toBundle();
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

            if (arrowView != null) {
                arrowView.setVisibility(isFirstEvent ? View.VISIBLE : View.GONE);
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
                    eventTimeView.setText(eventTime.toString());
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

    // Trending Category and Explore Category.
    private class TrendingCategoryData implements Data {
        private final TrendingTopic trendingTopic;

        private TrendingCategoryData(TrendingTopic trendingTopic) {
            this.trendingTopic = trendingTopic;
        }

        @Override
        public DataType getType() {
            return DataType.TRENDING_CATEGORY;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, final int position) {
            ((TrendingCategoryCard) card).populateTrendingCategoryData(this, activity);
        }

        public String getId() {
            return trendingTopic.tagName;
        }
    }

    private class ExploreCategoryData implements Data {
        private final String tag;

        private ExploreCategoryData(String tag) {
            this.tag = tag;
        }

        @Override
        public DataType getType() {
            return DataType.EXPLORE_CATEGORY;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, final int position) {
            ((TrendingCategoryCard) card).populateExploreCategoryData(this, activity);
        }

        public String getId() {
            return tag;
        }

        public int getInfoGraphId() {
            try {
                return R.drawable.class.getField("infograph_" +
                        EventCategory.toCategoryParsableString(tag).toLowerCase()).getInt(null);
            } catch (IllegalAccessException e) {
                // Ignore
            } catch (NoSuchFieldException e) {
                // Ignore
                Log.d("", "No image for: " + tag, e);
            }

            return R.drawable.eh_default_event;
        }
    }

    private static class TrendingCategoryCard extends ViewHolder {
        private NetworkImageView imageView;
        private TextView titleView;

        static TrendingCategoryCard newInstance(final BaseActivity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_explore, parent, false);
            return new TrendingCategoryCard(view);
        }

        public TrendingCategoryCard(View itemView) {
            super(itemView);

            imageView = (NetworkImageView) itemView.findViewById(R.id.image);
            titleView = (TextView) itemView.findViewById(R.id.title);
        }

        public void populateTrendingCategoryData(final TrendingCategoryData data,
                                                 final BaseContextActivity activity) {
            imageView.setImageUrl(data.trendingTopic.imgUrl, VolleyHelper.getImageLoader(activity));
            titleView.setText(data.trendingTopic.tagName);
            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.trendingTopic.launch(activity);
                }
            });

            Utils.waitForViewVisible(imageView, new Runnable() {
                @Override
                public void run() {
                    LayoutParams lp = imageView.getLayoutParams();
                    lp.height = 3 * imageView.getWidth() / 4;
                    imageView.setLayoutParams(lp);
                }
            });
        }

        public void populateExploreCategoryData(final ExploreCategoryData data,
                                                final BaseContextActivity activity) {
            imageView.setDefaultImageResId(data.getInfoGraphId());
            titleView.setVisibility(View.GONE);
            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.showSearchView(data.tag);
                }
            });

            Utils.waitForViewVisible(imageView, new Runnable() {
                @Override
                public void run() {
                    LayoutParams lp = imageView.getLayoutParams();
                    lp.height = imageView.getWidth();
                    imageView.setLayoutParams(lp);
                }
            });
        }
    }
}
