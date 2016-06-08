package com.eventshigh.nearme.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.network.DateCategoryRequest;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.EventCollectionRequest.EventsCollection;
import com.eventshigh.nearme.app.network.MobileUserEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
import com.eventshigh.nearme.app.network.SocialActionsRequest;
import com.eventshigh.nearme.app.network.SocialActionsRequest.SocialActions;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to show events.
 */
public class EventsFragment extends BaseEventsFragment {
    private static final String SHOW_EH_INVITE_NOTIFICATION_PARAM =
            EventsFragment.class.getName() + "_show_eh_invite_param";

    private AutofitRecyclerView eventGridView;
    private View topProgressBar;
    private View noMyEventsView;
    private View retryView;

    private TextView noEventHeaderText;
    private TextView callToActionButton;

    private EventsAdapter eventsAdapter;
    private OnScrollListener onScrollListener;

    private boolean showEhInviteForNotification;

    int scrollPosition = 0;

    SocialInvitationsRequest.SpecialCoupons special;

    EventsCollection eventsCollection;

    public static EventsFragment getInstance(EventsContext eventsContext, boolean showFollowCard,
                                             boolean showCategories, boolean showEhInviteForNotification, SocialInvitationsRequest.SpecialCoupons special) {
        EventsFragment fragment = new EventsFragment();
        Bundle args = getArgs(eventsContext, showFollowCard, showCategories);
        args.putBoolean(SHOW_EH_INVITE_NOTIFICATION_PARAM, showEhInviteForNotification);
        args.putParcelable("special_obj", special);
        fragment.setArguments(args);
        return fragment;
    }

    public void onAttach(Context context) {
        super.onAttach(context);

        showEhInviteForNotification = getArguments().getBoolean(SHOW_EH_INVITE_NOTIFICATION_PARAM);
        if (onScrollListener == null) {
            onScrollListener = new HideActionBarOnScroll(this.activity);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup the events adapter to show data.
        eventsAdapter = new EventsAdapter(activity);
        eventGridView = (AutofitRecyclerView) view.findViewById(R.id.event_grid);
        eventGridView.setAdapter(eventsAdapter);
        eventsAdapter.setOnItemClickedListener(new EventsAdapter.OnItemClickedListener() {
            @Override
            public void onItemClicked(int pos) {
                scrollPosition = pos;
            }
        });

        // Setup the actionbar hide/show on scroll.
        eventGridView.addOnScrollListener(onScrollListener);

        // Setup the refresh on swipe down.
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                activity.reportActionToAnalytics("swipeRefresh", eventsContext.toString());
                swipeRefreshLayout.setRefreshing(false);
                fetchNewListing(true /* bypass cache*/);
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setEnabled(false);
        noEventHeaderText = (TextView) view.findViewById(R.id.no_my_event_heading);
        callToActionButton = (TextView) view.findViewById(R.id.explore_events);
        // Actions Buttons.
        callToActionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (EventsHighEndpoints.isMyInterestEventQuery(eventsContext.query)) {
                    Intent intent = new Intent(getActivity(), SelectInterestsActivity.class);
                    startActivity(intent);
                } else {
                    activity.showSearchView("today");
                }
            }
        });

        view.findViewById(R.id.retry).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("retry");
                fetchNewListing(false);
            }
        });

        // More views.
        topProgressBar = view.findViewById(R.id.top_progress_bar);
        noMyEventsView = view.findViewById(R.id.view_no_my_event);
        retryView = view.findViewById(R.id.view_retry);

        if (getArguments() != null && getArguments().getParcelable("special_obj") != null)
            special = getArguments().getParcelable("special_obj");

    }


    @Override
    public void onStart() {
        super.onStart();
        if (Preferences.getInstance(getActivity()).isInterestUpdated() && EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
            fetchNewListing(Preferences.getInstance(getActivity()).isInterestUpdated());
            Preferences.getInstance(getActivity()).setIsInterestUpdated(false);
        } else {
            fetchNewListing(false);
        }

    }

    @Override
    public void onStop() {

        if (asyncRequest != null)
            asyncRequest.cancel(true);
        isLoading = false;
/*
        if(eventsAdapter!=null){
            eventsAdapter.clear();
        }*/
        super.onStop();
    }

    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    boolean isLoading;
    MyEventsRequest asyncRequest;

    public void fetchNewListing(boolean shouldBypassCache) {
        if (isLoading) {
            return;
        }
        isLoading = true;

        topProgressBar.setVisibility(View.VISIBLE);
        noMyEventsView.setVisibility(View.GONE);
        retryView.setVisibility(View.GONE);

        // Stop all requests associated with this fragment and then submit new request.
        if (EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
            asyncRequest = new MyEventsRequest(activity, eventsContext, Priority.IMMEDIATE, this,
                    shouldBypassCache, true, mMyFavEventsMoviesFetcherCallBack, mErrorListener);
            asyncRequest.execute();
        } else if (EventsHighEndpoints.isMyInterestEventQuery(eventsContext.query)) {
            MobileUserEventsRequest.submit(activity, eventsContext,
                    Priority.IMMEDIATE, this, shouldBypassCache, true, mMyEventsFetcherCallBack, mErrorListener);
        } else if (eventsContext.query.isEmpty() && !eventsContext.dateFilter.isEmpty() && showCategories) {
            DateCategoryRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                    shouldBypassCache, mMyEventsFetcherCallBack, mErrorListener);
        } else {

            EventCollectionRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                    shouldBypassCache, true, mEventsFetcherCallBack, mErrorListener);
        }
        SocialInvitationsRequest.submit(activity, Priority.NORMAL, this, false, mSocialInvitesCallback,
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        VolleyHelper.log(activity, volleyError);
                    }
                }
        );

        // Load social actions.
        if (showFollowCard) {
            SocialActionsRequest.submit(activity, Priority.LOW, this, shouldBypassCache,
                    new Listener<SocialActions>() {
                        @Override
                        public void onResponse(SocialActions socialActions, boolean isIntermediate) {
                            activity.reportActionToAnalytics("showSocialInfo", "followers",
                                    socialActions.getNumFollowers(eventsContext.query));
                            eventsAdapter.setSocialActions(socialActions);
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            VolleyHelper.log(activity, volleyError);
                        }
                    }
            );
        }
    }

    boolean isFragmentDestroyed = false;

    @Override
    public void onDestroy() {
        isFragmentDestroyed = true;
        super.onDestroy();
    }


    private Listener<List<TopicEvents>> mMyEventsFetcherCallBack = new Listener<List<TopicEvents>>() {
        @Override
        public void onResponse(List<TopicEvents> myEvents, boolean isIntermediate) {
            if (isFragmentDestroyed) {
                return;
            }

            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (myEvents.isEmpty()) {
                    if (EventsHighEndpoints.isMyEventQuery(eventsContext.query) && retryView.getVisibility() == View.GONE) {
                        noMyEventsView.setVisibility(View.VISIBLE);
                        noEventHeaderText.setText(getResources().getString(R.string.ui_no_my_event));
                        callToActionButton.setText("Explore Events");
                    } else if (EventsHighEndpoints.isMyInterestEventQuery(eventsContext.query) && retryView.getVisibility() == View.GONE) {
                        noMyEventsView.setVisibility(View.VISIBLE);
                        noEventHeaderText.setText(getResources().getString(R.string.ui_no_my_interest));
                        callToActionButton.setText("Personalize");
                    } else {
                        noMyEventsView.setVisibility(View.GONE);
                        retryView.setVisibility(View.VISIBLE);
                    }
                }
            }

            if (!isIntermediate || !myEvents.isEmpty()) {
                if (getActivity() != null && (getActivity()) instanceof EventsGridActivity) {
                    ((EventsGridActivity) getActivity()).setShareImageUrl(myEvents.get(0).events.get(0).imgUrl);
                }

                eventsAdapter.setTopicEvents(myEvents, eventsContext, eventGridView.getSpanCount() * 2);

                //    eventGridView.scrollToPosition(scrollPosition);
            }
            isLoading = false;
        }
    };

    private Listener<MyEventsRequest.MeEventFavouriteObject> mMyFavEventsMoviesFetcherCallBack = new Listener<MyEventsRequest.MeEventFavouriteObject>() {
        @Override
        public void onResponse(MyEventsRequest.MeEventFavouriteObject myEvents, boolean isIntermediate) {
            if (isFragmentDestroyed) {
                return;
            }

            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (myEvents.topicEvents.isEmpty() && myEvents.movies.isEmpty()) {
                    if (EventsHighEndpoints.isMyEventQuery(eventsContext.query) && retryView.getVisibility() == View.GONE) {
                        noMyEventsView.setVisibility(View.VISIBLE);
                        noEventHeaderText.setText(getResources().getString(R.string.ui_no_my_event));
                        callToActionButton.setText("Explore Events");
                    } else if (EventsHighEndpoints.isMyInterestEventQuery(eventsContext.query) && retryView.getVisibility() == View.GONE) {
                        noMyEventsView.setVisibility(View.VISIBLE);
                        noEventHeaderText.setText(getResources().getString(R.string.ui_no_my_interest));
                        callToActionButton.setText("Personalize");
                    } else {
                        noMyEventsView.setVisibility(View.GONE);
                        retryView.setVisibility(View.VISIBLE);
                    }
                }
            }

            if (!isIntermediate) {
                if (!myEvents.topicEvents.isEmpty()) {
                    if (getActivity() != null && (getActivity()) instanceof EventsGridActivity) {
                        ((EventsGridActivity) getActivity()).setShareImageUrl(myEvents.topicEvents.get(0).events.get(0).imgUrl);
                    }

                    eventsAdapter.setTopicEvents(myEvents.topicEvents, eventsContext, eventGridView.getSpanCount() * 2);
                }
                if (!myEvents.movies.isEmpty()) {
                    eventsAdapter.setMoviesListData(myEvents.movies, eventsContext, true, myEvents.topicEvents.isEmpty() ? true : false);
                }
                //    eventGridView.scrollToPosition(scrollPosition);
            }
            isLoading = false;
        }
    };

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private Listener<EventsCollection> mEventsFetcherCallBack = new Listener<EventsCollection>() {
        @Override
        public void onResponse(EventsCollection eventsCollection, boolean isIntermediate) {
            if (isDetached()) {
                return;
            }

            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (eventsCollection.events.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    Snackbar.make(topProgressBar, R.string.no_events, Snackbar.LENGTH_SHORT).show();
                }
            }

            if (!isIntermediate || !eventsCollection.events.isEmpty()) {
                String seeAllQuery = eventsContext.query.isEmpty() ||
                        eventsContext.dateFilter.isEmpty() ? null : eventsContext.query;
                if (getActivity() != null && (getActivity()) instanceof EventsGridActivity) {
                    if (eventsCollection.events.size() > 0)
                        ((EventsGridActivity) getActivity()).setShareImageUrl(eventsCollection.events.get(0).imgUrl);
                }
                EventsFragment.this.eventsCollection = eventsCollection;
                List<Event> filteredEvents = eventsCollection.events;
                filteredEvents = filterEventsWithCategory(null, filteredEvents);
                filteredEvents = filterEventsWithDate(filteredEvents, -1);
                filteredEvents = filterEventsWithPrice(filteredEvents, -1);
                eventsAdapter.setEvents(filteredEvents, seeAllQuery, showEhInviteForNotification);
                /*if (showFollowCard) {
                    eventsAdapter.addFollowCard(eventsContext.query, eventsCollection.events.size(),
                            eventsCollection.numFollowers, special);
                }*/
                //     eventGridView.scrollToPosition(scrollPosition);
            }
        }
    };

    private Listener<SocialInvitationsRequest.CommonInviteObject> mSocialInvitesCallback =
            new Listener<SocialInvitationsRequest.CommonInviteObject>() {
                @Override
                public void onResponse(SocialInvitationsRequest.CommonInviteObject inviteObj, boolean isIntermediate) {
                    if (isDetached()) {
                        return;
                    }

                    eventsAdapter.setSocialInvites(inviteObj.getInvites());
                    isLoading = false;
                }
            };

    private ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            if (isDetached()) {
                return;
            }

            topProgressBar.setVisibility(View.GONE);
            if (eventsAdapter.getItemCount() > 0) {
                activity.showMessage(R.string.failed_refresh);
            } else {
                retryView.setVisibility(View.VISIBLE);
            }

            VolleyHelper.log(activity, volleyError);
            isLoading = false;
        }
    };


    ArrayList<String> filterCategoryName;

    public List<Event> filterEventsWithCategory(String category, List<Event> totalEvents) {
        List<Event> allEvents;

        if (totalEvents == null && eventsCollection != null) {

            allEvents = eventsCollection.events;
        } else {
            allEvents = totalEvents;
        }

        if (allEvents != null) {
            if (filterCategoryName == null)
                filterCategoryName = new ArrayList<>();
            if (category != null) {
                if (filterCategoryName.contains(category)) {
                    filterCategoryName.remove(category);
                } else {
                    filterCategoryName.add(category);
                }
            }
            List<Event> filteredEvents = new ArrayList<>();
            if (filterCategoryName.size() > 0) {
                for (int i = 0; i < allEvents.size(); i++) {
                    for (int j = 0; j < filterCategoryName.size(); j++) {
                        if (allEvents.get(i).tags.contains(filterCategoryName.get(j))) {
                            filteredEvents.add(allEvents.get(i));
                            break;
                        }
                    }
                }
            } else {
                filteredEvents = allEvents;
            }

            if (totalEvents == null) {
                filteredEvents = filterEventsWithDate(filteredEvents, -1);
                filteredEvents = filterEventsWithPrice(filteredEvents, -1);
                eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
                if (filteredEvents.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    Snackbar.make(topProgressBar, R.string.no_events, Snackbar.LENGTH_SHORT).show();

                }
            }
            return filteredEvents;
        }
        return null;
    }


    ArrayList<Long> filterEventTimes;

    public List<Event> filterEventsWithDate(List<Event> totalEvents, long... times) {
        List<Event> allEvents;
        if (totalEvents == null && eventsCollection != null) {

            allEvents = eventsCollection.events;
        } else {
            allEvents = totalEvents;
        }

        if (allEvents != null) {

            if (filterEventTimes == null) {
                filterEventTimes = new ArrayList<>();
            }
            for (long time : times) {
                if (time != -1) {
                    if (filterEventTimes.contains(time)) {
                        filterEventTimes.remove(time);
                    } else {
                        filterEventTimes.add(time);
                    }
                }
            }
            List<Event> filteredEvents = new ArrayList<>();
            if (filterEventTimes.size() > 0) {
                for (int i = 0; i < allEvents.size(); i++) {

                    secondLoop:
                    for (int j = 0; j < filterEventTimes.size(); j++) {
                        for (int k = 0; k < allEvents.get(i).eventTimings.length; k++) {
                            if (filterEventTimes.get(j) == DateTimeUtils.getEventDate(allEvents.get(i), k).getTime()) {
                                filteredEvents.add(allEvents.get(i));
                                break secondLoop;

                            }
                        }
                    }
                }
            } else {
                filteredEvents = allEvents;
            }

            if (totalEvents == null) {
                filteredEvents = filterEventsWithCategory(null, filteredEvents);
                filteredEvents = filterEventsWithPrice(filteredEvents, -1);
                eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
                if (filteredEvents.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    Snackbar.make(topProgressBar, R.string.no_events, Snackbar.LENGTH_SHORT).show();

                }
            }
            return filteredEvents;
        }
        return null;

    }

    List<Integer> filterEventPrices;

    public List<Event> filterEventsWithPrice(List<Event> totalEvents, int priceValue) {
        List<Event> allEvents;
        if (totalEvents == null && eventsCollection != null) {

            allEvents = eventsCollection.events;
        } else {
            allEvents = totalEvents;
        }
        if (allEvents != null) {
            if (filterEventPrices == null) {
                filterEventPrices = new ArrayList<>();
            }


            if (priceValue != -1) {
                if (filterEventPrices.contains(priceValue)) {
                    filterEventPrices.remove((Integer) priceValue);
                } else {
                    filterEventPrices.add((Integer) priceValue);
                }
            }


            List<Event> filteredEvents = new ArrayList<>();
            if (filterEventPrices.size() > 0) {
                for (int i = 0; i < allEvents.size(); i++) {
                    Event event = allEvents.get(i);
                    secondLoop:
                    for (int j = 0; j < filterEventPrices.size(); j++) {
                        int priceMin = -1;
                        int priceMax = -1;
                        if (filterEventPrices.get(j) == EventsGridActivity.FREE) {
                            priceMax = 0;
                            priceMin = 0;
                        } else if (filterEventPrices.get(j) == EventsGridActivity.UPTO_250) {
                            priceMin = 1;
                            priceMax = 250;
                        } else if (filterEventPrices.get(j) == EventsGridActivity.PRICE_250_TO_750) {
                            priceMin = 250;
                            priceMax = 750;
                        } else if (filterEventPrices.get(j) == EventsGridActivity.PRICE_750_TO_1500) {
                            priceMin = 750;
                            priceMax = 1500;
                        } else {
                            priceMin = 1500;
                            priceMax = 500000;
                        }
                        if (event.ehPrices.size() > 0) {
                            for (EhPrices price : event.ehPrices) {
                                if (price.discountValue > 0) {
                                    if (price.discountValue >= priceMin && price.discountValue <= priceMax) {
                                        filteredEvents.add(event);
                                        break secondLoop;
                                    }
                                } else {
                                    if (price.value >= priceMin && price.value <= priceMax) {
                                        filteredEvents.add(event);
                                        break secondLoop;
                                    }
                                }
                            }
                        } else {
                            if (event.minPrice >= priceMin && event.maxPrice <= priceMax) {
                                filteredEvents.add(event);
                                break secondLoop;
                            }
                        }
                    }

                }
            } else {
                filteredEvents = allEvents;
            }

            if (totalEvents == null) {
                filteredEvents = filterEventsWithCategory(null, filteredEvents);
                filteredEvents = filterEventsWithDate(filteredEvents, -1);
                eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
                if (filteredEvents.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    Snackbar.make(topProgressBar, R.string.no_events, Snackbar.LENGTH_SHORT).show();

                }
            }
            return filteredEvents;
        }


        return null;
    }
}
