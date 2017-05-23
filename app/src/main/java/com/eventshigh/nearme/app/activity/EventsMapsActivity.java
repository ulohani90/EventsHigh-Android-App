package com.eventshigh.nearme.app.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GestureDetectorCompat;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.EventCollectionRequest.EventsCollection;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
import com.eventshigh.nearme.app.ui.adapter.EventCard;
import com.eventshigh.nearme.app.ui.MapMarkerManager;
import com.eventshigh.nearme.app.ui.animation.ResizeAnimation;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.squareup.timessquare.CalendarPickerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * Maps activity which shows users events happening in given locality. The events are marked
 * across Map and user can zoom in, zoom out or move around the map to discover more events.
 */
public class EventsMapsActivity extends BaseContextActivity {

    // ***********************
    // CONSTANTS
    // ***********************

    // For performance reasons, we show events only where user has reasonable zoom level.
    public static final int MIN_ZOOM_LEVEL = 9;
    public static final int DEFAULT_ZOOM_LEVEL = 14;


    // ***********************
    // MEMBERS
    // ***********************

    // Google Map View shows to user using MapFragment.
    private GoogleMap map;
    // ProgressBar.
    private View topProgressBar;
    // Gesture detector.
    private GestureDetectorCompat gestureDetector;
    // Manager for all markers drawn on map. Manager is responsible for hiding/showing markers
    // on map.
    private MapMarkerManager mapMarkerManager = new MapMarkerManager();
    // FrameLayout holding the Event Card.
    private FrameLayout eventCardContainer;
    // Last marker for which the event info card is shown.
    private Marker lastSelectedMarker;

    LinearLayout filtersContainer;

    HorizontalScrollView categoryFilter, priceFilter, dateFilter;

    EventsCollection eventsCollection;

    boolean showThisWeekView;
    public static final String IS_TODAY_SELECTED = "is_today_selected";
    public static final String IS_CATEGORY_FILTER_VISIBLE = "is_category_filter_visible";
    public static final String IS_THIS_WEEK_VIEW = "is_this_week_view";


    // ***********************
    // Delegated Methods from {@link BaseEventsActivity}
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_event_maps);
        isTodaySelected = getIntent().getBooleanExtra(IS_TODAY_SELECTED, false);
        isCategoryFilterVisible = getIntent().getBooleanExtra(IS_CATEGORY_FILTER_VISIBLE, false);
        showThisWeekView = getIntent().getBooleanExtra(IS_THIS_WEEK_VIEW, false);

        filtersContainer = (LinearLayout) findViewById(R.id.filters_container);
        categoryFilter = (HorizontalScrollView) findViewById(R.id.category_filter);
        if (!isCategoryFilterVisible) {
            categoryFilter.setVisibility(View.GONE);
        }
        dateFilter = (HorizontalScrollView) findViewById(R.id.date_filter);
        priceFilter = (HorizontalScrollView) findViewById(R.id.price_filter);
        if (eventsContext.location == null) {
            City lastCity = new Account(this).getLastCity();
            if (lastCity != null) {
                reportActionToAnalytics("usedLastCity");
                eventsContext.changeLocation(lastCity.cityBounds.getCenter());
            }
        }

        // Show query as title.
        if (!eventsContext.query.isEmpty()) {
            setTitle();
        }

        // Setup the view.
        topProgressBar = findViewById(R.id.top_progress_bar);
        setUpMap();
        setupGestureDetectorIfNeeded();

        eventCardContainer = (FrameLayout) findViewById(R.id.event_card_container);
        if (eventsContext.location == null) {
            finish();
        }
        addFiltersData();
    }

    // ***********************
    // Helper Methods
    // ***********************


    private void fetchEvents() {
        topProgressBar.setVisibility(View.VISIBLE);
        if (showThisWeekView || !eventsContext.query.isEmpty() && eventsContext.query.equalsIgnoreCase("today")) {
            Calendar endDate = Calendar.getInstance();
            endDate.setFirstDayOfWeek(Calendar.MONDAY);
            endDate.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            endDate.add(Calendar.DAY_OF_WEEK, 8);
            String dateString = EventsContext.formatDateFilter(Calendar.getInstance()) + "," + EventsContext.formatDateFilter(endDate);
            EventCollectionRequest.submit(this, eventsContext, Request.Priority.IMMEDIATE, this, dateString,
                    false, true, mEventsFetcherCallBack, mErrorListener, new EventCollectionRequest.OnDataProcessComplete() {
                        @Override
                        public void onDataProcessComplete(final EventsCollection eventsCollection, final boolean isLoadingFinished) {
                            if (!isRunning()) {
                                return;
                            }

                            if (!(eventsCollection.events.isEmpty())) {
                                final String seeAllQuery = eventsContext.query.isEmpty() ||
                                        eventsContext.dateFilter.isEmpty() ? null : eventsContext.query;

                                if (isLoadingFinished) {
                                    EventsMapsActivity.this.eventsCollection.events.addAll(eventsCollection.events);
                                } else {
                                    EventsMapsActivity.this.eventsCollection = eventsCollection;
                                }


                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (isLoadingFinished) {

                                            topProgressBar.setVisibility(View.GONE);

                                            if (eventsCollection.events.isEmpty()) {
                                                Snackbar.make(topProgressBar, R.string.no_events, Snackbar.LENGTH_SHORT).show();
                                            }
                                        }
                                        List<Event> filteredEvents = EventsMapsActivity.this.eventsCollection.events;
                                        filteredEvents = filterEventsWithCategory(null, filteredEvents);
                                        if (isTodaySelected) {
                                            if (filterEventTimes != null)
                                                filterEventTimes.clear();
                                            filteredEvents = filterEventsWithDate(filteredEvents, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                                        } else {
                                            filteredEvents = filterEventsWithDate(filteredEvents, -1);
                                        }

                                        filteredEvents = filterEventsWithPrice(filteredEvents, -1);
                                        EventsMapsActivity.this.filteredEvents = filteredEvents;
                                        mapMarkerManager.setEvents(map, EventsMapsActivity.this.filteredEvents);
                                    }
                                });

                            }
                        }
                    });

        } else if (EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
            new MyEventsRequest(this, eventsContext, Priority.IMMEDIATE, this,
                    false, false, mMyFavEventsFetcherCallBack, mErrorListener).execute();
        } else {
            EventCollectionRequest.submit(this, eventsContext, Priority.IMMEDIATE, this,
                    false, false, mEventsFetcherCallBack, mErrorListener);
        }
    }

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

    @Override
    protected void onResume() {
        super.onResume();
        setLightToolbarIcons();
    }

    private void setLightToolbarIcons() {
        toolbar.post(new Runnable() {
            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.white), EventsMapsActivity.this);
            }
        });
    }

    private void setUpMap() {
        // Try to obtain the map from the SupportMapFragment.
        ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(new OnMapReadyCallback() {
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
            gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent event) {
                    return true;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    Event event = mapMarkerManager.getEvent(lastSelectedMarker);
                    if (event != null) {
                        showEventDetails(event,
                                eventsContext.getLabel(), null);
                        return true;
                    } else {
                        return false;
                    }
                }

                @Override
                public boolean onFling(MotionEvent event1, MotionEvent event2,
                                       float velocityX, float velocityY) {
                    if (Math.abs(velocityY) > Math.abs(velocityX)) {
                        // this is either up or down movement, ignore.
                        reportActionToAnalytics("swipeVertical");
                        return false;
                    }

                    reportActionToAnalytics("swipe");
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

    private void showEventCard() {
        View eventView = eventCardContainer.getChildAt(0);
        Event event = mapMarkerManager.getEvent(lastSelectedMarker);
        if (event != null) {
            eventView = EventCard.getEventCard(
                    event, EventsMapsActivity.this, eventView, eventCardContainer, false);
            eventView.setOnTouchListener(
                    new OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            return gestureDetector.onTouchEvent(event);
                        }
                    });
            eventCardContainer.removeAllViews();
            eventCardContainer.addView(eventView);
        }
    }


    // ***********************
    // Callbacks
    // ***********************

    // This is called when maps camera position is changed (zoom in, zoom out or
    // user dragging the map around). We refresh the events listing if there is
    // change in city otherwise we refresh the event markers shown to user.
    private OnCameraChangeListener mOnCameraChangeListener = new OnCameraChangeListener() {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            boolean isInfoWindowShown = mapMarkerManager.updateListingForProjection(map.getProjection());
            if (!isInfoWindowShown) {
                mOnMapClickListener.onMapClick(null);
            }

            if (!eventsContext.changeLocation(cameraPosition.target)) {
                fetchEvents();
            }
        }
    };

    private OnMapClickListener mOnMapClickListener = new OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            if (latLng != null) {
                reportActionToAnalytics("onMapClick");
            }
            lastSelectedMarker = null;
            eventCardContainer.removeAllViews();
        }
    };

    private OnMarkerClickListener mOnMarkerClickListener = new OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            reportActionToAnalytics("onMarkerClick");
            lastSelectedMarker = marker;
            showEventCard();
            return false;
        }
    };

    // When user clicks on info window, we open the details screen.
    private OnInfoWindowClickListener mOnInfoWindowClickListener = new OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(Marker marker) {
            Event event = mapMarkerManager.getEvent(marker);
            if (event != null) {
                showEventDetails(event, eventsContext.getLabel(), null);
            } else {

            }

        }
    };

    private ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);
            showMessage(R.string.failed_load);
        }
    };

    private Listener<EventsCollection> mEventsFetcherCallBack = new Listener<EventsCollection>() {
        @Override
        public void onResponse(EventsCollection eventsCollection, boolean isIntermediate) {
            if (eventsCollection != null) {
                EventsMapsActivity.this.eventsCollection = eventsCollection;
                topProgressBar.setVisibility(isIntermediate ? View.VISIBLE : View.GONE);
                mOnMapClickListener.onMapClick(null);
                List<Event> filteredEvents = EventsMapsActivity.this.eventsCollection.events;
                filteredEvents = filterEventsWithCategory(null, filteredEvents);
                if (isTodaySelected) {
                    if (filterEventTimes != null)
                        filterEventTimes.clear();
                    filteredEvents = filterEventsWithDate(filteredEvents, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                } else {
                    filteredEvents = filterEventsWithDate(filteredEvents, -1);
                }

                filteredEvents = filterEventsWithPrice(filteredEvents, -1);
                EventsMapsActivity.this.filteredEvents = filteredEvents;

                mapMarkerManager.setEvents(map, eventsCollection.events);
            }
        }
    };

    private Listener<MyEventsRequest.MeEventFavouriteObject> mMyFavEventsFetcherCallBack = new Listener<MyEventsRequest.MeEventFavouriteObject>() {
        @Override
        public void onResponse(MyEventsRequest.MeEventFavouriteObject myEvents, boolean isIntermediate) {
            if (isFinishing()) {
                return;
            }

            Set<Event> events = new HashSet<>();
            for (TopicEvents topicEvents : myEvents.topicEvents) {
                events.addAll(topicEvents.events);
            }

            mEventsFetcherCallBack.onResponse(new EventsCollection(new ArrayList<>(events), 0),
                    isIntermediate);
        }
    };
    private Listener<List<TopicEvents>> mMyEventsFetcherCallBack = new Listener<List<TopicEvents>>() {
        @Override
        public void onResponse(List<TopicEvents> myEvents, boolean isIntermediate) {
            if (isFinishing()) {
                return;
            }

            Set<Event> events = new HashSet<>();
            for (TopicEvents topicEvents : myEvents) {
                events.addAll(topicEvents.events);
            }

            mEventsFetcherCallBack.onResponse(new EventsCollection(new ArrayList<>(events), 0),
                    isIntermediate);
        }
    };


    boolean isFiltersShown;

    public static final String[] EXPLORE_TAGS = {
            EventCategory.NIGHTLIFE.categoryName,
            EventCategory.LIVE_PERFORMANCES.categoryName,
            EventCategory.OUTDOORS.categoryName,
            EventCategory.HEALTH_WELLNESS.categoryName,
            EventCategory.KIDS_ENTERTAINMENT.categoryName,
            EventCategory.SPORTS.categoryName,
            EventCategory.WORKSHOPS.categoryName,
            EventCategory.TECH.categoryName,
            EventCategory.ART.categoryName,
            EventCategory.FOOD.categoryName

    };
    TextView showMoreFilterText;

    TextView selectCustomDates, today, tomorrow, weekend;

    View showFiltersView;

    boolean isTodaySelected;

    public static final int PRICE_FILTER = 3;
    public static final int CATEGORY_FILTER = 1;
    public static final int DATE_FILTER = 2;


    public static final int FREE = 1;
    public static final int UPTO_250 = 2;
    public static final int PRICE_250_TO_750 = 3;
    public static final int PRICE_750_TO_1500 = 4;
    public static final int MORE_THAN_1500 = 5;
    boolean isCategoryFilterVisible;

    public void addFiltersData() {
        isFiltersShown = true;
        final LinearLayout horizontalCategories = (LinearLayout) findViewById(R.id.category_container);
        final String[] categories = EXPLORE_TAGS;
        for (int i = 0; i < categories.length; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
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

                        startFilterAsyncTask(CATEGORY_FILTER, null, "Featured", -1, null);
                        //eventsFragment.filterEventsWithCategory("Featured", null);
                    } else {
                        startFilterAsyncTask(CATEGORY_FILTER, null, filterText.getText().toString(), -1, null);
                        //eventsFragment.filterEventsWithCategory(filterText.getText().toString(), null);
                    }
                    reportActionToAnalytics("filters", eventsContext.query + "-" + filterText);
                }
            });
        }

        LinearLayout horizontalprice = (LinearLayout) findViewById(R.id.price_container);
        String[] priceRanges = {"Free", " \u20B9 ", "\u20B9 \u20B9", "\u20B9 \u20B9 \u20B9", "\u20B9 \u20B9 \u20B9 \u20B9"};
        for (int i = 0; i < priceRanges.length; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
            final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
            filterText.setText(priceRanges[i]);
            horizontalprice.addView(view);
            filterText.setTag(i);
            filterText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = (Integer) filterText.getTag();
                    if (position == 0) {
                        startFilterAsyncTask(PRICE_FILTER, null, null, FREE, null);
                        // eventsFragment.filterEventsWithPrice(null, FREE);
                    } else if (position == 1) {
                        startFilterAsyncTask(PRICE_FILTER, null, null, UPTO_250, null);
                        //eventsFragment.filterEventsWithPrice(null, UPTO_250);
                    } else if (position == 2) {
                        startFilterAsyncTask(PRICE_FILTER, null, null, PRICE_250_TO_750, null);
                        //eventsFragment.filterEventsWithPrice(null, PRICE_250_TO_750);
                    } else if (position == 3) {
                        startFilterAsyncTask(PRICE_FILTER, null, null, PRICE_750_TO_1500, null);
                        //eventsFragment.filterEventsWithPrice(null, PRICE_750_TO_1500);
                    } else {
                        startFilterAsyncTask(PRICE_FILTER, null, null, MORE_THAN_1500, null);
                        //eventsFragment.filterEventsWithPrice(null, MORE_THAN_1500);
                    }

                    if (filterText.isSelected()) {
                        filterText.setSelected(false);
                    } else {
                        filterText.setSelected(true);
                    }
                }
            });
        }
        collapseAnimation();
        LinearLayout horizontalDate = (LinearLayout) findViewById(R.id.date_container);
        String[] dateRanges = {"Today", "Tomorrow", "Weekend", "Custom Dates", "\u2022 • •"};
        for (int i = 0; i < dateRanges.length; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.filter_tags_layout, horizontalCategories, false);
            final TextView filterText = (TextView) view.findViewById(R.id.filter_text);
            filterText.setText(dateRanges[i]);
            if (i == dateRanges.length - 1) {
                showFiltersView = view;
                showMoreFilterText = filterText;
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
                if (isTodaySelected) {
                    today.setSelected(true);
                } else {
                    today.setSelected(false);
                }
            }


            horizontalDate.addView(view);
            filterText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (filterText.getText().toString().equalsIgnoreCase("Today")) {
                        checkIfCustomDateSelected();
                        startFilterAsyncTask(DATE_FILTER, null, null, -1, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                        reportActionToAnalytics("filters", eventsContext.query + "-Today");

                        //eventsFragment.filterEventsWithDate(null, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                    } else if (filterText.getText().toString().equalsIgnoreCase("Tomorrow")) {
                        checkIfCustomDateSelected();
                        startFilterAsyncTask(DATE_FILTER, null, null, -1, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime() + DateTimeUtils.MILLISECONDS_IN_A_DAY);
                        reportActionToAnalytics("filters", eventsContext.query + "-Tomorrow");
                        // eventsFragment.filterEventsWithDate(null, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime() + DateTimeUtils.MILLISECONDS_IN_A_DAY);
                    } else if (filterText.getText().toString().equalsIgnoreCase("Weekend")) {
                        checkIfCustomDateSelected();
                        startFilterAsyncTask(DATE_FILTER, null, null, -1, DateTimeUtils.getWeekEndDates());
                        reportActionToAnalytics("filters", eventsContext.query + "-Weekend");
                        //eventsFragment.filterEventsWithDate(null, DateTimeUtils.getWeekEndDates());
                    } else if (filterText.getText().toString().equalsIgnoreCase("Custom Dates")) {

                        showDateDialog();
                        Calendar currentYear = Calendar.getInstance();
                        Calendar nextYear = Calendar.getInstance();
                        nextYear.add(Calendar.DAY_OF_MONTH, 45);

                        if (selectedDates != null) {
                            dialogView.init(currentYear.getTime(), nextYear.getTime()).inMode(CalendarPickerView.SelectionMode.MULTIPLE).withSelectedDates(selectedDates);

                        } else {
                            dialogView.init(currentYear.getTime(), nextYear.getTime()) //
                                    .inMode(CalendarPickerView.SelectionMode.MULTIPLE);
                        }
                    } else if (filterText.getText().toString().equalsIgnoreCase("• • •")) {
                        expandAnimation();
                        reportActionToAnalytics("filters", eventsContext.query + "-Expand");
                        return;
                    } else {
                        collapseAnimation();
                        reportActionToAnalytics("filters", eventsContext.query + "-Collapse");
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
    }

    public void expandAnimation() {
        int targetHeight;
        if (isCategoryFilterVisible) {
            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (56 * 3), getResources().getDisplayMetrics());
        } else {
            targetHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (56 * 2), getResources().getDisplayMetrics());
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
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        filtersContainer.startAnimation(resizeAnimation);
    }

    public void collapseAnimation() {
        ResizeAnimation resizeAnimation = new ResizeAnimation(filtersContainer, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (56), getResources().getDisplayMetrics()));
        resizeAnimation.setDuration(100);
        resizeAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                addDrawable(-1);
                showMoreFilterText.setText("• • •");

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

            if (!isCancelled()) {
                topProgressBar.setVisibility(View.GONE);
                filteredEvents = events;
                mapMarkerManager.setEvents(map, filteredEvents);

            }
        }
    }

    public List<Event> filteredEvents;
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
                        for (int k = 0; k < allEvents.get(i).eventTimings.size(); k++) {
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

    public void checkIfCustomDateSelected() {
        if (selectCustomDates.isSelected()) {
            selectCustomDates.setSelected(false);
            filterEventTimes.clear();
        }
    }

    CalendarPickerView dialogView;

    public void showDateDialog() {
        dialogView = (CalendarPickerView) getLayoutInflater().inflate(R.layout.date_picker_dialog_layout, null, false);
        AlertDialog theDialog = new AlertDialog.Builder(this) //
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

    List<Date> selectedDates;

    public void getSelectedDates() {
        selectedDates = dialogView.getSelectedDates();
        if (selectedDates != null && selectedDates.size() > 0) {
            removeAllSelectedDateFilters();
            selectCustomDates.setSelected(true);
            long[] dates = new long[selectedDates.size()];

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < selectedDates.size(); i++) {
                dates[i] = selectedDates.get(i).getTime();
                if (i != 0) {
                    builder.append("/");
                }
                builder.append(DateTimeUtils.getDateFromMillisTime(selectedDates.get(i).getTime()));
            }
            startFilterAsyncTask(DATE_FILTER, null, null, -1, dates);
            reportActionToAnalytics("filters", eventsContext.query + "-" + builder.toString());
            //eventsFragment.filterEventsWithDate(null, dates);
        }
    }
}
