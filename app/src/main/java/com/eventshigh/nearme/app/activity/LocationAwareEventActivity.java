package com.eventshigh.nearme.app.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.ViewGroup;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.data.EventsFetcher;
import com.eventshigh.nearme.app.data.EventsFetcher.EventsFetcherCallBack;
import com.eventshigh.nearme.app.utils.DaySelector;
import com.eventshigh.nearme.app.utils.DaySelector.DaySelectionListener;
import com.eventshigh.nearme.app.utils.LocationPickerDialog;
import com.eventshigh.nearme.app.utils.LocationPickerDialog.OnLocationSelection;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

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
public abstract class LocationAwareEventActivity extends BaseActivity {

    // ***********************
    // CONSTANTS
    // ***********************

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


    // ***********************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // ***********************

    protected void onStop() {
        super.onStop();

        // Avoid battery drain by turning of location client.
        if (locationClient != null && locationClient.isConnected()) {
            locationClient.disconnect();
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
        }
        if (param == null) {
            locationClient.connect();
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

    // ***********************
    // Helper methods
    // ***********************

    private void fetchNewListing(EventFetcherParam param) {
        reportActionToAnalytics("fetchNewListing");
        EventsFetcher fetcher =
                new EventsFetcher(LocationAwareEventActivity.this, mEventsFetcherCallBack);
        fetcher.execute(param);
    }

    protected void askUserForLocation() {
        reportActionToAnalytics("askUserForLocation");
        new LocationPickerDialog().show(this, new OnLocationSelection() {
            @Override
            public void onLocationSelection(String locationString, LatLng locationPoint) {
                updateUserLocation(locationPoint);
            }
        });
    }

    /**
     * Open events details page.
     * @param event event for which to show details page.
     */
    protected void showEventDetails(Event event) {
        reportActionToAnalytics("showEventDetails");
        Intent detailIntent = new Intent(this, EventDetailActivity.class);
        detailIntent.putExtra(EventDetailFragment.ARG_ITEM_ID, event);
        startActivity(detailIntent);
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

        lastEventFetcherParam = newEventFetcherParam;
        return false;
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
                // Check if location access is enabled or not. If not we ask user for the location.
                LocationManager locationManager =
                        (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Criteria criteria = new Criteria();
                criteria.setHorizontalAccuracy(Criteria.ACCURACY_MEDIUM);
                if (locationManager.getProviders(criteria, true).isEmpty()) {
                    mOnConnectionFailedListener.onConnectionFailed(null);
                } else {
                    LocationRequest REQUEST = LocationRequest.create()
                            .setInterval(5000)          // 5 sec
                            .setFastestInterval(1600)   // 16ms = 60fps, 1600ms = 0.6 fps
                            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    locationClient.requestLocationUpdates(REQUEST, mLocationListener);
                }
            }
        }

        @Override
        public void onDisconnected() {
            // do nothing.
        }
    };

    private OnConnectionFailedListener mOnConnectionFailedListener = new OnConnectionFailedListener() {

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            reportActionToAnalytics("locationFailed");
            Toast.makeText(LocationAwareEventActivity.this,
                    R.string.failed_location, Toast.LENGTH_SHORT).show();
            askUserForLocation();
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
            reportActionToAnalytics("onDaySelection");
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
