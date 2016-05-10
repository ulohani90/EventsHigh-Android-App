package com.eventshigh.nearme.app.ui.adapter;

import android.graphics.Movie;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.Locality;

import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MovieInfoObject;
import com.eventshigh.nearme.app.data.MovieReviewObject;
import com.eventshigh.nearme.app.data.MovieShowTimeObject;
import com.eventshigh.nearme.app.data.ShowDates;

import com.eventshigh.nearme.app.data.TrendingTopic;
import com.eventshigh.nearme.app.data.stream.OfferObject;
import com.eventshigh.nearme.app.data.stream.PointsObject;
import com.eventshigh.nearme.app.network.EventInvitationsRequest.EventInvitation;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest.EventCollection;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;

import com.eventshigh.nearme.app.network.MyPointsBreakdownRequest;

import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
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
public class EventsAdapter extends RecyclerView.Adapter<ViewHolder> implements SpanAllColumnLookup {
    private final BaseContextActivity activity;
    private final Map<String, Integer> eventIdToItemIdMap = Utils.getMap();
    private final Set<Integer> usedItemIds = new HashSet<>();
    private List<AdapterData> dataToShow;


    OnEditClickListener mListener;

    OnItemClickedListener pListener;

    public EventsAdapter(BaseContextActivity activity) {
        this.activity = activity;

        dataToShow = new ArrayList<>();
        setHasStableIds(true);
    }

    public void setEvents(List<Event> events, @Nullable String categoryForSeeAll) {
        dataToShow.clear();

        for (Event event: events) {
            dataToShow.add(new EventData("", event, false, activity));

        }
        if (categoryForSeeAll != null) {
            dataToShow.add(new SeeAllData(activity, categoryForSeeAll));
        }

        notifyDataSetChanged();
    }

    public void setMoviesListData(ArrayList<MovieDetailObject> objs) {
        dataToShow.clear();
        for (MovieDetailObject obj : objs) {
            dataToShow.add(new MovieListData(obj, activity));
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

            dataToShow.add(new HeaderData(activity, eventsContext, topicEvent.topicName, topicEvent.numEvents));
            boolean isFirstEvent = true;
            for (Event event : events) {
                dataToShow.add(new EventData(topicEvent.topicName, event, isFirstEvent, activity));
                isFirstEvent = false;
            }
        }

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

    public void setExploreCategories(@Nullable EventCollection eventCollection,
                                     List<Locality> localities, String[] tags) {
        dataToShow.clear();

        if (eventCollection != null) {
            if (!eventCollection.events.isEmpty()) {
                dataToShow.add(new EventPagerData(activity, eventCollection.showReferrer,
                        eventCollection.events));
            }
            if (!eventCollection.trendingTopics.isEmpty()) {
                dataToShow.add(new SmallHeaderData(activity.getString(R.string.ui_browse_featured)));
                for (TrendingTopic trendingTopic : eventCollection.trendingTopics) {
                    dataToShow.add(new TrendingCategoryData(trendingTopic, activity));
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
        for (String tag : tags) {
            dataToShow.add(new ExploreCategoryData(tag, activity));
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


    public void addFollowCard(String title, int numEvents, int numFollowers,SocialInvitationsRequest.SpecialCoupons coupon) {
        dataToShow.add(0, new FollowData(title, numEvents, numFollowers, activity, coupon));
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
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
        return DataType.onCreateViewHolder(activity, viewGroup, type);
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        if (card instanceof SmallHeaderCard) {
            ((SmallHeaderData) dataToShow.get(position)).onBindViewHolder(card, position, mListener);
        } else if (card instanceof EventCard) {
            ((EventData) dataToShow.get(position)).onBindViewHolder(card, position, pListener);
        } else {
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

    public void setOnItemClickedListener(OnItemClickedListener listener) {
        this.pListener = listener;
    }


    public interface OnEditClickListener {
        void onEditcliked();
    }


    public interface OnItemClickedListener {
        void onItemClicked(int pos);
    }
}
