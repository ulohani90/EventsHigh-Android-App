package com.eventshigh.nearme.app.ui.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.Locality;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.data.TrendingTopic;
import com.eventshigh.nearme.app.network.EventInvitationsRequest.EventInvitation;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest.EventCollection;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
import com.eventshigh.nearme.app.network.SocialActionsRequest.SocialActions;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest.SocialInvite;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView.SpanAllColumnLookup;

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
    @Nullable private SocialActions socialActions;
    @Nullable private Map<String, SocialInvite> socialInvites;

    OnEditClickListener mListener;

    OnItemClickedListener pListener;

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
        for (Event event: events) {
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
                dataToShow.add(new EventData(topicEvent.topicName, event, isFirstEvent, activity, this));
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
            dataToShow.add(new SmallHeaderData(activity , activity.getString(R.string.ui_browse_loc),true));
            for (int i=0;i<localities.size();i++) {
                dataToShow.add(new LocalityData(localities.get(i),activity,getMaterialColor(i)));
            }
        }

        dataToShow.add(new SmallHeaderData(activity.getString(R.string.ui_browse_cat)));
        for (String tag : tags) {
            dataToShow.add(new ExploreCategoryData(tag, activity, this));
        }
        notifyDataSetChanged();
    }

    public int getMaterialColor(int i) {
        switch(i){
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

    public void addEventInvitations(List<EventInvitation> invites) {
        int pos = dataToShow.get(0).getType() == DataType.EVENT_PAGER ? 1 : 0;
        for (EventInvitation invite : invites) {
            dataToShow.add(pos, new EventInvitationData(invite, activity));
        }
        if (! invites.isEmpty()) {
            dataToShow.add(pos, new SmallHeaderData("Invitations"));
        }
        notifyDataSetChanged();
    }

    public void addFollowCard(String title, int numEvents, int numFollowers) {
        dataToShow.add(0, new FollowData(title, numEvents, numFollowers, activity, this));
    }

    @Override
    public @Nullable Set<SocialFriend> getFollowers(String tag) {
        return socialActions == null ? null : socialActions.getTagFollowers(tag);
    }

    @Override
    public @Nullable SocialInvite getSocialInvite(String eventId) {
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
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
        return DataType.onCreateViewHolder(activity, viewGroup, type);
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        if (card instanceof SmallHeaderCard) {
            ((SmallHeaderData)dataToShow.get(position)).onBindViewHolder(card, position, mListener);
        }else if(card instanceof EventCard){
            ((EventData)dataToShow.get(position)).onBindViewHolder(card, position, pListener);
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

    public void clear(){
        dataToShow.clear();
        notifyDataSetChanged();
    }

    public void setOnEditClickListener(OnEditClickListener listener){
        this.mListener = listener;

    }

    public void setOnItemClickedListener(OnItemClickedListener listener){
            this.pListener = listener;
    }


    public interface OnEditClickListener{
        void onEditcliked();
    }


    public interface OnItemClickedListener{
        void onItemClicked(int pos);
    }
}
