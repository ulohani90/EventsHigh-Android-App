package com.eventshigh.nearme.app.ui;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.BaseEventsFragment;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.data.Locality;
import com.eventshigh.nearme.app.data.TrendingTopic;
import com.eventshigh.nearme.app.data.UserContact;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest.EventCollection;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
import com.eventshigh.nearme.app.network.SocialActionsRequest.SocialActions;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.FontUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.FollowedByView;

import java.text.MessageFormat;
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
    private final BaseEventsFragment eventsFragment;
    private final Map<String, Integer> eventIdToItemIdMap = new HashMap<>();
    private final Set<Integer> usedItemIds = new HashSet<>();
    private List<Data> dataToShow;
    @Nullable private SocialActions socialActions;

    public EventsAdapter(BaseEventsFragment eventsFragment) {
        this.eventsFragment = eventsFragment;

        dataToShow = new ArrayList<>();
        setHasStableIds(true);
    }

    public void setSocialActions(@Nullable SocialActions socialActions) {
        this.socialActions = socialActions;
        notifyDataSetChanged();
    }

    public void setEvents(List<Event> events, @Nullable String categoryForSeeAll) {
        dataToShow.clear();
        for (Event event: events) {
            dataToShow.add(new EventData("", event, false));
        }
        if (categoryForSeeAll != null) {
            dataToShow.add(new SeeAllData(categoryForSeeAll));
        }
        notifyDataSetChanged();
    }

    public void setTopicEvents(List<TopicEvents> topicEvents, int maxPerCategory) {
        dataToShow.clear();

        for (int i = 0; i < topicEvents.size(); i++) {
            TopicEvents topicEvent = topicEvents.get(i);
            List<Event> events = topicEvent.events;
            if (events.isEmpty()) {
                continue;
            }

            boolean isFavourite = topicEvent.topicName.equals(MyEventsRequest.FAVOURITES_NAME);
            if (!isFavourite && events.size() > maxPerCategory) {
                events = events.subList(0, maxPerCategory);
            }

            dataToShow.add(new HeaderData(topicEvent.topicName, topicEvent.numEvents));
            boolean isFirstEvent = true;
            for (Event event : events) {
                dataToShow.add(new EventData(topicEvent.topicName, event, isFirstEvent));
                isFirstEvent = false;
            }
        }

        notifyDataSetChanged();
    }

    public void setExploreCategories(@Nullable EventCollection eventCollection,
            List<Locality> localities, String[] tags) {
        dataToShow.clear();

        if (eventCollection != null) {
            if (!eventCollection.events.isEmpty()) {
                dataToShow.add(new EventPagerData(eventCollection.events));
            }
            if (!eventCollection.trendingTopics.isEmpty()) {
                dataToShow.add(new SmallHeaderData(
                        eventsFragment.getContextActivity().getString(R.string.ui_browse_featured)));
                for (TrendingTopic trendingTopic : eventCollection.trendingTopics) {
                    dataToShow.add(new TrendingCategoryData(trendingTopic));
                }
            }
        }

        if (!localities.isEmpty()) {
            dataToShow.add(new SmallHeaderData(
                    eventsFragment.getContextActivity().getString(R.string.ui_browse_loc)));
            for (Locality locality : localities) {
                dataToShow.add(new TrendingCategoryData(locality.asTrendingTopic()));
            }
        }

        dataToShow.add(new SmallHeaderData(
            eventsFragment.getContextActivity().getString(R.string.ui_browse_cat)));
        for (String tag : tags) {
            dataToShow.add(new ExploreCategoryData(tag));
        }
        notifyDataSetChanged();
    }

    public void addFollowCard(String title, int numEvents, int numFollowers) {
        dataToShow.add(0, new FollowData(title, numEvents, numFollowers));
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
        return DataType.onCreateViewHolder(eventsFragment.getContextActivity(), viewGroup, type);
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
    public static View getEventCard(final Event event, final BaseContextActivity activity,
                                    @Nullable View reuseView, ViewGroup parent) {
        View view = reuseView != null ? reuseView :
                activity.getLayoutInflater().inflate(R.layout.card_event_big, parent, false);
        new EventCard(view, true).bindEventView(event, false, -1, null, activity);
        return view;
    }

    public @Nullable Set<UserContact> getFollowers(String tag) {
        return socialActions == null ? null :
                socialActions.tagFollowers.get(EventCategory.toCategoryParsableString(tag));
    }

    private enum DataType {
        HEADER(0),
        EVENT(1),
        FOLLOW(3),
        TRENDING_CATEGORY(4),
        EXPLORE_CATEGORY(5),
        SMALL_HEADER(6),
        EVENT_PAGER(7),
        SEE_ALL(8);

        public final int typeId;
        DataType (int typeId) {
            this.typeId = typeId;
        }

        public static boolean spanAllColumns (int typeId) {
            return  typeId == HEADER.typeId || typeId == SMALL_HEADER.typeId
                    || typeId == EVENT_PAGER.typeId || typeId == SEE_ALL.typeId;
        }

        public static ViewHolder onCreateViewHolder(BaseActivity activity, ViewGroup parent, int typeId) {
            if (typeId == HEADER.typeId) {
                return HeaderCard.newInstance(activity, parent);
            }

            if (typeId == EVENT.typeId) {
                return EventCard.newInstance(activity, parent);
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

            if (typeId == SMALL_HEADER.typeId) {
                return SmallHeaderCard.newInstance(activity, parent);
            }

            if (typeId == EVENT_PAGER.typeId) {
                return EventPagerCard.newInstance(activity, parent);
            }

            if (typeId == SEE_ALL.typeId) {
                return SeeAllCard.newInstance(activity, parent);
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
            ((HeaderCard) card).bindHeaderView(eventsFragment, this);
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

        private void bindHeaderView(final BaseEventsFragment eventsFragment, final HeaderData header) {
            titleView.setText(header.header);
            FontUtils.setTypefaceQuicksandBold(titleView);
            if (header.numEvents <= 0) {
                numEventsView.setVisibility(View.GONE);
            } else {
                numEventsView.setVisibility(View.VISIBLE);
                numEventsView.setText(MessageFormat.format(
                    eventsFragment.getContextActivity().getString(R.string.num_events),
                    0, header.numEvents));
            }

            if (header.showMore()) {
                moreView.setVisibility(View.VISIBLE);
                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        eventsFragment.showSearchView(header.header);
                    }
                });
            } else {
                moreView.setVisibility(View.GONE);
                itemView.setClickable(false);
            }
        }
    }

    // Small Header.
    private class SmallHeaderData implements Data {
        private final String header;

        private SmallHeaderData(String header) {
            this.header = header;
        }

        @Override
        public DataType getType() {
            return DataType.SMALL_HEADER;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            ((SmallHeaderCard) card).bindHeaderView(this);
        }

        @Override
        public String getId() {
            return header;
        }
    }

    private static class SmallHeaderCard extends ViewHolder {
        private final TextView titleView;

        private static SmallHeaderCard newInstance(Activity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_header_small, parent, false);
            return new SmallHeaderCard(view);
        }

        private SmallHeaderCard(View cardView) {
            super(cardView);
            this.titleView = (TextView) cardView.findViewById(R.id.header);
        }

        private void bindHeaderView(final SmallHeaderData header) {
            titleView.setText(header.header);
        }
    }

    // Small Header.
    private class SeeAllData implements Data {
        private final String category;

        private SeeAllData(String category) {
            this.category = category;
        }

        @Override
        public DataType getType() {
            return DataType.SEE_ALL;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            ((SeeAllCard) card).bindHeaderView(eventsFragment, this);
        }

        @Override
        public String getId() {
            return category;
        }
    }

    private static class SeeAllCard extends ViewHolder {
        private final TextView seeAllView;

        private static SeeAllCard newInstance(Activity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_see_all, parent, false);
            return new SeeAllCard(view);
        }

        private SeeAllCard(View cardView) {
            super(cardView);
            this.seeAllView = (TextView) cardView.findViewById(R.id.see_all);
        }

        private void bindHeaderView(final BaseEventsFragment eventsFragment, final SeeAllData seeAllData) {
            seeAllView.setText("See All " + Utils.capitalize(seeAllData.category) + " Events");
            seeAllView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    eventsFragment.seeAll();
                }
            });
        }
    }

    // Event Pager.
    private class EventPagerData implements Data {
        private final List<Event> events;

        private EventPagerData(List<Event> events) {
            this.events = events;
        }

        @Override
        public DataType getType() {
            return DataType.EVENT_PAGER;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            ((EventPagerCard) card).bindHeaderView(eventsFragment.getContextActivity(), this);
        }

        @Override
        public String getId() {
            return events.get(0).id;
        }
    }

    private static class EventPagerCard extends ViewHolder {
        private final ViewPager viewPager;
        private final LinearLayout dotsView;

        private static EventPagerCard newInstance(Activity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_event_pager, parent, false);
            return new EventPagerCard(view);
        }

        private EventPagerCard(View cardView) {
            super(cardView);
            viewPager =  (ViewPager) cardView.findViewById(R.id.events_pager);
            dotsView = (LinearLayout) cardView.findViewById(R.id.dots_parent);
        }

        private void bindHeaderView(BaseContextActivity activity, EventPagerData eventPagerData) {
            viewPager.setAdapter(new FeaturedEventsAdapter(activity, eventPagerData.events));

            dotsView.removeAllViews();
            for (int i = 0; i < eventPagerData.events.size(); i++) {
                View view = activity.getLayoutInflater().inflate(R.layout.view_dot_featured, dotsView, false);
                view.setSelected(i == 0);
                dotsView.addView(view);
            }

            viewPager.clearOnPageChangeListeners();
            viewPager.addOnPageChangeListener(new DotsSelector(activity));
        }

        private class DotsSelector implements OnPageChangeListener {
            private final BaseContextActivity activity;

            private DotsSelector(BaseContextActivity activity) {
                this.activity = activity;
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // do nothing.
            }

            @Override
            public void onPageSelected(int position) {
                if (position != 0) {
                    activity.reportActionToAnalytics("featuredSwipe");
                }
                for (int i = 0; i < dotsView.getChildCount(); i++) {
                    dotsView.getChildAt(i).setSelected(i == position);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // do nothing.
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
            ((EventCard) card).bindEventView(event, isFirstEvent, position, eventsFragment,
                    eventsFragment.getContextActivity());
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
        private final TextView numEventsViews;
        private final ImageView favouriteView;
        private final View arrowView;

        private static EventCard newInstance(Activity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(
                    BuildConfig.SHOW_BIG_CARD ? R.layout.card_event_big : R.layout.card_event,
                    parent, false);
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
            numEventsViews = (TextView) cardView.findViewById(R.id.num_pv);
            favouriteView = (ImageView) cardView.findViewById(R.id.action_favourite);
            arrowView = cardView.findViewById(R.id.arrow);
        }

        public void setFavouriteView(@Nullable EventMark eventMark) {
            favouriteView.setTag(eventMark);
            favouriteView.setImageResource(EventMark.isFavourite(eventMark) ?
                    R.drawable.ic_favorite_red_18dp : R.drawable.ic_favorite_white_18dp);
        }

        private void bindEventView(final Event event, boolean isFirstEvent, final int position,
                @Nullable final BaseEventsFragment eventsFragment, final BaseContextActivity activity) {
            itemView.setTag(position);
            itemView.setVisibility(View.VISIBLE);
            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (eventsFragment != null) {
                        eventsFragment.showEventDetails(event, null);
                    } else {
                        activity.showEventDetails(event, "", null);
                    }
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

            // Num people interested ?
            if (numEventsViews != null) {
                if (event.numViews > 1) {
                    numEventsViews.setVisibility(View.VISIBLE);
                    numEventsViews.setText(Integer.toString(event.numViews));
                } else {
                    numEventsViews.setVisibility(View.GONE);
                }
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

        }
    }

    // Follow Data.
    private class FollowData implements Data {
        private final String title;
        private final int numEvents;
        private final int numFollowers;

        private FollowData(String title, int numEvents, int numFollowers) {
            this.title = title;
            this.numEvents = numEvents;
            this.numFollowers = numFollowers;
        }

        @Override
        public DataType getType() {
            return DataType.FOLLOW;
        }

        @Override
        public void onBindViewHolder(ViewHolder card, int position) {
            ((FollowCard) card).populate(this, eventsFragment.getContextActivity(), getFollowers(title));
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
        private FollowedByView followedByView;

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
            followedByView = (FollowedByView) itemView.findViewById(R.id.followed_by);
        }

        public void populate(final FollowData data, final BaseContextActivity activity,
                @Nullable Set<UserContact> followers) {
            titleView.setText(data.title);
            subtitleView.setText(MessageFormat.format(
                    activity.getString(R.string.num_events), data.numFollowers, data.numEvents));

            final Account account = new Account(activity);
            setFollowButtons(account.isFollowing(data.title));
            followButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportActionToAnalytics("addFollowing", data.title);
                    if (!account.getPhoneNumber().second) {
                        PhoneVerificationDialog.show(activity,
                                R.string.ui_verify_phone, R.string.ui_phone_verify_pa);
                    }
                    account.setIsFollowing(data.title, true);
                    setFollowButtons(true);
                }
            });
            followingButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportActionToAnalytics("removeFollowing", data.title);
                    account.setIsFollowing(data.title, false);
                    setFollowButtons(false);
                }
            });

            followedByView.setFollowers(activity, followers, "Followed By: ", Gravity.CENTER);
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
            ((TrendingCategoryCard) card).populateTrendingCategoryData(this, eventsFragment,
                getFollowers(trendingTopic.tagName));
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
            ((TrendingCategoryCard) card).populateExploreCategoryData(this, eventsFragment,
                getFollowers(tag));
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
                Crashlytics.getInstance().core.logException(e);
            } catch (NoSuchFieldException e) {
                // Ignore
                Crashlytics.getInstance().core.logException(e);
                Log.d("", "No image for: " + tag, e);
            }

            return R.drawable.eh_default_event;
        }
    }

    private static class TrendingCategoryCard extends ViewHolder {
        private NetworkImageView imageView;
        private TextView titleView;
        private FollowedByView followedByView;

        static TrendingCategoryCard newInstance(final BaseActivity activity, ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_explore, parent, false);
            return new TrendingCategoryCard(view);
        }

        public TrendingCategoryCard(View itemView) {
            super(itemView);

            imageView = (NetworkImageView) itemView.findViewById(R.id.image);
            titleView = (TextView) itemView.findViewById(R.id.title);
            followedByView = (FollowedByView) itemView.findViewById(R.id.followed_by);
        }

        public void populateTrendingCategoryData(final TrendingCategoryData data,
                final BaseEventsFragment eventsFragment, @Nullable Set<UserContact> followers) {
            imageView.setImageUrl(data.trendingTopic.imgUrl,
                    VolleyHelper.getImageLoader(eventsFragment.getContextActivity()));
            titleView.setText(data.trendingTopic.tagName);
            followedByView.setFollowers(eventsFragment.getContextActivity(), followers, null,
                    Gravity.START);

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) followedByView.getLayoutParams();
            lp.gravity = Gravity.BOTTOM;
            followedByView.setLayoutParams(lp);

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.trendingTopic.launch(eventsFragment);
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
                final BaseEventsFragment eventsFragment, @Nullable Set<UserContact> followers) {
            imageView.setDefaultImageResId(data.getInfoGraphId());
            titleView.setVisibility(View.GONE);
            followedByView.setFollowers(eventsFragment.getContextActivity(), followers, null,
                    Gravity.END);

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) followedByView.getLayoutParams();
            lp.gravity = Gravity.TOP;
            followedByView.setLayoutParams(lp);

            itemView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    eventsFragment.showSearchView(data.tag);
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
