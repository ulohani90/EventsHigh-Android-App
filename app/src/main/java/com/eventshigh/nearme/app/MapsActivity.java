package com.eventshigh.nearme.app;

import android.graphics.Point;
import android.graphics.Typeface;
import android.location.Location;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.DaySelector.DaySelectionListener;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.data.EventsFetcher;
import com.eventshigh.nearme.app.data.EventsFetcher.EventsFetcherCallBack;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps activity which shows users events happening in given locality. The events are marked
 * across Map and user can zoom in, zoom out or move around the map to discover more events.
 *
 * In additions to location, an date filter is also provided. The date filter is filled with
 * days from upcoming week and user can select perticular date. By default, today's events are
 * shown.
 */
public class MapsActivity extends FragmentActivity {

    // ***********************
    //      CONSTANTS
    // ***********************

    // log tag used for debugging.
    private static final String LOG_TAG = MapsActivity.class.getSimpleName();

    // TO avoid the map cluttering and to provide sense of relevance, we show few
    // event marker is bigger size (highlighted). This constant controls how
    // many markers are highlighted in a view.
    private static final int NUM_MARKERS_HIGHLIGHTED = 5;

    // To avoid cluttering, we do not show marker for event if it happens to be within
    // small distance from other event. This parameter controls that distance as measured
    // in screen units.
    private static final int MIN_MARKER_DISTANCE_SQ = 4000;

    // For performance reasons, we show events only where user has reasonable zoom level.
    private static final int MIN_ZOOM_LEVEL = 11;


    // ***********************
    //      MEMBERS
    // ***********************

    // Google Map View shows to user using MapFragment.
    private GoogleMap map;
    // LocationClient used to determine user current city and location.
    private LocationClient locationClient;
    // Day selector widget which is shown to user to select any day from upcoming week.
    private DaySelector daySelector;
    // Last city for which events are shown.
    private City shownCity;
    // Selected day from upcoming week for which events are shown.
    // 0: today, 1: tomorrow and so on.
    private int shownDay;
    // Markers currently created on Maps. Each marker represents one event.
    // Note that all markers may not be visible to user and it is controlled
    // through code below.
    private Map<Marker, Event> markers = new HashMap<Marker, Event>();
    // Set of Markers which are in highlighted state.
    private Set<Marker> highlightedMarkers = new HashSet<Marker>(NUM_MARKERS_HIGHLIGHTED);
    // Marker which user clicked last time. We always show this marker no matter
    // what relevance. This is user shown interest.
    private Marker lastClickedMarker;
    // font-awesome font.
    private Typeface font;
    // Have we shown various helper toast to user. We show them only once application lifetime.
    boolean showZoomToast = true;


    // ***********************
    //      View Methods
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpAll();

        // Setup HttpResponseCache.
        try {
            File httpCacheDir = new File(getCacheDir(), "http");
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
                Log.i(LOG_TAG, "HTTP response cache installation failed:" + e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpAll();

        // We need to repopulate daySelector as user might have reopened the
        // app on next day.
        daySelector.populate();
        // Connect the locationclient to know user's location and city.
        locationClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Avoid battery drain by turning of location client.
        if (locationClient != null) {
            locationClient.disconnect();
        }
    }

    protected void onStop() {
        super.onStop();

        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
    }


    // ***********************
    //      Setup Helper Methods
    // ***********************

    private void setUpAll() {
        setUpLocationClientIfNeeded();
        setUpMapIfNeeded();
        setUpDaySelectorIfNeeded();
        setupFontIfNeeded();

        shownCity = null;
    }

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
            }
        }
    }

    private void setUpLocationClientIfNeeded() {
        if (locationClient == null) {
            locationClient = new LocationClient(
                    getApplicationContext(),
                    mConnectionCallbacks,
                    mOnConnectionFailedListener);
        }
    }

    private void setUpDaySelectorIfNeeded() {
        if (daySelector == null) {
            daySelector = new DaySelector(this, (ViewGroup)findViewById(R.id.daySelector));
            daySelector.setDaySelectionListener(mDaySelectionListener);
        }
    }

    private void setupFontIfNeeded() {
        if (font == null) {
            font = Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
        }
    }


    // ***********************
    //      Other Helper Methods
    // ***********************

    private void animateCamera(Location userLocation) {
        map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder()
                                .target(Utils.locationToLatLng(userLocation))
                                .zoom(map.getCameraPosition().zoom)
                                .build()
                )
        );
    }

    private void clear() {
        map.clear();
        markers.clear();
        shownCity = null;
    }

    private boolean refreshListingsIfNeeded(CameraPosition cameraPosition) {
        City userCity = City.getCity(cameraPosition.target);
        if (userCity == null) {
            // We do not support this city.
            clear();
            return true;
        }

        // If user has zoomed out too much, do not show events marker.
        // We also show toast for first time of application runtime.
        if (cameraPosition.zoom < MIN_ZOOM_LEVEL) {
            if (showZoomToast) {
                Toast.makeText(this, R.string.zoom, Toast.LENGTH_SHORT).show();
                showZoomToast = false;
            }
            clear();
            return true;
        }

        // Do nothing, if user has not changed the city and there is no day change.
        if (shownCity != null && shownCity == userCity &&
            shownDay == daySelector.getSelectedDay()) {
            return false;
        }

        clear();
        shownCity = userCity;
        shownDay = daySelector.getSelectedDay();
        EventsFetcher fetcher = new EventsFetcher(MapsActivity.this, mEventsFetcherCallBack);
        fetcher.execute(new EventFetcherParam(shownCity, shownDay));
        return true;
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

                return markers.get(rhs).popularityScore.compareTo(
                        markers.get(lhs).popularityScore);
            }
        });

        // We now show as much point as possible so that no two markers are very close.
        // Few first markers (high popularity score) are highlighted.
        List<Point> shownPoints = new ArrayList<Point>(markersInProjection.size());
        for (Marker marker : markersInProjection) {
            Event event = markers.get(marker);
            Point point = projection.toScreenLocation(event.location);
            boolean toClose = false;
            for (Point shownPoint : shownPoints) {
                if (Utils.getDistanceSQ(shownPoint, point) < MIN_MARKER_DISTANCE_SQ) {
                    toClose = true;
                    break;
                }
            }

            if (toClose) {
                marker.setVisible(false);
                continue;
            }

            if (shownPoints.size() < NUM_MARKERS_HIGHLIGHTED) {
                if (! highlightedMarkers.contains(marker)) {
                    marker.setIcon(event.category.icon(getLayoutInflater(), font, false));
                    highlightedMarkers.add(marker);
                }
            } else {
                if (highlightedMarkers.contains(marker) || marker.getAlpha() > 0.8f) {
                    highlightedMarkers.remove(marker);
                    marker.setIcon(event.category.icon(getLayoutInflater(), font, true));
                    marker.setAlpha(0.7f);
                }
            }

            marker.setVisible(true);
            shownPoints.add(point);
        }

        // Show the info card for highest popular event.
        if (!markersInProjection.isEmpty()) {
            markersInProjection.get(0).showInfoWindow();
        }
    }


    // ***********************
    //    Callbacks
    // ***********************

    // Callback for LocationClient. This is called when locationClient is
    // ready to accept requests. We first move map's camera position to last
    // known location and then send the request to fetch the user location.
    private ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {

        private final LocationRequest REQUEST = LocationRequest.create()
                .setInterval(5000)          // 5 sec
                .setFastestInterval(1600)   // 16ms = 60fps, 1600ms = 0.6 fps
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        @Override
        public void onConnected(Bundle bundle) {
            Log.w(LOG_TAG, "in onConnected");
            locationClient.requestLocationUpdates(REQUEST, mLocationListener);
            if (locationClient.getLastLocation() != null) {
                animateCamera(locationClient.getLastLocation());
            }
        }

        @Override
        public void onDisconnected() {
            // do nothing for now.
        }
    };

    private OnConnectionFailedListener mOnConnectionFailedListener = new OnConnectionFailedListener() {

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            // do nothing for now.
        }
    };

    // Callback for LocationClient which user location is available. We move the
    // map's camera to user location and disable the future updates for user location
    // change. User can still click on map to move the map's camera to new location.
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.v(LOG_TAG, "in onLocationChanged:" + location.toString());
            animateCamera(location);
            locationClient.disconnect();
        }
    };

    // This is called when maps camera position is changed (zoom in, zoom out or
    // user dragging the map around). We refresh the events listing if there is
    // change in city otherwise we refresh the event markers shown to user.
    private OnCameraChangeListener mOnCameraChangeListener = new OnCameraChangeListener() {
        @Override
        public void onCameraChange(CameraPosition cameraPosition) {
            if (!refreshListingsIfNeeded(cameraPosition)) {
                updateListingForProjection();
            }
        }
    };

    private OnMarkerClickListener mOnMarkerClickListener = new OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
            lastClickedMarker = marker;
            return false;
        }
    };

    // This is called when user changes the day for events are shown.
    // We refresh the maps listing.
    private DaySelectionListener mDaySelectionListener = new DaySelectionListener() {
        @Override
        public void onDaySelection(int dayNo) {
            refreshListingsIfNeeded(map.getCameraPosition());
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

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private EventsFetcherCallBack mEventsFetcherCallBack = new EventsFetcherCallBack() {
        @Override
        public void OnEventsAvailable(List<Event> events) {
            for(Event event : events) {
                Marker marker = map.addMarker(
                        new MarkerOptions()
                                .position(new LatLng(event.location.latitude, event.location.longitude))
                                .visible(false)
                );

                markers.put(marker, event);
            }

            updateListingForProjection();
        }
    };
}
