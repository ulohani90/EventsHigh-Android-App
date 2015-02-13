package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.Editor;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.data.EventsMarkerManager.OnEventMarkChangeListener;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.eventshigh.nearme.app.ui.MapMarkerManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.List;

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
    // We show the helper toast asking user to zoom in to see events.
    // We show them only once application lifetime.
    private boolean showZoomToast = true;
    // Editor to modify the event marks when user favourite or dismiss an event.
    private Editor eventsMarkerEditor;

    // ***********************
    // Delegated Methods from {@link BaseEventsActivity}
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the UI.
        View pager = findViewById(R.id.pager);
        LinearLayout parent = (LinearLayout) pager.getParent();
        View mapsView = getLayoutInflater().inflate(R.layout.view_maps, parent, false);
        pager.setVisibility(View.GONE);
        parent.addView(mapsView, pager.getLayoutParams());
        setUpMap();
        setupGestureDetectorIfNeeded();

        eventCardContainer = (FrameLayout) findViewById(R.id.event_card_container);
        fab.setImageResource(R.drawable.ic_list_white_36dp);
    }

    @Override
    protected void onResume() {
        super.onResume();

        eventsMarkerEditor = EventsMarkerManager.getInstance(this).getEditor();
        mapMarkerManager.removeDismissedEvents(eventsMarkerEditor.getEventsMarkerManager());
        eventsMarkerEditor.getEventsMarkerManager()
                .addOnEventMarkChangeListener(mOnEventMarkChangeListener);

        if (lastSelectedMarker != null) {
            if (lastSelectedMarker.isVisible()) {
                showEventCard();
            } else {
                mOnMapClickListener.onMapClick(null);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        eventsMarkerEditor.close();
        eventsMarkerEditor.getEventsMarkerManager()
                .removeOnEventMarkChangeListener(mOnEventMarkChangeListener);
    }


    // ***********************
    // Delegated Methods from {@link BaseEventsActivity}
    // ***********************

    @Override
    protected boolean showLocationInActionBar() {
        return false;
    }

    @Override
    protected boolean isDefaultView() {
        return pref.isMapsViewDefault();
    }

    @Override
    protected Fragment getNewFragment() {
        return new Fragment();
    }

    @Override
    protected void updateEventListing(List<Event> events, boolean isFavouriteView) {
        mOnMapClickListener.onMapClick(null);
        if (isFavouriteView) {
            events = eventsMarkerEditor.getEventsMarkerManager().filterFavouriteEvents(events);
        }
        mapMarkerManager.setEvents(map, EventsMarkerManager.getInstance(this), events);
        super.updateEventListing(events, isFavouriteView);
    }

    @Override
    protected void updateUserLocation(LatLng userLocation) {
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


    // ***********************
    // Setup Helper Methods
    // ***********************

    private void setUpMap() {
        // Try to obtain the map from the SupportMapFragment.
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        // Check if we were successful in obtaining the map.
        if (map != null) {
            map.setMyLocationEnabled(true);
            map.setOnCameraChangeListener(mOnCameraChangeListener);
            map.setOnMarkerClickListener(mOnMarkerClickListener);
            map.setOnInfoWindowClickListener(mOnInfoWindowClickListener);
            map.setOnMapClickListener(mOnMapClickListener);

            MapsInitializer.initialize(this);
        }
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
                    showEventDetails(mapMarkerManager.getEvent(lastSelectedMarker), 0);
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

    /**
     * Refresh the event listings if user city has changed as per new location.
     * Parent activity can pass {@code NULL} to cleanup any state like {@code lastCity}.
     *
     * @param userLocation location of user.
     * @return true if city was updated as per new location and request for
     * fetching new events was submitted.
     */
    private boolean refreshListingsIfNeeded(LatLng userLocation) {
        if (!eventFetcherParam.changeLocation(userLocation)) {
            super.updateUserLocation(userLocation);
            return true;
        }

        return false;
    }

    private void showEventCard() {
        View eventView = eventCardContainer.getChildAt(0);
        Event event = mapMarkerManager.getEvent(lastSelectedMarker);
        eventView = EventsAdapter.getView(
                event, EventsMapsActivity.this, eventView, eventCardContainer, eventsMarkerEditor);
        eventView.setOnTouchListener(
                new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        return gestureDetector.onTouchEvent(event);
                    }
                });
        eventView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                v.removeOnLayoutChangeListener(this);
                fab.animate().translationY(-v.getHeight()).setDuration(300).start();
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
            // If user has zoomed out too much, do not show events marker.
            // We also show helper toast once per application runtime.
            if (cameraPosition.zoom < MIN_ZOOM_LEVEL) {
                if (!isAppMovement && showZoomToast) {
                    Toast.makeText(EventsMapsActivity.this, R.string.zoom, Toast.LENGTH_SHORT).show();
                    showZoomToast = false;
                }

                updateEventListing(new ArrayList<Event>(), false);
            } else if (!refreshListingsIfNeeded(cameraPosition.target)) {
                if (!isAppMovement && lastSelectedMarker == null) {
                    reportActionToAnalytics("onCameraChange");
                }
                boolean isInfoWindowShown = mapMarkerManager.updateListingForProjection(map.getProjection());
                if (!isInfoWindowShown) {
                    mOnMapClickListener.onMapClick(null);
                }
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
            fab.animate().translationY(0).setDuration(300).start();
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
            showEventDetails(mapMarkerManager.getEvent(marker), 0);
        }
    };

    private OnEventMarkChangeListener mOnEventMarkChangeListener = new OnEventMarkChangeListener() {
        @Override
        public void onEventStateChange(String eventId, @Nullable EventMark eventMark) {
            if (EventMark.isDismissed(eventMark)) {
                if (lastSelectedMarker != null &&
                        mapMarkerManager.getEvent(lastSelectedMarker).id.equals(eventId)) {
                    mOnMapClickListener.onMapClick(null);
                }

                mapMarkerManager.removeEvent(eventId);
            }
        }
    };

    // Called when fab icon is pressed
    public void onSwitchView(View view) {
        switchTo(EventsGridActivity.class);
    }
}
