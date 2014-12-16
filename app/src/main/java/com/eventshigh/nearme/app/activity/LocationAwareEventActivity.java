package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.ViewGroup;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.data.EventsCollection;
import com.eventshigh.nearme.app.data.EventsCollection.Builder;
import com.eventshigh.nearme.app.data.EventsFetcher;
import com.eventshigh.nearme.app.data.EventsFetcher.EventsFetcherCallBack;
import com.eventshigh.nearme.app.utils.DaySelector;
import com.eventshigh.nearme.app.utils.DaySelector.DaySelectionListener;
import com.eventshigh.nearme.app.utils.LocationPickerDialog;
import com.eventshigh.nearme.app.utils.LocationPickerDialog.OnLocationSelection;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Base activity for location aware events listing. This class implements
 * common methods to get user location and fetch listings when needed.
 * This is abstract class the event UI is left to implementing class.
 * <p/>
 *
 * The Parent activity must call the {@link #setUpAll()} ()} method in its
 * {@link #onCreate} method after populating view. The parent activity view
 * should have {@link android.widget.LinearLayout} to hold day picker.
 */
public abstract class LocationAwareEventActivity extends BaseActivity {

    // ***********************
    // CONSTANTS
    // ***********************
    public static final String EXTRA_EVENT_FETCHER_PARAM = EventFetcherParam.class.getSimpleName();
    public static final String EXTRA_TAG_NAME_PARAM = "extra.event.tag.name";


    // ***********************
    // MEMBERS
    // ***********************

    // GoogleApiClient used to determine user current location.
    private GoogleApiClient googleApiClient;
    // Day selector widget which is shown to user to
    // select any day from upcoming week.
    private DaySelector daySelector;
    // Last city,day for which events are shown.
    protected EventFetcherParam lastEventFetcherParam;
    // Last fetched events collection.
    private EventsCollection events;
    // Tag selected from tab bar for which events are shown.
    private String lastSelectedTag;


    // ***********************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // ***********************

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup GoogleApiClient to fetch location.
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mOnConnectionFailedListener)
                .build();

        // Setup DaySelector.
        daySelector = new DaySelector(this);
        daySelector.setDaySelectionListener(mDaySelectionListener);
    }

    protected void onStop() {
        super.onStop();

        // Avoid battery drain by turning of location client.
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
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
     * is created, it calls {@link #setUpAll()} ()}
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
    protected void setUpAll() {
        // See if we have location passed to us within intent.
        Intent intent = getIntent();
        EventFetcherParam param = null;
        if (intent != null) {
            param = intent.getParcelableExtra(EXTRA_EVENT_FETCHER_PARAM);
            lastSelectedTag = intent.getStringExtra(EXTRA_TAG_NAME_PARAM);
        }

        // Populate the Day selection bar.
        daySelector.populate((ViewGroup)findViewById(R.id.daySelector));
        if (param != null) {
            daySelector.setSelected(param.day);
        }

        // If location is passed in param, use it. Otherwise ask GoogleApiClient for
        // user location.
        if (param == null) {
            googleApiClient.connect();
        } else {
            updateUserLocation(param.location);
        }
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

    protected void askUserForLocation(@Nullable final ActionBar actionBar) {
        reportActionToAnalytics("askUserForLocation");
        String countryCode =
                lastEventFetcherParam == null || lastEventFetcherParam.city == null
                        ? null : lastEventFetcherParam.city.countryCode;
        new LocationPickerDialog().show(this, countryCode, new OnLocationSelection() {
            @Override
            public void onLocationSelection(String locationString, LatLng locationPoint) {
                if (actionBar != null) {
                    actionBar.setSubtitle(locationString);
                }
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
        detailIntent.putExtra(EventDetailActivity.ARG_EVENT_INFO, event);
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
        City userCity = City.getCity(userLocation);
        if (userCity == null) {
            if (userLocation != null) {
                Toast.makeText(this, R.string.no_event, Toast.LENGTH_SHORT).show();
            }
            lastEventFetcherParam = null;
            basicTabs();
            return true;
        }

        EventFetcherParam newEventFetcherParam =
                new EventFetcherParam(userCity, userLocation, daySelector.getSelectedDay());
        if (!newEventFetcherParam.equals(lastEventFetcherParam)) {
            basicTabs();
            fetchNewListing(newEventFetcherParam);
            return true;
        }

        lastEventFetcherParam = newEventFetcherParam;
        return false;
    }

    protected void switchTo(Class<?> cls) {
        Intent intent = new Intent(this, cls)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (lastEventFetcherParam != null) {
            intent.putExtra(EXTRA_EVENT_FETCHER_PARAM, lastEventFetcherParam);
        }
        if (lastSelectedTag != null) {
            intent.putExtra(EXTRA_TAG_NAME_PARAM, lastSelectedTag);
        }
        startActivity(intent);
    }

    private void basicTabs() {
        events = new Builder().build();

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            Tab selectedTab = actionBar.getSelectedTab();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.removeAllTabs();
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(EventsCollection.ALL_EVENTS_CATEGORY + " (" + 0 + " )")
                            .setTag(EventsCollection.ALL_EVENTS_CATEGORY)
                            .setTabListener(mTabListener));
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(EventsCollection.RECOMMENDED_EVENTS_CATEGORY + " (" + 0 + " )")
                            .setTag(EventsCollection.RECOMMENDED_EVENTS_CATEGORY)
                            .setTabListener(mTabListener));

            if (selectedTab != null) {
                lastSelectedTag = selectedTab.getTag().toString();
            }
        }
    }


    // ***********************
    // Callbacks
    // ***********************

    // Callback for GoogleClientApi. This is called when googleClientApi is
    // ready to accept requests. We move map's camera position to last
    // known location if available or send the request to fetch the user location.
    private ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (location != null) {
                updateUserLocation(Utils.locationToLatLng(location));
                googleApiClient.disconnect();
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
                    LocationServices.FusedLocationApi.requestLocationUpdates(
                            googleApiClient, REQUEST, mLocationListener);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            // do nothing.
        }
    };

    private OnConnectionFailedListener mOnConnectionFailedListener = new OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            reportActionToAnalytics("locationFailed");
            Toast.makeText(LocationAwareEventActivity.this,
                    R.string.failed_location, Toast.LENGTH_SHORT).show();
            askUserForLocation(null);
        }
    };

    // Callback for LocationClient which user location is available. We move the
    // map's camera to user location and disable the future updates for user location
    // change. User can still click on map to move the map's camera to new location.
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateUserLocation(Utils.locationToLatLng(location));
            googleApiClient.disconnect();
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
        public void OnEventsAvailable(EventFetcherParam param, EventsCollection events) {
            lastEventFetcherParam = param;
            LocationAwareEventActivity.this.events = events;

            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.removeAllTabs();
                int selectedItem = 0;
                List<Pair<String, Integer>> tags = events.getTags();
                if (lastSelectedTag != null) {
                    for (int i = 0; i < tags.size(); i++) {
                        if (tags.get(i).first.equalsIgnoreCase(lastSelectedTag)) {
                            selectedItem = i;
                            break;
                        }
                    }
                }

                for (Pair<String, Integer> tag : tags) {
                    actionBar.addTab(
                            actionBar.newTab()
                                    .setText(tag.first + "\n(" + tag.second + ")")
                                    .setTag(tag.first)
                                    .setTabListener(mTabListener), false);
                }

                actionBar.setSelectedNavigationItem(selectedItem);
            }
        }
    };

    private TabListener mTabListener = new TabListener() {
        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (events != null) {
                lastSelectedTag = tab.getTag().toString();
                updateNewEvents(events.getEvents(tab.getPosition()));
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    };
}
