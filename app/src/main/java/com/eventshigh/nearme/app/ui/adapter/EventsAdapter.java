package com.eventshigh.nearme.app.ui.adapter;

import android.graphics.Movie;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.MyTicketsFragment;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.Locality;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MovieInfoObject;
import com.eventshigh.nearme.app.data.MovieReviewObject;
import com.eventshigh.nearme.app.data.MovieShowTimeObject;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.data.MyTicketObject;
import com.eventshigh.nearme.app.data.ShowDates;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.data.TrendingTopic;
import com.eventshigh.nearme.app.data.stream.OfferObject;
import com.eventshigh.nearme.app.data.stream.PointsObject;
import com.eventshigh.nearme.app.network.EventInvitationsRequest.EventInvitation;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest.EventCollection;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
import com.eventshigh.nearme.app.network.MyPointsBreakdownRequest;
import com.eventshigh.nearme.app.network.SocialActionsRequest.SocialActions;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest.SocialInvite;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView.SpanAllColumnLookup;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An adapter which can be used to populate the Event card.
 */
public class EventsAdapter extends RecyclerView.Adapter<ViewHolder> implements SpanAllColumnLookup, SocialDataProvider {
    private final BaseContextActivity activity;
    private final Map<String, Integer> eventIdToItemIdMap = Utils.getMap();
    private final Set<Integer> usedItemIds = new HashSet<>();
    private List<AdapterData> dataToShow;
    @Nullable
    private SocialActions socialActions;
    @Nullable
    private Map<String, SocialInvite> socialInvites;

    OnEditClickListener mListener;

    OnItemClickedListener pListener;

    OnMyTicketItemClickedListener myTicketListener;

    public EventsAdapter(BaseContextActivity activity) {
        this.activity = activity;

        dataToShow = new ArrayList<>();
        setHasStableIds(true);
    }

    public void setSocialActions(@Nullable SocialActions socialActions) {
        this.socialActions = socialActions;
        notifyDataSetChanged();
    }

    public void setSocialInvites(@Nullable Map<String, SocialInvite> socialInvites) {
        this.socialInvites = socialInvites;
        notifyDataSetChanged();
    }

    public void setEvents(List<Event> events, @Nullable String categoryForSeeAll,
                          boolean showEhInviteForNotification) {
        dataToShow.clear();
        for (Event event : events) {
            dataToShow.add(new EventData("", event, false, activity, this));
        }
        if (categoryForSeeAll != null) {
            dataToShow.add(new SeeAllData(activity, categoryForSeeAll));
        }
        if (showEhInviteForNotification) {
            dataToShow.add(new EhInviteNotificationData(activity));
        }

        notifyDataSetChanged();
    }

    public void setMoviesListData(List<MovieDetailObject> objs, EventsContext eventsContext, boolean addHeader, boolean clearOldData) {
        if (clearOldData)
            dataToShow.clear();
        if (addHeader) {
            dataToShow.add(new HeaderData(activity, eventsContext, MyEventsRequest.MOVIES_NAME, objs.size(), HeaderData.TYPE_MOVIE));
        }
        for (MovieDetailObject obj : objs) {
            dataToShow.add(new MovieListData(obj, activity));
        }
        notifyDataSetChanged();
    }


    public void setMyTicketsData(List<MyTicketObject> objs, MyTicketsFragment myTicketsFragment){
        for (MyTicketObject obj : objs) {
            dataToShow.add(new MyTicketData(obj, activity, myTicketsFragment));
        }
        notifyDataSetChanged();
    }


    public void setOffers(ArrayList<OfferObject> offers, long totalPoints) {
        dataToShow.clear();
        for (OfferObject offer : offers) {
            dataToShow.add(new OfferData(offer, activity, totalPoints));
        }
        dataToShow.add(0, new TotalPointsHeaderData(totalPoints, activity, false, true, false));
        notifyDataSetChanged();
    }

    public void setPoints(ArrayList<PointsObject> points) {
        dataToShow.clear();
        int totalPoints = 0;
        for (PointsObject point : points) {
            totalPoints += point.points;
            dataToShow.add(new PointsData(point, activity));
        }

        dataToShow.add(new TotalPointsHeaderData(totalPoints, activity, true, false, false));

        notifyDataSetChanged();
    }

    public void setTopicEvents(List<TopicEvents> topicEvents, EventsContext eventsContext, int maxPerCategory) {
        dataToShow.clear();

        for (int i = 0; i < topicEvents.size(); i++) {
            TopicEvents topicEvent = topicEvents.get(i);
            List<Event> events = topicEvent.events;
            if (events.isEmpty()) {
                continue;
            }

            boolean isFavourite = MyEventsRequest.isSpecialTag(topicEvent.topicName);
            if (!isFavourite && events.size() > maxPerCategory) {
                events = events.subList(0, maxPerCategory);
            }

            dataToShow.add(new HeaderData(activity, eventsContext, topicEvent.topicName, topicEvent.numEvents, HeaderData.TYPE_EVENT));
            boolean isFirstEvent = true;
            for (Event event : events) {
                dataToShow.add(new EventData(topicEvent.topicName, event, isFirstEvent, activity, this));
                isFirstEvent = false;
            }
        }

        notifyDataSetChanged();
    }

    public void setEventInfoObject(Event event) {
        dataToShow.clear();
        dataToShow.add(new EventInfoData(event, activity));
        notifyDataSetChanged();

    }

    public void setInfoObject(MovieInfoObject obj) {
        dataToShow.clear();
        dataToShow.add(new MovieInfoData(obj, activity));
        notifyDataSetChanged();
    }

    public void setMovieReviews(ArrayList<MovieReviewObject> objs) {
        dataToShow.clear();
        for (MovieReviewObject obj : objs) {
            dataToShow.add(new MovieReviewData(obj, activity));
        }
        notifyDataSetChanged();
    }


    public void setUserMovieReviews(ArrayList<MovieUserReviewObject> objs, String reviewForId) {
        dataToShow.clear();

        for (MovieUserReviewObject obj : objs) {
            dataToShow.add(new MovieUserReviewData(obj, activity, reviewForId));
        }
        notifyDataSetChanged();
    }

    public void setMovieShowTimes(ArrayList<MovieShowTimeObject> objs) {
        dataToShow.clear();
        for (MovieShowTimeObject obj : objs) {
            dataToShow.add(new ShowTimeData(obj, activity));
        }
        notifyDataSetChanged();
    }


    public void addPointsBreakDown(MyPointsBreakdownRequest.PointBreakdownBaseObj objs) {
        dataToShow.clear();
        for (MyPointsBreakdownRequest.PointBreakDown obj : objs.points) {
            dataToShow.add(new PointBreakdownData(obj, activity));
        }
        dataToShow.add(0, new TotalPointsHeaderData(objs.totalPoints, activity, false, false, true));
        notifyDataSetChanged();

    }

    public void setNewExploreCategories(@Nullable EventCollection eventCollection, String[] tags) {
        dataToShow.clear();
        if (eventCollection != null) {
            if (!eventCollection.events.isEmpty()) {
                dataToShow.add(new EventPagerData(activity, eventCollection.showReferrer,
                        eventCollection.events));
            }
        }
        dataToShow.add(new ExploreCategoryHeaderData(activity));

        for (String tag : tags) {
            dataToShow.add(new NewExploreCategoryData(tag, activity, this));
        }

        notifyDataSetChanged();
    }

    public void setExploreCategories(@Nullable EventCollection eventCollection,
                                     List<Locality> localities, String[] tags, String movies) {
        dataToShow.clear();

        if (eventCollection != null) {
            if (!eventCollection.events.isEmpty()) {
                dataToShow.add(new EventPagerData(activity, eventCollection.showReferrer,
                        eventCollection.events));
            }
            if (!eventCollection.trendingTopics.isEmpty()) {
                dataToShow.add(new SmallHeaderData(activity.getString(R.string.ui_browse_featured)));
                for (TrendingTopic trendingTopic : eventCollection.trendingTopics) {
                    dataToShow.add(new TrendingCategoryData(trendingTopic, activity, this));
                }
            }
        }

        if (!localities.isEmpty()) {
            dataToShow.add(new SmallHeaderData(activity, activity.getString(R.string.ui_browse_loc), true, 0));
            for (int i = 0; i < localities.size(); i++) {
                dataToShow.add(new LocalityData(localities.get(i), activity, getMaterialColor(i)));
            }
        }

        dataToShow.add(new SmallHeaderData(activity.getString(R.string.ui_browse_cat)));

        dataToShow.add(new MovieCategoryData("movies", activity, this));
        for (String tag : tags) {
            dataToShow.add(new ExploreCategoryData(tag, activity, this));
        }
        notifyDataSetChanged();
    }

    public int getMaterialColor(int i) {
        switch (i) {
            case 0:
                return R.color.material_color_orange;
            case 1:
                return R.color.material_color_pink;
            case 2:
                return R.color.material_color_green;
            case 3:
                return R.color.material_color_purple;
            case 4:
                return R.color.material_color_blue;
            case 5:
                return R.color.material_color_red;

        }
        return R.color.material_color_green;
    }

    public void addEventInvitations(List<EventInvitation> invites, List<SocialInvitationsRequest.SpecialCoupons> specials) {
        int pos = dataToShow.get(0).getType() == DataType.EVENT_PAGER ? 1 : 0;


        for (SocialInvitationsRequest.SpecialCoupons special : specials) {
            dataToShow.add(pos, new EventInvitationData(special, activity));
        }
        if (!specials.isEmpty()) {
            dataToShow.add(pos, new SmallHeaderData("Specials"));
        }
        for (EventInvitation invite : invites) {
            dataToShow.add(pos, new EventInvitationData(invite, activity));
        }
        if (!invites.isEmpty()) {
            dataToShow.add(pos, new SmallHeaderData("Invitations"));
        }


        notifyDataSetChanged();
    }

    public void addFollowCard(String title, int numEvents, int numFollowers, SocialInvitationsRequest.SpecialCoupons coupon) {
        dataToShow.add(0, new FollowData(title, numEvents, numFollowers, activity, this, coupon));
    }

    @Override
    public
    @Nullable
    Set<SocialFriend> getFollowers(String tag) {
        return socialActions == null ? null : socialActions.getTagFollowers(tag);
    }

    @Override
    public
    @Nullable
    SocialInvite getSocialInvite(String eventId) {
        return socialInvites == null ? null : socialInvites.get(eventId);
    }

    @Override
    public boolean spanAllColumns(int position) {
        return !(position >= dataToShow.size()) && DataType.spanAllColumns(getItemViewType(position));
    }

    @Override
    public int getItemViewType(int position) {
        return dataToShow.get(position).getType().typeId;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int type){
        return DataType.onCreateViewHolder(activity, viewGroup, type);
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        if (card instanceof SmallHeaderCard) {
            ((SmallHeaderData) dataToShow.get(position)).onBindViewHolder(card, position, mListener);
        } else if (card instanceof EventCard) {
            ((EventData) dataToShow.get(position)).onBindViewHolder(card, position, pListener);
        } else if (card instanceof MovieReviewCard) {
            ((MovieReviewData) dataToShow.get(position)).onBindViewHolder(card, position);
        } else if (card instanceof MovieUserReviewCard) {
            ((MovieUserReviewData) dataToShow.get(position)).onBindViewHolder(card, position);
        }else if (card instanceof MyTicketCard){
            ((MyTicketData) dataToShow.get(position)).onBindViewHolder(card, position,myTicketListener);
        }else{
            dataToShow.get(position).onBindViewHolder(card, position);
        }
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

    public void clear() {
        dataToShow.clear();
        notifyDataSetChanged();
    }

    public void setOnEditClickListener(OnEditClickListener listener) {
        this.mListener = listener;

    }

    public void setOnMyTicketClickListener(OnMyTicketItemClickedListener listener) {
        this.myTicketListener = listener;

    }

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        this.pListener = listener;
    }


    public interface OnEditClickListener {
        void onEditcliked();
    }


    public interface OnItemClickedListener {
        void onItemClicked(int pos);
    }

    public interface OnMyTicketItemClickedListener{
        void onItemClicked(int pos);
    }

}
