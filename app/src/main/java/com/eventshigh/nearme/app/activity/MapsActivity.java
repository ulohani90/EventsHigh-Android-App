package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.utils.MarkerManager;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.SupportMapFragment;
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
public class MapsActivity extends LocationAwareEventActivity {

    // ***********************
    // CONSTANTS
    // ***********************

    // log tag used for debugging.
    private static final String LOG_TAG = MapsActivity.class.getSimpleName();

    // For performance reasons, we show events only where user has reasonable zoom level.
    private static final int MIN_ZOOM_LEVEL = 11;


    // ***********************
    // MEMBERS
    // ***********************

    // Google Map View shows to user using MapFragment.
    private GoogleMap map;
    // Manager for all markers drawn on map. Manager is responsible for hiding/showing markers
    // on map.
    private MarkerManager markerManager = new MarkerManager();
    // Have we shown various helper toast to user. We show them only once application lifetime.
    boolean showZoomToast = true;


    // ***********************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // See if we have location passed to us within intent.
        Intent intent = getIntent();
        EventFetcherParam param = null;
        if (intent != null) {
            param = intent.getParcelableExtra(EXTRA_EVENT_FETCHER_PARAM);
        }

        // Setup the local member variables.
        setUpMapIfNeeded();
        setUpAll(param);

        // Automatic Google Analytics reporting.
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_list) {
            if (lastEventFetcherParam == null) {
                Toast.makeText(this, R.string.no_event, Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, EventListActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .putExtra(EXTRA_EVENT_FETCHER_PARAM, lastEventFetcherParam)
                );
            }
            return true;
        }

        if (item.getItemId() == R.id.action_change_location) {
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(LOG_TAG)
                    .setAction("menu_change_location")
                    .setLabel("")
                    .setValue(1)
                    .build());

            askUserForLocation();
        }
        return super.onOptionsItemSelected(item);
    }


    // ***********************
    // Setup Helper Methods
    // ***********************
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            map = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();

            // Check if we were successful in obtaining the map.
            if (map != null) {
                map.setMyLocationEnabled(true);
                map.setOnCameraChangeListener(mOnCameraChangeListener);
                map.setInfoWindowAdapter(mInfoWindowAdapter);
                map.setOnMarkerClickListener(mOnMarkerClickListener);
                map.setOnInfoWindowClickListener(mOnInfoWindowClickListener);

                MapsInitializer.initialize(this);
            }
        }
    }


    // ***********************
    // Other Helper Methods
    // ***********************

    @Override
    protected void updateNewEvents(List<Event> events) {
        markerManager.setEvents(map, events);
    }

    @Override
    protected void updateUserLocation(LatLng userLocation) {
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder()
                    .target(userLocation)
                    .zoom(Math.max(map.getCameraPosition().zoom, MIN_ZOOM_LEVEL + 1))
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
        private boolean firstCall = true;

        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            // If user has zoomed out too much, do not show events marker.
            // We also show helper toast once per application runtime.
            if (cameraPosition.zoom < MIN_ZOOM_LEVEL) {
                if (!firstCall && showZoomToast) {
                    Toast.makeText(MapsActivity.this, R.string.zoom, Toast.LENGTH_SHORT).show();
                    showZoomToast = false;
                }

                firstCall = false;
                refreshListingsIfNeeded(null);
                return;
            }

            firstCall = false;
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(LOG_TAG)
                    .setAction("onCameraChange")
                    .setLabel("")
                    .setValue(1)
                    .build());

            if (!refreshListingsIfNeeded(cameraPosition.target)) {
                markerManager.updateListingForProjection(map.getProjection());
            }
        }
    };

    private OnMarkerClickListener mOnMarkerClickListener = new OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(LOG_TAG)
                    .setAction("onMarkerClick")
                    .setLabel("")
                    .setValue(1)
                    .build());

            return false;
        }
    };

    // This is called when we need to present the InfoWindow to user for selected marker.
    private InfoWindowAdapter mInfoWindowAdapter = new InfoWindowAdapter() {
        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            Event event = markerManager.getEvent(marker);

            View infoView = getLayoutInflater().inflate(R.layout.event_info_card, null);
            ((TextView)infoView.findViewById(R.id.event_catergory)).setTypeface(font);
            ((TextView)infoView.findViewById(R.id.event_catergory)).setText(event.category.getIconStringId());
            ((TextView)infoView.findViewById(R.id.event_title)).setText(
                    Utils.shortenIfNeeded(event.title));
            ((TextView)infoView.findViewById(R.id.event_time)).setText(
                    Utils.getEventTime(event));
            ((TextView)infoView.findViewById(R.id.num_people_interested)).setText(
                    Integer.toString(event.numPeopleInterested));
            infoView.findViewById(R.id.event_recommended).setVisibility(
                    event.ehRecommended ? View.VISIBLE : View.INVISIBLE);

            return infoView;
        }
    };

    // When user clicks on info window, we open the browser with details URL.
    private OnInfoWindowClickListener mOnInfoWindowClickListener = new OnInfoWindowClickListener() {
        @Override
        public void onInfoWindowClick(Marker marker) {
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(LOG_TAG)
                    .setAction("onInfoWindowClick")
                    .setLabel("")
                    .setValue(1)
                    .build());

            showEventDetails(markerManager.getEvent(marker));
        }
    };
}
