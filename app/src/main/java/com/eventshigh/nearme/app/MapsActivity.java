package com.eventshigh.nearme.app;

import android.graphics.Typeface;
import android.location.Location;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.DaySelector.DaySelectionListener;
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
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.SphericalUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity {

    private static final String LOG_TAG = MapsActivity.class.getSimpleName();
    private static final int NUM_MARKERS_SHOWN = 5;
    private static final int CITY_BOUNDARY_METERS = 40000;

    private GoogleMap map;
    private LocationClient locationClient;
    private DaySelector daySelector;
    private LatLng shownLocation;
    private int shownDay;
    private Map<Marker, Event> markers = new HashMap<Marker, Event>();
    private Marker lastClickedMarker;
    private Typeface font;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpAll();

        // HttpResponseCache
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
        daySelector.populate();
        locationClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
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

    private void setUpAll() {
        setUpLocationClientIfNeeded();
        setUpMapIfNeeded();
        setUpDaySelectorIfNeeded();
        shownLocation = null;

        if (font == null) {
            font = Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
        }
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

    private boolean refreshListingsIfNeeded(CameraPosition cameraPosition) {
        // Do nothing, if last shown location is within same city and there is no day change.
        if (shownLocation != null &&
                shownDay == daySelector.getSelectedDay() &&
                SphericalUtil.computeDistanceBetween(shownLocation, cameraPosition.target) < CITY_BOUNDARY_METERS) {
            return false;
        }

        shownLocation = cameraPosition.target;
        shownDay = daySelector.getSelectedDay();
        map.clear();
        markers.clear();
        EventsFetcher fetcher = new EventsFetcher(MapsActivity.this, mEventsFetcherCallBack);
        fetcher.execute(new EventFetcherParam(shownLocation, shownDay));
        return true;
    }

    private void updateListingForProjection() {
        LatLngBounds bounds = map.getProjection().getVisibleRegion().latLngBounds;
        List<Marker> markersInProjection = new ArrayList<Marker>();
        for (Marker marker : markers.keySet()) {
            if (bounds.contains(marker.getPosition())) {
                markersInProjection.add(marker);
            } else {
                marker.setVisible(false);
            }
        }

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

        for (int i = 0; i < markersInProjection.size(); i++) {
            markersInProjection.get(i).setVisible(i < NUM_MARKERS_SHOWN);
        }

        if (!markersInProjection.isEmpty()) {
            markersInProjection.get(0).showInfoWindow();
        }
    }

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


    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.v(LOG_TAG, "in onLocationChanged:" + location.toString());
            animateCamera(location);
            locationClient.disconnect();
        }
    };

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

    private DaySelectionListener mDaySelectionListener = new DaySelectionListener() {
        @Override
        public void onDaySelection(int dayNo) {
            refreshListingsIfNeeded(map.getCameraPosition());
        }
    };

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

    private EventsFetcherCallBack mEventsFetcherCallBack = new EventsFetcherCallBack() {
        @Override
        public void OnEventsAvailable(List<Event> events) {
            markers.clear();
            for(Event event : events) {
                Marker marker = map.addMarker(
                        new MarkerOptions()
                        .position(new LatLng(event.location.latitude, event.location.longitude))
                        .visible(false)
                        .alpha(0.7f)
                );

                markers.put(marker, event);
            }

            updateListingForProjection();
        }
    };
}
