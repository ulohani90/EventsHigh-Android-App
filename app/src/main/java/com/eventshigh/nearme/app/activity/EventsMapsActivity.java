package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.ui.EventListAdapter;
import com.eventshigh.nearme.app.ui.MarkerManager;
import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.ShowcaseView.Builder;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
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

import java.util.List;

/**
 * Maps activity which shows users events happening in given locality. The events are marked
 * across Map and user can zoom in, zoom out or move around the map to discover more events.
 *
 * In additions to location, an date filter is also provided. The date filter is filled with
 * days from upcoming week and user can select perticular date. By default, today's events are
 * shown.
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
    private MarkerManager markerManager = new MarkerManager();
    // FrameLayout holding the Event Card.
    private FrameLayout eventCardContainer;
    // Last marker for which the event info card is shown.
    private Marker lastSelectedMarker;
    // is the movement in camera position is because of app ?
    private boolean isAppMovement = true;
    // We show the helper toast asking user to zoom in to see events.
    // We show them only once application lifetime.
    private boolean showZoomToast = true;


    // ***********************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the local member variables.
        setUpMapIfNeeded();
        setupGestureDetectorIfNeeded();
        eventCardContainer = (FrameLayout) findViewById(R.id.event_card_container);
    }

    protected boolean showLocationInActionBar() {
        return false;
    }

    @Override
    protected boolean isDefaultView() {
        return pref.isMapsViewDefault();
    }

    @Override
    protected int getDisabledMenuId() {
        return R.id.action_map;
    }

    @Override
    protected int getActivityView() {
        return R.layout.activity_maps;
    }


    // ***********************
    // Setup Helper Methods
    // ***********************

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
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
                    showEventDetails(markerManager.getEvent(lastSelectedMarker));
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
                            markerManager.getPrevMarker(lastSelectedMarker) :
                            markerManager.getNextMarker(lastSelectedMarker);
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

    // ***********************
    // Other Helper Methods
    // ***********************

    @Override
    protected void updateNewEvents(List<Event> events) {
        mOnMapClickListener.onMapClick(null);
        markerManager.setEvents(map, events);
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

                refreshListingsIfNeeded(null);
            } else if (!refreshListingsIfNeeded(cameraPosition.target)) {
                if (!isAppMovement && lastSelectedMarker == null) {
                    reportActionToAnalytics("onCameraChange");
                }
                boolean listingShown = markerManager.updateListingForProjection(
                    isAppMovement ? null : map.getCameraPosition().target,
                    map.getProjection());
                if (!listingShown) {
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
        }
    };

    private OnMarkerClickListener mOnMarkerClickListener = new OnMarkerClickListener() {
        ShowcaseView showcaseView;
        boolean isVisible = false;

        @Override
        public boolean onMarkerClick(Marker marker) {
            reportActionToAnalytics("onMarkerClick");

            lastSelectedMarker = marker;
            View eventView = eventCardContainer.getChildAt(0);
            Event event = markerManager.getEvent(marker);
            eventView = EventListAdapter.getView(event, EventsMapsActivity.this, eventView, eventCardContainer);
            eventView.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return gestureDetector.onTouchEvent(event);
                }
            });
            eventCardContainer.removeAllViews();
            eventCardContainer.addView(eventView);

            if (showcaseView != null) {
                if (isVisible) {
                    showcaseView.hide();
                }
            } else {
                // Show Helper with swipe information.
                showcaseView = new Builder(EventsMapsActivity.this, true)
                    .setTarget(new ViewTarget(eventView))
                    .setContentText(R.string.onboarding_swipe)
                    .setStyle(R.style.ShowcaseTheme)
                    .singleShot(2)
                    .hideOnTouchOutside()
                    .setShowcaseEventListener(new OnShowcaseEventListener() {
                        ImageView swipeImage;

                        @Override
                        public void onShowcaseViewHide(ShowcaseView showcaseView) {
                            reportActionToAnalytics("endOnboardingSwipe");
                            isVisible = false;
                            swipeImage.setVisibility(View.GONE);
                        }

                        @Override
                        public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
                        }

                        @Override
                        public void onShowcaseViewShow(ShowcaseView showcaseView) {
                            reportActionToAnalytics("startOnboardingSwipe");
                            isVisible = true;
                            swipeImage = new ImageView(EventsMapsActivity.this);
                            swipeImage.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                            swipeImage.setImageResource(R.drawable.gestures_flick);
                            swipeImage.setScaleType(ScaleType.CENTER_INSIDE);
                            eventCardContainer.addView(swipeImage);
                        }
                    }).build();
            }

            return false;
        }
    };

    // When user clicks on info window, we open the details screen.
    private OnInfoWindowClickListener mOnInfoWindowClickListener = new OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(Marker marker) {
            showEventDetails(markerManager.getEvent(marker));
        }
    };
}
