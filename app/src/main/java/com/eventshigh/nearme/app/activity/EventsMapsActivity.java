package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.EventCollectionRequest.EventsCollection;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.eventshigh.nearme.app.ui.MapMarkerManager;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maps activity which shows users events happening in given locality. The events are marked
 * across Map and user can zoom in, zoom out or move around the map to discover more events.
 */
public class EventsMapsActivity extends BaseEventsActivity {

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
    // is the movement in camera position is because of app ?
    private boolean isAppMovement = true;

    // ***********************
    // Delegated Methods from {@link BaseEventsActivity}
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the UI.
        getLayoutInflater().inflate(R.layout.activity_event_maps, eventContainer);
        topProgressBar = findViewById(R.id.top_progress_bar);
        setUpMap();
        setupGestureDetectorIfNeeded();

        eventCardContainer = (FrameLayout) findViewById(R.id.event_card_container);
    }

    @Override
    protected  int getDisabledMenuItem() {
        return R.id.action_show_map;
    }

    @Override
    protected void showEvents() {
        // do nothing.
    }


    // ***********************
    // Helper Methods
    // ***********************

    private void fetchEvents() {
        topProgressBar.setVisibility(View.VISIBLE);
        if (EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
            new MyEventsRequest(this, eventsContext, Priority.IMMEDIATE, this,
                    false, true, mMyEventsFetcherCallBack, mErrorListener).execute();
        } else {
            EventCollectionRequest.submit(this, eventsContext, Priority.IMMEDIATE, this,
                    false, true, mEventsFetcherCallBack, mErrorListener);
        }
    }

    private void updateUserLocation(LatLng userLocation) {
        isAppMovement = true;
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
        ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
                map.setMyLocationEnabled(true);
                map.setOnCameraChangeListener(mOnCameraChangeListener);
                map.setOnMarkerClickListener(mOnMarkerClickListener);
                map.setOnInfoWindowClickListener(mOnInfoWindowClickListener);
                map.setOnMapClickListener(mOnMapClickListener);

                updateUserLocation(eventsContext.location);
                eventsContext.changeLocation(null);
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
                    showEventDetails(mapMarkerManager.getEvent(lastSelectedMarker), null);
                    return true;
                }

                @Override
                public boolean onFling(MotionEvent event1, MotionEvent event2,
                                       float velocityX, float velocityY) {
                    if (Math.abs(velocityY) > Math.abs(velocityX)) {
                        // this is either up or down movement, ignore.
                        reportActionToAnalytics("swipeVertical");
                        return  false;
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
        eventView = EventsAdapter.getEventCard(
                event, EventsMapsActivity.this, eventView, eventCardContainer);
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


    // ***********************
    // Callbacks
    // ***********************

    // This is called when maps camera position is changed (zoom in, zoom out or
    // user dragging the map around). We refresh the events listing if there is
    // change in city otherwise we refresh the event markers shown to user.
    private OnCameraChangeListener mOnCameraChangeListener = new OnCameraChangeListener() {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            if (!isAppMovement && lastSelectedMarker == null) {
                reportActionToAnalytics("onCameraChange");
            }
            boolean isInfoWindowShown = mapMarkerManager.updateListingForProjection(map.getProjection());
            if (!isInfoWindowShown) {
                mOnMapClickListener.onMapClick(null);
            }

            if (!eventsContext.changeLocation(cameraPosition.target)) {
                fetchEvents();
            }
            isAppMovement = false;
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
            showEventDetails(mapMarkerManager.getEvent(marker), null);
        }
    };

    private ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);
            Toast.makeText(EventsMapsActivity.this, R.string.failed_load, Toast.LENGTH_SHORT).show();
        }
    };

    private Listener<EventsCollection> mEventsFetcherCallBack = new Listener<EventsCollection>() {
        @Override
        public void onResponse(EventsCollection eventsCollection, boolean isIntermediate) {
            topProgressBar.setVisibility(isIntermediate ? View.VISIBLE : View.GONE);
            mOnMapClickListener.onMapClick(null);
            mapMarkerManager.setEvents(map, eventsCollection.events);
        }
    };

    private Listener<List<TopicEvents>> mMyEventsFetcherCallBack = new Listener<List<TopicEvents>>() {
        @Override
        public void onResponse(List<TopicEvents> myEvents, boolean isIntermediate) {
            Set<Event> events = new HashSet<>();
            for (TopicEvents topicEvents : myEvents) {
                events.addAll(topicEvents.events);
            }

            mEventsFetcherCallBack.onResponse(new EventsCollection(new ArrayList<>(events), 0),
                    isIntermediate);
        }
    };
}
