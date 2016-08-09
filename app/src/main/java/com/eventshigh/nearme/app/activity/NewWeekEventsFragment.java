package com.eventshigh.nearme.app.activity;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventDistanceComparator;
import com.eventshigh.nearme.app.data.EventPriceComparator;
import com.eventshigh.nearme.app.data.EventScoreComparator;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.MapMarkerManager;
import com.eventshigh.nearme.app.ui.adapter.EventCard;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.ui.animation.ResizeAnimation;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.squareup.timessquare.CalendarPickerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by umesh on 29/05/16.
 */
public class NewWeekEventsFragment extends BaseEventsFragment {


    public static NewWeekEventsFragment getInstance(EventsContext eventsContext, boolean showEhInviteForNotification) {
        NewWeekEventsFragment fragment = new NewWeekEventsFragment();
        Bundle args = getArgs(eventsContext, false, false);
        args.putBoolean(SHOW_EH_INVITE_NOTIFICATION_PARAM, showEhInviteForNotification);
        fragment.setArguments(args);
        return fragment;
    }

    private static final String SHOW_EH_INVITE_NOTIFICATION_PARAM =
            EventsFragment.class.getName() + "_show_eh_invite_param";
    private AutofitRecyclerView eventGridView;
    private View topProgressBar;

    private final Calendar todayDate = DateTimeUtils.toMidnight(Calendar.getInstance(), null);

    EventsAdapter eventsAdapter;
    EventCollectionRequest.EventsCollection eventsCollection;
    View noMyEventsView;
    View retryView;

    private boolean showEhInviteForNotification;
    View view;

    LinearLayout filtersContainer;
    HorizontalScrollView dateFilter;

    public static final int SORT_STATE_TRENDING = 1;
    public static final int SORT_STATE_PRICE = 2;
    public static final int SORT_STATE_DISTANCE = 3;


    int sortState = SORT_STATE_TRENDING;

    List<Event> filteredEvents;

    Account account;
    HorizontalScrollView sortFilter;

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

    LinearLayout eventListContainer;

    // FrameLayout eventCardContainer;

    boolean isLoadingComplete;

    HorizontalScrollView priceFilter;

    LinearLayout mapClickedList;

    AutofitRecyclerView mapClickedEvents;

    View dimBgView;

    TextView mapEventsCount;

    boolean isMapListShown;

    boolean isFiltersShown;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = LayoutInflater.from(getActivity()).inflate(R.layout.fragment_by_week, container, false);
        eventsAdapter = new EventsAdapter(activity);
        eventGridView = (AutofitRecyclerView) view.findViewById(R.id.event_grid);
        eventGridView.setAdapter(eventsAdapter);
        filtersContainer = (LinearLayout) view.findViewById(R.id.filters_container);
        noMyEventsView = view.findViewById(R.id.view_no_my_event);

        dateFilter = (HorizontalScrollView) view.findViewById(R.id.date_filter);
        priceFilter = (HorizontalScrollView) view.findViewById(R.id.price_filter);
        sortFilter = (HorizontalScrollView) view.findViewById(R.id.sort_container);
        retryView = view.findViewById(R.id.view_retry);
        retryView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("retry");
                makeServerRequest(false);
            }
        });
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setEnabled(false);

        // Setup the actionbar hide/show on scroll.
        /*((CoordinatorLayout)view.findViewById(R.id.main_content)).setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

            }
        });*/
        eventGridView.addOnScrollListener(onScrollListener);

        topProgressBar = view.findViewById(R.id.top_progress_bar);
        account = new Account(activity);
        eventListContainer = (LinearLayout) view.findViewById(R.id.week_parent);
        mapClickedList = (LinearLayout) view.findViewById(R.id.map_clicked_events);
        mapClickedEvents = (AutofitRecyclerView) view.findViewById(R.id.event_grid_map);
        mapClickedEvents.getParent().requestDisallowInterceptTouchEvent(true);
        dimBgView = view.findViewById(R.id.dim_bg_view);
        dimBgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMapEvents(-1);
            }
        });
        view.findViewById(R.id.cross_img).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMapEvents(-1);
            }
        });
        mapEventsCount = (TextView) view.findViewById(R.id.map_events_count);

        hideMapEvents(-1);
        return view;
    }


    private RecyclerView.OnScrollListener onScrollListener;

    public void onAttach(Context context) {
        super.onAttach(context);

        showEhInviteForNotification = getArguments().getBoolean(SHOW_EH_INVITE_NOTIFICATION_PARAM);
        if (onScrollListener == null) {
            onScrollListener = new HideActionBarOnScroll(this.activity) {
                int y;

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        y = 0;
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    y += dy;
                    if (y > 250) {
                        //collapseAnimation();
                    }
                }
            };
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        addFiltersData();
        makeServerRequest(false);
        if (filterCategoryName != null)
            filterCategoryName.clear();
        if (filterEventPrices != null)
            filterEventPrices.clear();
        if (filterEventTimes != null)
            filterEventTimes.clear();
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    public void makeServerRequest(boolean shouldBypassCache) {
        Calendar endDate = Calendar.getInstance();
        endDate.setFirstDayOfWeek(Calendar.MONDAY);
        endDate.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        endDate.add(Calendar.DAY_OF_WEEK, 8);
        String dateString = EventsContext.formatDateFilter(Calendar.getInstance()) + "," + EventsContext.formatDateFilter(endDate);
        EventCollectionRequest.submit(activity, eventsContext, Request.Priority.IMMEDIATE, this, dateString,
                shouldBypassCache, true, mEventsFetcherCallBack, mErrorListener, new EventCollectionRequest.OnDataProcessComplete() {
                    @Override
                    public void onDataProcessComplete(EventCollectionRequest.EventsCollection eventsCollection, final boolean isLoadingFinished) {
                        if (isDetached()) {
                            return;
                        }
                        if (!eventsCollection.events.isEmpty()) {

                            final String seeAllQuery = eventsContext.query.isEmpty() ||
                                    eventsContext.dateFilter.isEmpty() ? null : eventsContext.query;
                            if (getActivity() != null && (getActivity()) instanceof EventsGridActivity) {
                                if (eventsCollection.events.size() > 0)
                                    ((EventsGridActivity) getActivity()).setShareImageUrl(eventsCollection.events.get(0).imgUrl);
                            }
                            if (isLoadingFinished) {
                                NewWeekEventsFragment.this.eventsCollection.events.addAll(eventsCollection.events);
                            } else {
                                NewWeekEventsFragment.this.eventsCollection = eventsCollection;
                            }


                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (isLoadingFinished) {
                                        topProgressBar.setVisibility(View.GONE);

                                        if (NewWeekEventsFragment.this.eventsCollection.events.isEmpty()) {
                                            // Failed. Show toast and return empty list.
                                            noMyEventsView.setVisibility(View.VISIBLE);
                                            Toast.makeText(getActivity(), R.string.no_events, Toast.LENGTH_SHORT).show();

                                        } else {
                                            isLoadingComplete = true;
                                            if (getActivity() != null) {
                                                filtersContainer.setVisibility(View.VISIBLE);
                                                ((LaunchActivity) getActivity()).fabWriteReviews.setImageResource(R.drawable.ic_browse_map);
                                                ((LaunchActivity) getActivity()).animateFabIn();
                                            }
                                            noMyEventsView.setVisibility(View.GONE);
                                        }


                                    }
                                    List<Event> filteredEvents = NewWeekEventsFragment.this.eventsCollection.events;
                                    filteredEvents = filterEventsWithCategory(null, filteredEvents);
                                    if (filterEventTimes != null) {
                                        filterEventTimes.clear();
                                    }
                                    filteredEvents = filterEventsWithDate(filteredEvents, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                                    filteredEvents = filterEventsWithPrice(filteredEvents, -1);
                                    NewWeekEventsFragment.this.filteredEvents = filteredEvents;
                                    sortData();
                                    eventsAdapter.setEvents(NewWeekEventsFragment.this.filteredEvents, seeAllQuery, false);
                                    eventsContext.removeDateFilter();

                                }
                            });
                            //addSocialInvitationRequests();

                        }
                    }
                });
    }

    private Response.Listener<EventCollectionRequest.EventsCollection> mEventsFetcherCallBack = new Response.Listener<EventCollectionRequest.EventsCollection>() {
        @Override
        public void onResponse(EventCollectionRequest.EventsCollection eventsCollection, boolean isIntermediate) {
            if (eventsCollection != null) {
                if (isDetached()) {
                    return;
                }

                if (!isIntermediate) {
                    topProgressBar.setVisibility(View.GONE);

                    if (eventsCollection.events.isEmpty()) {
                        // Failed. Show toast and return empty list.
                        noMyEventsView.setVisibility(View.VISIBLE);
                        Toast.makeText(getActivity(), R.string.no_events, Toast.LENGTH_SHORT).show();

                    }
                }

                if (!isIntermediate || !eventsCollection.events.isEmpty()) {
                    noMyEventsView.setVisibility(View.GONE);
                    String seeAllQuery = eventsContext.query.isEmpty() ||
                            eventsContext.dateFilter.isEmpty() ? null : eventsContext.query;
                    if (getActivity() != null && (getActivity()) instanceof EventsGridActivity) {
                        if (eventsCollection.events.size() > 0)
                            ((EventsGridActivity) getActivity()).setShareImageUrl(eventsCollection.events.get(0).imgUrl);
                    }
                    NewWeekEventsFragment.this.eventsCollection = eventsCollection;
                    List<Event> filteredEvents = eventsCollection.events;
                    filteredEvents = filterEventsWithCategory(null, filteredEvents);
                    filteredEvents = filterEventsWithDate(filteredEvents, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                    filteredEvents = filterEventsWithPrice(filteredEvents, -1);
                    eventsAdapter.setEvents(filteredEvents, seeAllQuery, false);
                    eventsContext.removeDateFilter();
                /*if (showFollowCard) {
                    eventsAdapter.addFollowCard(eventsContext.query, eventsCollection.events.size(),
                            eventsCollection.numFollowers, special);
                }*/
                    //     eventGridView.scrollToPosition(scrollPosition);
                }
            }
        }
    };

    private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
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
            eventsContext.removeDateFilter();

            VolleyHelper.log(activity, volleyError);

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
               /* eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
                if (filteredEvents.size() > 0) {

                    noMyEventsView.setVisibility(View.GONE);
                } else {
                    noMyEventsView.setVisibility(View.VISIBLE);
                    // Failed. Show toast and return empty list.
                    Toast.makeText(getActivity(), R.string.no_events, Toast.LENGTH_SHORT).show();


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
                /*eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
                if (filteredEvents.size() > 0) {

                    noMyEventsView.setVisibility(View.GONE);
                } else {
                    noMyEventsView.setVisibility(View.VISIBLE);
                    // Failed. Show toast and return empty list.
                    Toast.makeText(getActivity(), R.string.no_events, Toast.LENGTH_SHORT).show();

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

                            priceMax = 50000;
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
                if (filteredEvents.size() > 0) {

                    noMyEventsView.setVisibility(View.GONE);
                } else {
                    noMyEventsView.setVisibility(View.VISIBLE);
                    // Failed. Show toast and return empty list.
                    Toast.makeText(getActivity(), R.string.no_events, Toast.LENGTH_SHORT).show();

                }*/
            }
            return filteredEvents;
        }

        return null;
    }

    View showFiltersView;

    TextView showMoreFilterText;

    TextView selectCustomDates, today, tomorrow, weekend;

    public void addFiltersData() {
        final LinearLayout horizontalCategories = (LinearLayout) view.findViewById(R.id.category_container);
        final String[] categories = EventsGridActivity.EXPLORE_TAGS;
        for (int i = 0; i < categories.length; i++) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
            final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
            filterText.setText(categories[i]);
            horizontalCategories.addView(view);
            filterText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (filterText.isSelected()) {
                        filterText.setSelected(false);
                    } else {
                        filterText.setSelected(true);
                    }
                    if (filterText.getText().toString().equalsIgnoreCase(EventsHighEndpoints.QUERY_FEATURED)) {
                        startFilterAsyncTask(EventsGridActivity.CATEGORY_FILTER, null, "Featured", -1, null);
                        //filterEventsWithCategory("Featured", null);
                    } else {
                        startFilterAsyncTask(EventsGridActivity.CATEGORY_FILTER, null, filterText.getText().toString(), -1, null);
                        //filterEventsWithCategory(filterText.getText().toString(), null);
                    }

                    activity.reportActionToAnalytics("filters", "This week" + "-" + filterText.getText().toString());
                }
            });
        }
        LinearLayout horizontalprice = (LinearLayout) view.findViewById(R.id.price_container);
        String[] priceRanges = {"Free", " \u20B9 ", "\u20B9 \u20B9", "\u20B9 \u20B9 \u20B9", "\u20B9 \u20B9 \u20B9 \u20B9"};
        for (int i = 0; i < priceRanges.length; i++) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
            final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
            filterText.setText(priceRanges[i]);
            horizontalprice.addView(view);
            filterText.setTag(i);
            filterText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (Integer) filterText.getTag();
                    if (position == 0) {
                        startFilterAsyncTask(EventsGridActivity.PRICE_FILTER, null, null, EventsGridActivity.FREE, null);
                        activity.reportActionToAnalytics("filters", eventsContext.query + "-FREE");
                        // filterEventsWithPrice(null, EventsGridActivity.FREE);
                    } else if (position == 1) {
                        startFilterAsyncTask(EventsGridActivity.PRICE_FILTER, null, null, EventsGridActivity.UPTO_250, null);
                        //filterEventsWithPrice(null, EventsGridActivity.UPTO_250);
                        activity.reportActionToAnalytics("filters", eventsContext.query + "uptp250");
                    } else if (position == 2) {
                        startFilterAsyncTask(EventsGridActivity.PRICE_FILTER, null, null, EventsGridActivity.PRICE_250_TO_750, null);
                        //filterEventsWithPrice(null, EventsGridActivity.PRICE_250_TO_750);
                        activity.reportActionToAnalytics("filters", eventsContext.query + "price250to750");
                    } else if (position == 3) {
                        startFilterAsyncTask(EventsGridActivity.PRICE_FILTER, null, null, EventsGridActivity.PRICE_750_TO_1500, null);
                        activity.reportActionToAnalytics
                                ("filters", eventsContext.query + "price750to1500");
                        //filterEventsWithPrice(null, EventsGridActivity.PRICE_750_TO_1500);
                    } else {
                        startFilterAsyncTask(EventsGridActivity.PRICE_FILTER, null, null, EventsGridActivity.MORE_THAN_1500, null);
                        activity.reportActionToAnalytics("filters", eventsContext.query + "moreThan1500");
                        //filterEventsWithPrice(null, EventsGridActivity.MORE_THAN_1500);
                    }
                    if (filterText.isSelected()) {
                        filterText.setSelected(false);
                    } else {
                        filterText.setSelected(true);
                    }
                }
            });
        }

        LinearLayout horizontalDate = (LinearLayout) view.findViewById(R.id.date_container);
        String[] dateRanges = {"Today", "Tomorrow", "Weekend", "Custom Dates", "• • •"};
        for (int i = 0; i < dateRanges.length; i++) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
            final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
            filterText.setText(dateRanges[i]);

            if (i == dateRanges.length - 1) {
                showFiltersView = view;
                showMoreFilterText = filterText;
                showMoreFilterText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
                showMoreFilterText.setTypeface(null, Typeface.BOLD);
            } else if (i == dateRanges.length - 2) {
                selectCustomDates = filterText;
                selectCustomDates.setVisibility(View.GONE);
            } else if (i == dateRanges.length - 3) {
                weekend = filterText;
                weekend.setVisibility(View.GONE);
            } else if (i == dateRanges.length - 4) {
                tomorrow = filterText;
            } else {
                today = filterText;
                filterText.setSelected(true);
            }

            horizontalDate.addView(view);
            filterText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (filterText.getText().toString().equalsIgnoreCase("Today")) {
                        checkIfCustomDateSelected();
                        startFilterAsyncTask(EventsGridActivity.DATE_FILTER, null, null, -1, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());

                        activity.reportActionToAnalytics("filters", "This week" + "-Today");
                        //filterEventsWithDate(null, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                    } else if (filterText.getText().toString().equalsIgnoreCase("Tomorrow")) {
                        checkIfCustomDateSelected();
                        startFilterAsyncTask(EventsGridActivity.DATE_FILTER, null, null, -1, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime() + DateTimeUtils.MILLISECONDS_IN_A_DAY);

                        activity.reportActionToAnalytics("filters", "This week" + "-Tomorrow");
                        // filterEventsWithDate(null, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime() + DateTimeUtils.MILLISECONDS_IN_A_DAY);
                    } else if (filterText.getText().toString().equalsIgnoreCase("Weekend")) {
                        checkIfCustomDateSelected();
                        startFilterAsyncTask(EventsGridActivity.DATE_FILTER, null, null, -1, DateTimeUtils.getWeekEndDates());
                        activity.reportActionToAnalytics("filters", "This week" + "-Weekend");
                        //filterEventsWithDate(null, DateTimeUtils.getWeekEndDates());
                    } else if (filterText.getText().toString().equalsIgnoreCase("Custom Dates")) {
                        showDateDialog();
                        Calendar currentYear = Calendar.getInstance();
                        Calendar nextYear = Calendar.getInstance();
                        nextYear.setFirstDayOfWeek(Calendar.MONDAY);
                        nextYear.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                        nextYear.add(Calendar.DAY_OF_WEEK, 8);

                        if (selectedDates != null) {
                            dialogView.init(currentYear.getTime(), nextYear.getTime())//
                                    .inMode(CalendarPickerView.SelectionMode.MULTIPLE).withSelectedDates(selectedDates);
                        } else {
                            dialogView.init(currentYear.getTime(), nextYear.getTime()) //
                                    .inMode(CalendarPickerView.SelectionMode.MULTIPLE);
                        }
                    } else if (filterText.getText().toString().equalsIgnoreCase("• • •")) {
                        if (isMapListShown) {
                            hideMapEvents(EventsGridActivity.SHOW_FILTERS_STATE);
                        } else {
                            expandAnimation();
                        }
                        activity.reportActionToAnalytics("filters", "This week" + "-Expand");
                        return;
                    } else {
                        collapseAnimation(-1);
                        activity.reportActionToAnalytics("filters", "This week" + "-Collapse");
                        return;
                    }
                    if (!(filterText.getText().toString().equalsIgnoreCase("Custom Dates"))) {
                        if (filterText.isSelected()) {
                            filterText.setSelected(false);
                        } else {
                            filterText.setSelected(true);
                        }
                    }
                }
            });
        }

        collapseAnimation(-1);

        trending = (TextView) view.findViewById(R.id.sort_trending);
        price = (TextView) view.findViewById(R.id.sort_price);
        distance = (TextView) view.findViewById(R.id.sort_distance);
        trending.setSelected(true);
        trending.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(sortState == SORT_STATE_TRENDING)) {
                    trending.setSelected(true);
                    price.setSelected(false);
                    distance.setSelected(false);
                    sortAccToSortState(SORT_STATE_TRENDING);
                    activity.reportActionToAnalytics("filters", "This week" + "-sortTrending");

                }
            }
        });

        price.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!(sortState == SORT_STATE_PRICE)) {
                    trending.setSelected(false);
                    price.setSelected(true);
                    distance.setSelected(false);
                    sortAccToSortState(SORT_STATE_PRICE);
                    activity.reportActionToAnalytics("filters", "This week" + "-sortPrice");
                }
            }
        });

        distance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (account.getLastLocality() != null) {
                    if (!(sortState == SORT_STATE_DISTANCE)) {
                        trending.setSelected(false);
                        price.setSelected(false);
                        distance.setSelected(true);
                        sortAccToSortState(SORT_STATE_DISTANCE);
                        activity.reportActionToAnalytics("filters", "This week" + "-sortDistance");
                    }
                } else {
                    isDistanceClicked = true;
                    Intent intent = new Intent(getActivity(), PlacesAutocompleteBoundedActivity.class);
                    getActivity().startActivityForResult(intent, LaunchActivity.PLACE_AUTOCOMPLETE_REQUEST_CODE);
                }
            }
        });
    }

    TextView trending, price, distance;
    boolean isDistanceClicked;

    @Override
    public void onResume() {
        super.onResume();
        setSelectedLocalityDistanceText();
        if (isDistanceClicked || ((LaunchActivity) getActivity()).isLocalityUpdated()) {
            ((LaunchActivity) getActivity()).setIsLocalityUpdated(false);
            if (account.getLastLocality() != null)
                makeDistanceSortTrue();
            isDistanceClicked = false;
        }
    }

    public void makeDistanceSortTrue() {
        distance.setSelected(true);
        price.setSelected(false);
        trending.setSelected(false);
        sortAccToSortState(SORT_STATE_DISTANCE);
        sortFilter.postDelayed(new Runnable() {
            @Override
            public void run() {
                sortFilter.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
            }
        }, 100L);

    }

    public void setSelectedLocalityDistanceText() {
        if (account.getLastLocality() != null)
            distance.setText("Distance from " + Utils.capitalize(account.getLastLocality().getName()));
    }

    public void sortAccToSortState(int sortState) {
        if (filteredEvents != null) {
            this.sortState = sortState;
            sortData();
            eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
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
                    Collections.sort(filteredEvents, new EventDistanceComparator(account.getLastLocality().getLatLng()));
                else
                    Collections.sort(filteredEvents, new EventDistanceComparator(account.getLastCity().cityBounds.getCenter()));
            }
        }


    }

    CalendarPickerView dialogView;

    public void showDateDialog() {
        dialogView = (CalendarPickerView) LayoutInflater.from(getActivity()).inflate(R.layout.date_picker_dialog_layout, null, false);
        AlertDialog theDialog = new AlertDialog.Builder(getActivity()) //
                .setTitle(null)
                .setView(dialogView)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                }).setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getSelectedDates();
                        dialog.dismiss();
                    }
                })
                .create();
        theDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {

                dialogView.fixDialogDimens();
            }
        });
        theDialog.show();

    }

    List<Date> selectedDates;

    public void getSelectedDates() {
        selectedDates = dialogView.getSelectedDates();
        if (selectedDates != null && selectedDates.size() > 0) {
            removeAllSelectedDateFilters();
            selectCustomDates.setSelected(true);
            long[] dates = new long[selectedDates.size()];
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < selectedDates.size(); i++) {
                if (i != 0) {
                    builder.append("/");
                }
                dates[i] = selectedDates.get(i).getTime();
                builder.append(DateTimeUtils.getDateFromMillisTime(selectedDates.get(i).getTime()));
            }
            startFilterAsyncTask(EventsGridActivity.DATE_FILTER, null, null, -1, dates);
            activity.reportActionToAnalytics("filters", "This week" + "-" + builder.toString());
            //filterEventsWithDate(null, dates);
        }
    }

    public void removeAllSelectedDateFilters() {
        if (today.isSelected()) {
            today.setSelected(false);
        }
        if (tomorrow.isSelected()) {
            tomorrow.setSelected(false);
        }
        if (weekend.isSelected()) {
            weekend.setSelected(false);
        }

        if (filterEventTimes != null)
            filterEventTimes.clear();
    }

    public void checkIfCustomDateSelected() {
        if (selectCustomDates != null && selectCustomDates.isSelected()) {
            selectCustomDates.setSelected(false);
            if (filterEventTimes != null)
                filterEventTimes.clear();
        }
    }

    public void expandAnimation() {
        int targetHeight;
        if (!isMapShown) {
            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (56 * 4), getResources().getDisplayMetrics());
        } else {
            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (56 * 3), getResources().getDisplayMetrics());
        }
        ResizeAnimation resizeAnimation = new ResizeAnimation(filtersContainer, targetHeight);
        resizeAnimation.setDuration(100);
        resizeAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                showMoreFilterText.setText("");
                if (!weekend.isShown())
                    weekend.setVisibility(View.VISIBLE);
                if (!selectCustomDates.isShown()) {
                    selectCustomDates.setVisibility(View.VISIBLE);
                    dateFilter.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                    dateFilter.postDelayed(new Runnable() {
                        public void run() {
                            dateFilter.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                        }
                    }, 100L);
                }
                addDrawable(R.drawable.ic_action_highlight_remove);
                isFiltersShown = true;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        filtersContainer.startAnimation(resizeAnimation);
    }

    public void collapseAnimation(final int hideValue) {
        ResizeAnimation resizeAnimation = new ResizeAnimation(filtersContainer, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (56), getResources().getDisplayMetrics()));
        resizeAnimation.setDuration(100);
        resizeAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isFiltersShown = false;
                showMoreFilterText.setText("• • •");
                addDrawable(-1);
                if (hideValue == EventsGridActivity.SHOW_MAP_EVENTS_LIST) {
                    bringMapEventsVisible();
                } else if (hideValue == EventsGridActivity.SHOW_MAP_VIEW) {
                    hideFilterComponents(HIDE_SORT);
                    hideListView();
                } else {
                    hideFilterComponents(hideValue);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        filtersContainer.startAnimation(resizeAnimation);
    }

    public void addDrawable(int resId) {
        Drawable drawableLeft = null;
        if (resId != -1) {
            drawableLeft = getResources().getDrawable(resId);
            drawableLeft.setBounds(0, 0, drawableLeft.getIntrinsicWidth(), drawableLeft.getIntrinsicHeight());

        }
        showMoreFilterText.setCompoundDrawables(drawableLeft, null, null, null);
    }

    FilterAsyncTask filterAsyncTask;

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
                    ((BaseContextActivity) getActivity()).reportActionToAnalytics("filterClick", "priceFilter", 1, priceValue + "");
                    return filterEventsWithPrice(totalEvents, priceValue);
                case EventsGridActivity.DATE_FILTER:
                    ((BaseContextActivity) getActivity()).reportActionToAnalytics("filterClick", "dateFilter", 1, getTimesString(times));
                    return filterEventsWithDate(totalEvents, times);
                case EventsGridActivity.CATEGORY_FILTER:
                    ((BaseContextActivity) getActivity()).reportActionToAnalytics("filterClick", "categoryFilter", 1, category);
                    return filterEventsWithCategory(category, totalEvents);

            }
            return null;
        }

        @Override
        protected void onPostExecute(List<Event> events) {

            if (!isCancelled() && events != null) {
                topProgressBar.setVisibility(View.GONE);
                NewWeekEventsFragment.this.filteredEvents = events;
                if (isMapShown) {
                    mapMarkerManager.setEvents(map, filteredEvents);
                } else {
                    sortData();
                    eventsAdapter.setEvents(filteredEvents, null, showEhInviteForNotification);
                }
                if (events.size() > 0) {
                    noMyEventsView.setVisibility(View.GONE);
                } else {
                    noMyEventsView.setVisibility(View.VISIBLE);
                    Toast.makeText(getActivity(), R.string.no_events, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public String getTimesString(long... times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times.length; i++) {
            if (i != 0) {
                builder.append(",");
            }
            builder.append(times[i] + "");
        }
        return builder.toString();
    }


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

        isMapShown = false;
        ObjectAnimator anim = ObjectAnimator.ofFloat(eventListContainer, View.TRANSLATION_Y, MOVE_VIEW_TO_POS, 0);
        anim.setDuration(500);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.start();
    }

    public void setUpMapContents() {
        if (map == null) {
            FragmentManager fm = getChildFragmentManager();
            mapFragment = (SupportMapFragment) fm.findFragmentByTag("supportMapFragment");
            if (mapFragment == null) {
                mapFragment = new SupportMapFragment();
                FragmentTransaction ft = fm.beginTransaction();
                ft.add(R.id.map_container, mapFragment, "supportMapFragment");
                ft.commit();
                fm.executePendingTransactions();
            }
            setUpMap();
            setupGestureDetectorIfNeeded();
        } else {
            mapMarkerManager.setEvents(map, filteredEvents);
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
        if (isFiltersShown) {
            collapseAnimation(EventsGridActivity.SHOW_MAP_EVENTS_LIST);
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
                    expandAnimation();
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
    /*private void showEventCard() {
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
    }*/


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

        }
    };

    private GoogleMap.OnMarkerClickListener mOnMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            activity.reportActionToAnalytics("onMarkerClick");
            lastSelectedMarker = marker;
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

    public static final int SHOW_SORT = 1;
    public static final int HIDE_SORT = 2;

    public void hideFilterComponents(int hideValue) {
        if (hideValue == SHOW_SORT) {
            view.findViewById(R.id.sort_separator).setVisibility(View.VISIBLE);
            sortFilter.setVisibility(View.VISIBLE);
        } else if (hideValue == HIDE_SORT) {
            view.findViewById(R.id.sort_separator).setVisibility(View.GONE);
            sortFilter.setVisibility(View.GONE);
        }

    }
}
