package com.eventshigh.nearme.app.activity;


import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Request.Priority;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventComparator;
import com.eventshigh.nearme.app.data.EventDistanceComparator;
import com.eventshigh.nearme.app.data.EventPriceComparator;
import com.eventshigh.nearme.app.data.EventScoreComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.ProfileInfo;
import com.eventshigh.nearme.app.data.SocialFriend;
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
import com.eventshigh.nearme.app.ui.MapMarkerManager;
import com.eventshigh.nearme.app.ui.adapter.EventCard;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;
import com.eventshigh.nearme.app.view.ContactListView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    private ProfileInfo profileInfo;

    EventsCollection eventsCollection;

    boolean isTodaySelected;


    public static final String IS_TODAY_SELECTED = "is_today_selected";


    int sortState = EventsGridActivity.SORT_STATE_TRENDING;

    public List<Event> filteredEvents;


    LinearLayout eventListContainer;


    //Map contents
    SupportMapFragment mapFragment;
    private GestureDetectorCompat gestureDetector;
    // Manager for all markers drawn on map. Manager is responsible for hiding/showing markers
    // on map.
    private MapMarkerManager mapMarkerManager = new MapMarkerManager();

    // Last marker for which the event info card is shown.
    private Marker lastSelectedMarker;

    // Google Map View shows to user using MapFragment.
    private GoogleMap map;

    // For performance reasons, we show events only where user has reasonable zoom level.
    public static final int MIN_ZOOM_LEVEL = 9;
    public static final int DEFAULT_ZOOM_LEVEL = 14;


    boolean isMapShown;

    TextView mapEventsCount;

    boolean isMapListShown;


    public static EventsFragment getInstance(EventsContext eventsContext, boolean showFollowCard,
                                             boolean showCategories, boolean showEhInviteForNotification, SocialInvitationsRequest.SpecialCoupons special, boolean isTodaySelected, ProfileInfo profileInfo) {
        EventsFragment fragment = new EventsFragment();
        Bundle args = getArgs(eventsContext, showFollowCard, showCategories);
        args.putBoolean(SHOW_EH_INVITE_NOTIFICATION_PARAM, showEhInviteForNotification);
        args.putBoolean(IS_TODAY_SELECTED, isTodaySelected);
        args.putParcelable("special_obj", special);
        args.putParcelable("profile_info", profileInfo);
        fragment.setArguments(args);
        return fragment;
    }

    public void onAttach(Context context) {
        super.onAttach(context);

        showEhInviteForNotification = getArguments().getBoolean(SHOW_EH_INVITE_NOTIFICATION_PARAM);
        isTodaySelected = getArguments().getBoolean(IS_TODAY_SELECTED);
        if (onScrollListener == null) {
            onScrollListener = new HideActionBarOnScroll(this.activity);
        }

        if (getArguments() != null && getArguments().getParcelable("profile_info") != null)
            profileInfo = getArguments().getParcelable("profile_info");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
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
        if (profileInfo == null)
            eventGridView.addOnScrollListener(onScrollListener);

        // Setup the refresh on swipe down.
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                activity.reportActionToAnalytics("swipeRefresh", eventsContext.toString());
                swipeRefreshLayout.setRefreshing(false);
                fetchNewListing(true);// bypass cache
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


        // Setup the view.

        eventListContainer = (LinearLayout) view.findViewById(R.id.events_list);
        //eventCardContainer = (FrameLayout) view.findViewById(R.id.event_card_container);
        mapClickedList = (LinearLayout) view.findViewById(R.id.map_clicked_events);
        mapClickedEvents = (AutofitRecyclerView) view.findViewById(R.id.event_grid_map);
        mapClickedEvents.getParent().requestDisallowInterceptTouchEvent(true);
        dimBgView = view.findViewById(R.id.dim_bg_view);
        dimBgView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMapEvents(-1);
            }
        });
        view.findViewById(R.id.cross_img).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMapEvents(-1);
            }
        });
        mapEventsCount = (TextView) view.findViewById(R.id.map_events_count);

        hideMapEvents(-1);
    }

    LinearLayout mapClickedList;
    AutofitRecyclerView mapClickedEvents;
    View dimBgView;


    public static final int MOVE_VIEW_TO_POS = 2500;

    public void hideListView() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(eventListContainer, View.TRANSLATION_Y, 0, MOVE_VIEW_TO_POS);
        anim.setDuration(500);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                isMapShown = true;
                setUpMapContents();
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        anim.start();
    }

    public void showListView() {
        eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
        if (showFollowCard) {
            eventsAdapter.addFollowCard(eventsContext.query, eventsCollection.events.size(),
                    eventsCollection.numFollowers);
        }

        if (eventCardContainer != null) {
            eventCardContainer.removeAllViews();
        }
        isMapShown = false;
        ObjectAnimator anim = ObjectAnimator.ofFloat(eventListContainer, View.TRANSLATION_Y, MOVE_VIEW_TO_POS, 0);
        anim.setDuration(500);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
    }


    public void setUpMapContents() {
        if (map == null) {
            FragmentManager fm = getChildFragmentManager();
            mapFragment = (SupportMapFragment) fm.findFragmentByTag("mapFragment");
            if (mapFragment == null) {
                mapFragment = new SupportMapFragment();
                FragmentTransaction ft = fm.beginTransaction();
                ft.add(R.id.map_container, mapFragment, "mapFragment");
                ft.commit();
                fm.executePendingTransactions();
            }
            setUpMap();
            setupGestureDetectorIfNeeded();
        } else {
            mapMarkerManager.setEvents(map, filteredEvents);
        }


    }

    FrameLayout eventCardContainer;


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
            if (profileInfo == null) {
                asyncRequest = new MyEventsRequest(activity, eventsContext, Priority.IMMEDIATE, this,
                        shouldBypassCache, true, mMyFavEventsMoviesFetcherCallBack, mErrorListener);
                asyncRequest.execute();
            } else {
                bindDataFavourite(profileInfo.getMeEventFavouriteObject());
            }

        } else if (EventsHighEndpoints.isMyInterestEventQuery(eventsContext.query)) {
            if (profileInfo == null) {
                MobileUserEventsRequest.submit(activity, eventsContext,
                        Priority.IMMEDIATE, this, shouldBypassCache, true, mMyEventsFetcherCallBack, mErrorListener);
            } else {
                bindDataInterestEvents(profileInfo.getMyInterestEvents());
            }
        } else if (eventsContext.query.isEmpty() && !eventsContext.dateFilter.isEmpty() && showCategories) {
            DateCategoryRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                    shouldBypassCache, mMyEventsFetcherCallBack, mErrorListener);
        } else if (!eventsContext.query.isEmpty() && eventsContext.query.equalsIgnoreCase("today")) {

            showFollowCard = false;
            Calendar endDate = Calendar.getInstance();
            endDate.setFirstDayOfWeek(Calendar.MONDAY);
            endDate.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            endDate.add(Calendar.DAY_OF_WEEK, 8);
            String dateString = EventsContext.formatDateFilter(Calendar.getInstance()) + "," + EventsContext.formatDateFilter(endDate);
            EventCollectionRequest.submit(activity, eventsContext, Request.Priority.IMMEDIATE, this, dateString,
                    shouldBypassCache, true, mEventsFetcherCallBack, mErrorListener, new EventCollectionRequest.OnDataProcessComplete() {
                        @Override
                        public void onDataProcessComplete(final EventsCollection eventsCollection, final boolean isLoadingFinished) {
                            if (isDetached()) {
                                return;
                            }

                            if (!(eventsCollection.events.isEmpty())) {
                                final String seeAllQuery = eventsContext.query.isEmpty() ||
                                        eventsContext.dateFilter.isEmpty() ? null : eventsContext.query;
                                if (getActivity() != null && (getActivity()) instanceof EventsGridActivity) {
                                    if (eventsCollection.events.size() > 0)
                                        ((EventsGridActivity) getActivity()).setShareImageUrl(eventsCollection.events.get(0).imgUrl);
                                }
                                if (isLoadingFinished) {
                                    EventsFragment.this.eventsCollection.events.addAll(eventsCollection.events);
                                } else {
                                    EventsFragment.this.eventsCollection = eventsCollection;
                                }


                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (isLoadingFinished) {

                                            topProgressBar.setVisibility(View.GONE);

                                            if (eventsCollection.events.isEmpty()) {
                                                noMyEventsView.setVisibility(View.VISIBLE);

                                                // Failed. Show toast and return empty list.
                                                Snackbar.make(topProgressBar, R.string.no_events, Snackbar.LENGTH_SHORT).show();
                                            } else {
                                                noMyEventsView.setVisibility(View.GONE);
                                            }

                                            ((EventsGridActivity) getActivity()).filtersContainer.setVisibility(View.VISIBLE);
                                            ((EventsGridActivity) getActivity()).fabBrowseMap.setVisibility(View.VISIBLE);
                                        }
                                        List<Event> filteredEvents = EventsFragment.this.eventsCollection.events;
                                        filteredEvents = filterEventsWithCategory(null, filteredEvents);
                                        if (isTodaySelected) {
                                            if (filterEventTimes != null)
                                                filterEventTimes.clear();
                                            filteredEvents = filterEventsWithDate(filteredEvents, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                                        } else {
                                            filteredEvents = filterEventsWithDate(filteredEvents, -1);
                                        }

                                        filteredEvents = filterEventsWithPrice(filteredEvents, -1);
                                        EventsFragment.this.filteredEvents = filteredEvents;
                                        sortData();

                                        eventsAdapter.setEvents(filteredEvents, seeAllQuery, showEhInviteForNotification);


                                    }
                                });

                            }
                        }
                    });

        } else {

            EventCollectionRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                    shouldBypassCache, true, mEventsFetcherCallBack, mErrorListener);
        }
        // Load social actions.


    }

    public void addSocialInvitationRequests() {
        SocialActionsRequest.submit(getActivity(), Request.Priority.LOW, this, false,
                new Response.Listener<SocialActionsRequest.SocialActions>() {
                    @Override
                    public void onResponse(SocialActionsRequest.SocialActions socialActions, boolean isIntermediate) {
                        if (isAdded() && !isFragmentDestroyed && eventsAdapter != null) {
                            eventsAdapter.setSocialActions(socialActions);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        VolleyHelper.log(((BaseActivity) getActivity()), volleyError);
                    }
                }
        );
    }


    boolean isFragmentDestroyed = false;

    @Override
    public void onDestroy() {
        isFragmentDestroyed = true;
        super.onDestroy();
    }

    private void bindDataInterestEvents(List<TopicEvents> myEvents) {
        if (isFragmentDestroyed) {
            return;
        }

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

        if (!myEvents.isEmpty() && myEvents.get(0).events != null && myEvents.get(0).events.size() > 0) {
            if (getActivity() != null && (getActivity()) instanceof EventsGridActivity) {
                ((EventsGridActivity) getActivity()).setShareImageUrl(myEvents.get(0).events.get(0).imgUrl);
            }

            eventsAdapter.setTopicEvents(myEvents, eventsContext, eventGridView.getSpanCount() * 2);
        }
        isLoading = false;
    }

    private void bindDataFavourite(MyEventsRequest.MeEventFavouriteObject myEvents) {
        if (isFragmentDestroyed) {
            return;
        }

        topProgressBar.setVisibility(View.GONE);

        if (myEvents == null || ((myEvents.topicEvents == null || myEvents.topicEvents.size() == 0 || myEvents.topicEvents.get(0).events == null || myEvents.topicEvents.get(0).events.isEmpty()) && myEvents.movies.isEmpty())) {
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
        if (!myEvents.topicEvents.isEmpty() && myEvents.topicEvents.get(0).events != null && myEvents.topicEvents.get(0).events.size() > 0) {
            if (getActivity() != null && (getActivity()) instanceof EventsGridActivity) {
                ((EventsGridActivity) getActivity()).setShareImageUrl(myEvents.topicEvents.get(0).events.get(0).imgUrl);
            }
            eventsAdapter.setTopicEvents(myEvents.topicEvents, eventsContext, eventGridView.getSpanCount() * 2);
        }
        if (!myEvents.movies.isEmpty()) {
            eventsAdapter.setMoviesListData(myEvents.movies, eventsContext, true, myEvents.topicEvents.isEmpty() ? true : false);
        }
        //    eventGridView.scrollToPosition(scrollPosition);
        isLoading = false;
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

            if (!isIntermediate || (!myEvents.isEmpty() && myEvents.get(0).events != null || myEvents.get(0).events.size() > 0)) {
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
                if (!myEvents.topicEvents.isEmpty() && myEvents.topicEvents.get(0).events != null || myEvents.topicEvents.get(0).events.size() > 0) {
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
            if (eventsCollection != null) {
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
                    if (isTodaySelected) {
                        filteredEvents = filterEventsWithDate(filteredEvents, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                    } else {
                        filteredEvents = filterEventsWithDate(filteredEvents, -1);
                    }

                    filteredEvents = filterEventsWithPrice(filteredEvents, -1);
                    EventsFragment.this.filteredEvents = filteredEvents;
                    sortData();

                    eventsAdapter.setEvents(filteredEvents, seeAllQuery, showEhInviteForNotification);
                    if (showFollowCard) {
                        eventsAdapter.addFollowCard(eventsContext.query, eventsCollection.events.size(),
                                eventsCollection.numFollowers);
                    }
                    addSocialInvitationRequests();
                    ((EventsGridActivity) getActivity()).filtersContainer.setVisibility(View.VISIBLE);
                    ((EventsGridActivity) getActivity()).fabBrowseMap.setVisibility(View.VISIBLE);
                /*if (showFollowCard) {
                    eventsAdapter.addFollowCard(eventsContext.query, eventsCollection.events.size(),
                            eventsCollection.numFollowers, special);
                }*/
                    //     eventGridView.scrollToPosition(scrollPosition);
                }
            }

            addSocialInvitationRequests();
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
                /*eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
                if (filteredEvents.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    Snackbar.make(topProgressBar, R.string.no_events, Snackbar.LENGTH_SHORT).show();

                }*/
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
               /* eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
                if (filteredEvents.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    Snackbar.make(topProgressBar, R.string.no_events, Snackbar.LENGTH_SHORT).show();

                }*/
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
                /*eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
                if (filteredEvents.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    Snackbar.make(topProgressBar, R.string.no_events, Snackbar.LENGTH_SHORT).show();

                }*/
            }
            return filteredEvents;
        }


        return null;
    }

    FilterAsyncTask filterAsyncTask;

    public void sortAccToSortState(int sortState) {
        if (filteredEvents != null) {
            this.sortState = sortState;
            sortData();
            eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
            if (showFollowCard) {
                eventsAdapter.addFollowCard(eventsContext.query, eventsCollection.events.size(),
                        eventsCollection.numFollowers);
            }
        }
    }

    public void sortData() {
        if (filteredEvents != null) {
            if (sortState == EventsGridActivity.SORT_STATE_TRENDING) {
                Collections.sort(filteredEvents, new EventScoreComparator());
            } else if (sortState == EventsGridActivity.SORT_STATE_PRICE) {
                Collections.sort(filteredEvents, new EventPriceComparator());
            } else if (sortState == EventsGridActivity.SORT_STATE_DISTANCE) {
                if (new Account(activity).getLastLocality() != null)
                    Collections.sort(filteredEvents, new EventDistanceComparator(new Account(activity).getLastLocality().getLatLng()));
                else
                    Collections.sort(filteredEvents, new EventDistanceComparator(new Account(activity).getLastCity().cityBounds.getCenter()));
            }
        }

    }

    public void startFilterAsyncTask(int type, List<Event> totalEvents, String category, int priceValue, long... times) {

        if (isMapListShown) {
            hideMapEvents(-1);
        }

        if (filterAsyncTask != null && !filterAsyncTask.isCancelled()) {
            filterAsyncTask.cancel(true);

        }
        filterAsyncTask = new FilterAsyncTask(type, totalEvents, category, priceValue, times);
        filterAsyncTask.execute();

    }

    public class FilterAsyncTask extends AsyncTask<Void, Void, List<Event>> {

        List<Event> totalEvents;
        String category;
        int priceValue;
        long[] times;
        int type;

        public FilterAsyncTask(int type, List<Event> totalEvents, String category, int priceValue, long... times) {
            this.type = type;
            this.totalEvents = totalEvents;
            this.category = category;
            this.priceValue = priceValue;
            this.times = times;
        }

        @Override
        protected void onPreExecute() {
            topProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<Event> doInBackground(Void... params) {
            switch (type) {
                case EventsGridActivity.PRICE_FILTER:
                    return filterEventsWithPrice(totalEvents, priceValue);
                case EventsGridActivity.DATE_FILTER:
                    return filterEventsWithDate(totalEvents, times);
                case EventsGridActivity.CATEGORY_FILTER:
                    return filterEventsWithCategory(category, totalEvents);

            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Event> events) {

            if (!isCancelled() && events != null) {
                topProgressBar.setVisibility(View.GONE);
                EventsFragment.this.filteredEvents = events;

                if (isMapShown) {
                    mapMarkerManager.setEvents(map, filteredEvents);
                } else {

                    sortData();
                    eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
                    if (showFollowCard) {
                        eventsAdapter.addFollowCard(eventsContext.query, eventsCollection.events.size(),
                                eventsCollection.numFollowers);
                    }
                }
                if (events.isEmpty()) {
                    Snackbar.make(getView(), R.string.no_events, Snackbar.LENGTH_SHORT).show();

                }
            }
        }
    }


    private void setUpMap() {
        // Try to obtain the map from the SupportMapFragment.
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
                map.setMyLocationEnabled(true);
                map.setOnCameraChangeListener(mOnCameraChangeListener);
                map.setOnMarkerClickListener(mOnMarkerClickListener);
                map.setOnInfoWindowClickListener(mOnInfoWindowClickListener);
                map.setOnMapClickListener(mOnMapClickListener);

                LatLng location = eventsContext.location;
                eventsContext.changeLocation(null);
                updateUserLocation(location);
            }
        });
    }

    private void setupGestureDetectorIfNeeded() {
        if (gestureDetector == null) {
            gestureDetector = new GestureDetectorCompat(activity, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent event) {
                    return true;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    activity.showEventDetails(mapMarkerManager.getEvent(lastSelectedMarker),
                            eventsContext.getLabel(), null);
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent event1, MotionEvent event2,
                                       float velocityX, float velocityY) {
                    if (Math.abs(velocityY) > Math.abs(velocityX)) {
                        // this is either up or down movement, ignore.
                        activity.reportActionToAnalytics("swipeVertical");
                        return false;
                    }

                    activity.reportActionToAnalytics("swipe");
                    Marker nextMarker = velocityX > 0 ?
                            mapMarkerManager.getPrevMarker(lastSelectedMarker) :
                            mapMarkerManager.getNextMarker(lastSelectedMarker);
                    if (nextMarker != null) {
                        lastSelectedMarker.hideInfoWindow();
                        nextMarker.setVisible(true);
                        nextMarker.showInfoWindow();
                        updateUserLocation(nextMarker.getPosition());
                        mOnMarkerClickListener.onMarkerClick(nextMarker);
                    }
                    return true;
                }
            });
        }
    }


    private void showEventsListOnMap() {
        EventsAdapter adapter = new EventsAdapter(activity);
        mapClickedEvents.setAdapter(adapter);
        List<Event> events = new ArrayList<>();

        Event event = mapMarkerManager.getEvent(lastSelectedMarker);
        events.add(event);
        events.addAll(getEventsForSameAddress(event));
        mapEventsCount.setText(events.size() + " Events");
        adapter.setEvents(events, null, false);
        if (((EventsGridActivity) getActivity()).isFiltersShown) {
            ((EventsGridActivity) getActivity()).collapseAnimation(EventsGridActivity.SHOW_MAP_EVENTS_LIST);
        } else {
            bringMapEventsVisible();
        }
    }

    public ArrayList<Event> getEventsForSameAddress(Event mappedEvent) {

        ArrayList<Event> events = new ArrayList<>();
        for (Event event : filteredEvents) {
            if (event.id == mappedEvent.id || !(event.location != null)) {
                continue;
            }
            if (event.location.equals(mappedEvent.location)) {
                events.add(event);
            }
        }
        return events;

    }


    public void hideMapEvents(final int state) {
        AnimatorSet set = new AnimatorSet();
        ObjectAnimator anim = ObjectAnimator.ofFloat(mapClickedList, View.TRANSLATION_Y, 0, MOVE_VIEW_TO_POS);
        anim.setDuration(300);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(dimBgView, View.ALPHA, 1, 0);
        anim1.setDuration(300);
        anim1.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                dimBgView.setVisibility(View.GONE);
                lastSelectedMarker = null;
                isMapListShown = false;
                if (state == EventsGridActivity.SHOW_EVENT_LIST_STATE) {
                    showListView();
                } else if (state == EventsGridActivity.SHOW_FILTERS_STATE) {
                    ((EventsGridActivity) getActivity()).expandAnimation();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        set.playTogether(anim, anim1);
        set.start();

    }

    public void bringMapEventsVisible() {

        AnimatorSet set = new AnimatorSet();
        ObjectAnimator anim = ObjectAnimator.ofFloat(mapClickedList, View.TRANSLATION_Y, MOVE_VIEW_TO_POS, 0);
        anim.setDuration(300);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        ObjectAnimator anim1 = ObjectAnimator.ofFloat(dimBgView, View.ALPHA, 0, 1);
        anim1.setDuration(300);
        anim1.setInterpolator(new AccelerateDecelerateInterpolator());
        dimBgView.setVisibility(View.VISIBLE);
        set.playTogether(anim, anim1);
        set.start();
        isMapListShown = true;
    }

    private void showEventCard() {
        View eventView = eventCardContainer.getChildAt(0);
        Event event = mapMarkerManager.getEvent(lastSelectedMarker);
        eventView = EventCard.getEventCard(
                event, activity, eventView, eventCardContainer, false);
        eventView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return gestureDetector.onTouchEvent(event);
                    }
                });
        eventCardContainer.removeAllViews();
        eventCardContainer.addView(eventView);
    }


    // ***********************
    // Callbacks
    // ***********************

    // This is called when maps camera position is changed (zoom in, zoom out or
    // user dragging the map around). We refresh the events listing if there is
    // change in city otherwise we refresh the event markers shown to user.
    private GoogleMap.OnCameraChangeListener mOnCameraChangeListener = new GoogleMap.OnCameraChangeListener() {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            boolean isInfoWindowShown = mapMarkerManager.updateListingForProjection(map.getProjection());
            if (!isInfoWindowShown) {
                mOnMapClickListener.onMapClick(null);
            }

            if (!eventsContext.changeLocation(cameraPosition.target)) {

                mapMarkerManager.setEvents(map, filteredEvents);
            }
        }
    };

    private GoogleMap.OnMapClickListener mOnMapClickListener = new GoogleMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            if (latLng != null) {
                activity.reportActionToAnalytics("onMapClick");
            }
            lastSelectedMarker = null;
            // eventCardContainer.removeAllViews();
        }
    };

    private GoogleMap.OnMarkerClickListener mOnMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            activity.reportActionToAnalytics("onMarkerClick");
            lastSelectedMarker = marker;
            // showEventCard();
            showEventsListOnMap();
            return false;
        }
    };

    // When user clicks on info window, we open the details screen.
    private GoogleMap.OnInfoWindowClickListener mOnInfoWindowClickListener = new GoogleMap.OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(Marker marker) {
            activity.showEventDetails(mapMarkerManager.getEvent(marker), eventsContext.getLabel(), null);
        }
    };

    private void updateUserLocation(@Nullable LatLng userLocation) {
        if (userLocation == null) {
            // do nothing.
            return;
        }

        map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder()
                                .target(userLocation)
                                .zoom(Math.max(map.getCameraPosition().zoom, DEFAULT_ZOOM_LEVEL))
                                .build()
                )
        );
    }
}
