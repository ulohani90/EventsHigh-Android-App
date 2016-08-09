package com.eventshigh.nearme.app.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;


import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.ui.MapMarkerManager;
import com.eventshigh.nearme.app.ui.adapter.EventCard;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 05/08/16.
 */
public class EventsMapFragment extends Fragment {

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

    boolean showThisWeekView;
    public static final String IS_TODAY_SELECTED = "is_today_selected";
    public static final String IS_CATEGORY_FILTER_VISIBLE = "is_category_filter_visible";
    public static final String IS_THIS_WEEK_VIEW = "is_this_week_view";

    List<Event> eventsList;
    BaseContextActivity activity;

    EventsContext eventsContext;

    boolean loadMap;

    public static EventsMapFragment newInstance(EventsContext eventsContext, ArrayList<Event> events, boolean loadMap) {
        Bundle args = new Bundle();
        args.putParcelable("events_context", eventsContext);
        args.putParcelableArrayList("events", events);
        args.putBoolean("load_map", loadMap);
        EventsMapFragment fragment = new EventsMapFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (BaseContextActivity) context;
        eventsContext = getArguments().getParcelable("events_context");
        eventsList = getArguments().getParcelableArrayList("events");
    }

    // ***********************
    // Delegated Methods from {@link BaseEventsActivity}
    // ***********************

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    MapFragment mapFragment;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_maps, container, false);

        FragmentManager fm = getChildFragmentManager();
        mapFragment = (MapFragment) fm.findFragmentByTag("mapFragment");
        if (mapFragment == null) {
            mapFragment = new MapFragment();
            FragmentTransaction ft = fm.beginTransaction();
            ft.add(R.id.map_container, mapFragment, "mapFragment");
            ft.commit();
            fm.executePendingTransactions();
        }
        // Setup the view.


        eventCardContainer = (FrameLayout) view.findViewById(R.id.event_card_container);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setUpMap();
        setupGestureDetectorIfNeeded();
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

    }


    // ***********************
    // Helper Methods
    // ***********************


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
                List<Event> filteredEvents = filterEventsWithCategory(null, null);
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
            eventCardContainer.removeAllViews();
        }
    };

    private GoogleMap.OnMarkerClickListener mOnMarkerClickListener = new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            activity.reportActionToAnalytics("onMarkerClick");
            lastSelectedMarker = marker;
            showEventCard();
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
/*

    private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);
            activity.showMessage(R.string.failed_load);
        }
    };

    private Response.Listener<EventCollectionRequest.EventsCollection> mEventsFetcherCallBack = new Response.Listener<EventCollectionRequest.EventsCollection>() {
        @Override
        public void onResponse(EventCollectionRequest.EventsCollection eventsCollection, boolean isIntermediate) {
            if (eventsCollection != null) {
                EventsMapFragment.this.eventsCollection = eventsCollection;
                topProgressBar.setVisibility(isIntermediate ? View.VISIBLE : View.GONE);
                mOnMapClickListener.onMapClick(null);
                List<Event> filteredEvents = EventsMapFragment.this.eventsCollection.events;
                filteredEvents = filterEventsWithCategory(null, filteredEvents);
                if (isTodaySelected) {
                    if (filterEventTimes != null)
                        filterEventTimes.clear();
                    filteredEvents = filterEventsWithDate(filteredEvents, DateTimeUtils.getCurrentDate(System.currentTimeMillis()).getTime());
                } else {
                    filteredEvents = filterEventsWithDate(filteredEvents, -1);
                }

                filteredEvents = filterEventsWithPrice(filteredEvents, -1);
                EventsMapFragment.this.filteredEvents = filteredEvents;

                mapMarkerManager.setEvents(map, eventsCollection.events);
            }
        }
    };

    private Response.Listener<MyEventsRequest.MeEventFavouriteObject> mMyFavEventsFetcherCallBack = new Response.Listener<MyEventsRequest.MeEventFavouriteObject>() {
        @Override
        public void onResponse(MyEventsRequest.MeEventFavouriteObject myEvents, boolean isIntermediate) {
            if (!isDetached()) {
                return;
            }

            Set<Event> events = new HashSet<>();
            for (MyEventsRequest.TopicEvents topicEvents : myEvents.topicEvents) {
                events.addAll(topicEvents.events);
            }

            mEventsFetcherCallBack.onResponse(new EventCollectionRequest.EventsCollection(new ArrayList<>(events), 0),
                    isIntermediate);
        }
    };
    private Response.Listener<List<MyEventsRequest.TopicEvents>> mMyEventsFetcherCallBack = new Response.Listener<List<MyEventsRequest.TopicEvents>>() {
        @Override
        public void onResponse(List<MyEventsRequest.TopicEvents> myEvents, boolean isIntermediate) {
            if (!isDetached()) {
                return;
            }

            Set<Event> events = new HashSet<>();
            for (MyEventsRequest.TopicEvents topicEvents : myEvents) {
                events.addAll(topicEvents.events);
            }

            mEventsFetcherCallBack.onResponse(new EventCollectionRequest.EventsCollection(new ArrayList<>(events), 0),
                    isIntermediate);
        }
    };
*/

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

    boolean isTodaySelected;

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
        if (totalEvents == null && eventsList != null) {

            allEvents = eventsList;
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

        if (totalEvents == null && eventsList != null) {

            allEvents = eventsList;
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
        if (totalEvents == null && eventsList != null) {

            allEvents = eventsList;
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


    public void updatedEventsList(ArrayList<Event> events) {
        this.eventsList = events;
    }
}
