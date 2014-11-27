package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.graphics.Point;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventFetcherParam;
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
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // TO avoid the map cluttering and to provide sense of relevance, we show icon
    // markers for event which has interest from minimum number of users.
    private static final int MIN_RELEVANCE_FOR_MARKER = 20;

    // To avoid cluttering, we do not show marker for event if it happens to be within
    // small distance from other event. This parameter controls that distance as measured
    // in screen units.
    private static final int MIN_MARKER_DISTANCE_SQ = 2000;

    // For performance reasons, we show events only where user has reasonable zoom level.
    private static final int MIN_ZOOM_LEVEL = 11;


    // ***********************
    // MEMBERS
    // ***********************

    // Google Map View shows to user using MapFragment.
    private GoogleMap map;
    // Markers currently created on Maps. Each marker represents one event.
    // Note that all markers may not be visible to user and it is controlled
    // through code below.
    private Map<Marker, Event> markers = new HashMap<Marker, Event>();
    // Marker which user clicked last time. We always show this marker no matter
    // what relevance. This is user shown interest.
    private Marker lastClickedMarker;
    // Have we shown various helper toast to user. We show them only once application lifetime.
    boolean showZoomToast = true;
    boolean showInfoWindow = true;


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

        // Setup HttpResponseCache.
        try {
            File httpCacheDir = new File(getCacheDir(), "http");
            long httpCacheSize = 10 * 1024 * 1024; // 10 MB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.w(LOG_TAG, "HTTP response cache installation failed:" + e);
        }

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
        getMenuInflater().inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_list) {
            if (lastEventFetcherParam == null) {
                Toast.makeText(this, R.string.no_event, Toast.LENGTH_SHORT).show();
            } else {
                EventFetcherParam param = new EventFetcherParam(
                        lastEventFetcherParam.city,
                        map.getCameraPosition().target,
                        lastEventFetcherParam.day);
                Log.w(LOG_TAG, "Passing: " + lastEventFetcherParam);
                startActivity(new Intent(this, ListActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                .putExtra(EXTRA_EVENT_FETCHER_PARAM, param)
                );
            }
            return true;
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

    private void clear() {
        map.clear();
        markers.clear();
    }

    @Override
    protected void updateNewEvents(List<Event> events) {
        clear();

        for(Event event : events) {
            Marker marker = map.addMarker(
                    new MarkerOptions()
                            .position(new LatLng(event.location.latitude, event.location.longitude))
                            .visible(false)
                            .icon(event.getPopularityScore() < MIN_RELEVANCE_FOR_MARKER ?
                                EventCategory.circleIcon():
                                event.category.icon(getLayoutInflater(), font))
            );

            markers.put(marker, event);
        }

        updateListingForProjection();
    }

    @Override
    protected void updateUserLocation(LatLng userLocation) {
            map.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                            CameraPosition.builder()
                                    .target(userLocation)
                                    .zoom(map.getCameraPosition().zoom)
                                    .build()
                    )
            );
    }

    // Updates the listing for current maps projection. This method decides which markers should
    // be visible and which one should not be visible. Also few markers are highlighted to
    // give relevance information.
    private void updateListingForProjection() {
        // First find the markers which are withing visible region bound. All other
        // markers are marked invisible.
        Projection projection = map.getProjection();
        LatLngBounds bounds = projection.getVisibleRegion().latLngBounds;
        List<Marker> markersInProjection = new ArrayList<Marker>();
        for (Marker marker : markers.keySet()) {
            if (bounds.contains(marker.getPosition())) {
                markersInProjection.add(marker);
            } else {
                marker.setVisible(false);
            }
        }

        // We now sort the markers within visible region based on the relevance or
        // popularity score.
        Collections.sort(markersInProjection, new Comparator<Marker>() {
            @Override
            public int compare(Marker lhs, Marker rhs) {
                if (lhs.equals(lastClickedMarker)) {
                    return -1;
                }
                if (rhs.equals(lastClickedMarker)) {
                    return 1;
                }

                return Integer.valueOf(markers.get(rhs).getPopularityScore()).compareTo(
                        markers.get(lhs).getPopularityScore());
            }
        });

        // We now show as much point as possible so that no two markers are very close.
        // Few first markers (high popularity score) are highlighted.
        List<Point> shownPoints = new ArrayList<Point>(markersInProjection.size());
        for (Marker marker : markersInProjection) {
            Event event = markers.get(marker);
            Point point = projection.toScreenLocation(event.location);

            // Is this marker too close to other marker? if yes then we do not show it.
            boolean toClose = false;
            for (Point shownPoint : shownPoints) {
                if (Utils.getDistanceSQ(shownPoint, point) < MIN_MARKER_DISTANCE_SQ) {
                    toClose = true;
                    break;
                }
            }

            marker.setVisible(!toClose);
            if (!toClose) {
                shownPoints.add(point);
            }
        }

        // Show the info card for highest popular event.
        if (showInfoWindow && !markersInProjection.isEmpty()) {
            markersInProjection.get(0).showInfoWindow();
            showInfoWindow = false;
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
            // If user has zoomed out too much, do not show events marker.
            // We also show helper toast once per application runtime.
            if (cameraPosition.zoom < MIN_ZOOM_LEVEL) {
                if (showZoomToast) {
                    Toast.makeText(MapsActivity.this, R.string.zoom, Toast.LENGTH_SHORT).show();
                    showZoomToast = false;
                }

                refreshListingsIfNeeded(null);
                return;
            }

            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(LOG_TAG)
                    .setAction("onCameraChange")
                    .setLabel("")
                    .setValue(1)
                    .build());

            if (!refreshListingsIfNeeded(cameraPosition.target)) {
                updateListingForProjection();
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

            lastClickedMarker = marker;
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
            Event event = markers.get(marker);

            View infoView = getLayoutInflater().inflate(R.layout.event_info_card, null);
            ((TextView)infoView.findViewById(R.id.event_catergory)).setTypeface(font);
            ((TextView)infoView.findViewById(R.id.event_catergory)).setText(event.category.getIconStringId());
            ((TextView)infoView.findViewById(R.id.event_title)).setText(
                    Utils.shortenIfNeeded(event.title));
            ((TextView)infoView.findViewById(R.id.event_time)).setText(
                    Utils.getEventTime(event));
            ((TextView)infoView.findViewById(R.id.num_people_interested)).setText(
                    Integer.toString(event.numPeopleInterested));

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

            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    getEventUri(markers.get(marker)));
            startActivity(browserIntent);
        }
    };
}
