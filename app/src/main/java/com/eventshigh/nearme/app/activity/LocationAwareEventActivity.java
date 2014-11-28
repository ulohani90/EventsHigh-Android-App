package com.eventshigh.nearme.app.activity;

import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.utils.DaySelector;
import com.eventshigh.nearme.app.utils.DaySelector.DaySelectionListener;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.data.EventsFetcher;
import com.eventshigh.nearme.app.data.EventsFetcher.EventsFetcherCallBack;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base activity for location aware events listing. This class implements
 * common methods to get user location and fetch listings when needed.
 * This is abstract class the event UI is left to implementing class.
 * <p/>
 *
 * The Parent activity must call the
 * {@link #setUpAll(com.eventshigh.nearme.app.data.EventFetcherParam)} ()}
 * method in its {@link #onCreate} method after populating view. The parent
 * activity view should have {@link android.widget.LinearLayout} to hold day picker.
 */
public abstract class LocationAwareEventActivity extends FragmentActivity {

    // ***********************
    // CONSTANTS
    // ***********************

    // log tag used for debugging.
    public static final String EXTRA_EVENT_FETCHER_PARAM = EventFetcherParam.class.getSimpleName();


    // ***********************
    // MEMBERS
    // ***********************

    // LocationClient used to determine user current city and location.
    private LocationClient locationClient;
    // Day selector widget which is shown to user to select any day from upcoming week.
    private DaySelector daySelector;
    // Last city,day for which events are shown.
    protected EventFetcherParam lastEventFetcherParam;
    // font-awesome font, used for icons.
    protected static Typeface font;
    // Tracker
    protected static Tracker tracker;


    // ***********************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // ***********************

    protected void onStop() {
        super.onStop();

        // Avoid battery drain by turning of location client.
        if (locationClient != null) {
            locationClient.disconnect();
        }

        // Save the cache.
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
    }


    // ***********************
    // Delegated methods
    // ***********************

    /**
     * Sets new events data. This is called when we get new events from
     * {@link com.eventshigh.nearme.app.data.EventsFetcher}.
     *
     * EventFetcher could be asked to fetch events when user changes the city or day.
     *
     * @param events a list of events as returned by {@link com.eventshigh.nearme.app.data.EventsFetcher}
     */
    protected abstract void updateNewEvents(List<Event> events);

    /**
     * Updates the user location as reported by LocationClient. When parent activity
     * is created, it calls {@link #setUpAll(com.eventshigh.nearme.app.data.EventFetcherParam)} ()}
     * method, which sets up the location client to know user location. When the information
     * is available, parent activity is notified about user location.
     *
     * @param userLocation user location as reported by location client.
     */
    protected void updateUserLocation(LatLng userLocation) {
        refreshListingsIfNeeded(userLocation);
    }


    // ***********************
    // Setup Helper Methods
    // ***********************

    /**
     * This method sets up the internal variables and states maintained. The parent
     * activity must call once its view is populated.
     */
    protected void setUpAll(@Nullable EventFetcherParam param) {
        setupFontIfNeeded();
        setUpDaySelectorIfNeeded(param);
        setUpLocationClientIfNeeded(param);
        setUpGoogleAnalyticsIfNeeded();
        setupHttpResponseCache();

        if (param != null) {
            updateUserLocation(param.location);
        }
    }

    private void setUpLocationClientIfNeeded(@Nullable EventFetcherParam param) {
        if (locationClient == null) {
            locationClient = new LocationClient(
                    getApplicationContext(),
                    mConnectionCallbacks,
                    mOnConnectionFailedListener);

            if (param == null) {
                locationClient.connect();
            }
        }
    }

    private void setUpDaySelectorIfNeeded(@Nullable EventFetcherParam param) {
        if (daySelector == null) {
            daySelector = new DaySelector(this, (ViewGroup)findViewById(R.id.daySelector));
            daySelector.setDaySelectionListener(mDaySelectionListener);
            daySelector.populate();
        }

        if (param != null) {
            daySelector.setSelected(param.day);
        }
    }

    private void setupFontIfNeeded() {
        if (font == null) {
            font = Typeface.createFromAsset(getAssets(), "fontawesome-webfont.ttf");
        }
        EventCategory.setIconResources(getLayoutInflater(), font);
    }

    private void setUpGoogleAnalyticsIfNeeded() {
        if (tracker == null) {
            tracker = GoogleAnalytics.getInstance(this).newTracker(R.xml.analytics);
            tracker.enableAdvertisingIdCollection(true);
        }
    }

    private void setupHttpResponseCache() {
        // Setup HttpResponseCache.
        if (HttpResponseCache.getInstalled() == null) {
            try {
                File httpCacheDir = new File(getCacheDir(), "http");
                long httpCacheSize = 10 * 1024 * 1024; // 10 MB
                HttpResponseCache.install(httpCacheDir, httpCacheSize);
            } catch (IOException e) {
                Log.w(LocationAwareEventActivity.class.getSimpleName(),
                        "HTTP response cache installation failed:" + e);
            }
        }
    }

    // ***********************
    // Helper methods
    // ***********************

    private void fetchNewListing(EventFetcherParam param) {
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(getClass().getSimpleName())
                .setAction("fetchNewListing")
                .setLabel("")
                .setValue(1)
                .build());

        EventsFetcher fetcher =
                new EventsFetcher(LocationAwareEventActivity.this, mEventsFetcherCallBack);
        fetcher.execute(param);
    }

    /**
     * Refresh the event listings if user city has changed as per new location.
     * Parent activity can pass {@code NULL} to cleanup any state like {@code lastCity}.
     *
     * @param userLocation location of user.
     * @return true if city was updated as per new location and request for
     * fetching new events was submitted.
     */
    protected boolean refreshListingsIfNeeded(@Nullable LatLng userLocation) {
        if (userLocation == null) {
            lastEventFetcherParam = null;
            updateNewEvents(new ArrayList<Event>());
            return true;
        }

        City userCity = City.getCity(userLocation);
        if (userCity == null) {
            lastEventFetcherParam = null;
            updateNewEvents(new ArrayList<Event>());
            return true;
        }

        EventFetcherParam newEventFetcherParam =
                new EventFetcherParam(userCity, userLocation, daySelector.getSelectedDay());
        if (!newEventFetcherParam.equals(lastEventFetcherParam)) {
            fetchNewListing(newEventFetcherParam);
            return true;
        }

        return false;
    }

    protected Uri getEventUri(Event event) {
        return event.getEventDetailsURI(lastEventFetcherParam.city);
    }


    // ***********************
    // Callbacks
    // ***********************

    // Callback for LocationClient. This is called when locationClient is
    // ready to accept requests. We first move map's camera position to last
    // known location and then send the request to fetch the user location.
    private ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            if (locationClient.getLastLocation() != null) {
                updateUserLocation(Utils.locationToLatLng(locationClient.getLastLocation()));
                locationClient.disconnect();
            } else {
                LocationRequest REQUEST = LocationRequest.create()
                        .setInterval(5000)          // 5 sec
                        .setFastestInterval(1600)   // 16ms = 60fps, 1600ms = 0.6 fps
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                locationClient.requestLocationUpdates(REQUEST, mLocationListener);
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
            updateUserLocation(Utils.locationToLatLng(location));
            locationClient.disconnect();
        }
    };

    // This is called when user changes the day for events are shown.
    // We refresh the maps listing.
    private DaySelectionListener mDaySelectionListener = new DaySelectionListener() {
        @Override
        public void onDaySelection(int dayNo) {
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(getClass().getSimpleName())
                    .setAction("onDaySelection")
                    .setLabel("")
                    .setValue(1)
                    .build());

            if (lastEventFetcherParam != null) {
                fetchNewListing(new EventFetcherParam(lastEventFetcherParam.city,
                        lastEventFetcherParam.location, dayNo));
            }
        }
    };

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private EventsFetcherCallBack mEventsFetcherCallBack = new EventsFetcherCallBack() {
        @Override
        public void OnEventsAvailable(EventFetcherParam param, List<Event> events) {
            lastEventFetcherParam = param;
            updateNewEvents(events);
        }
    };
}
