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
import android.support.v4.app.NavUtils;
import android.util.Pair;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.data.EventsCollection;
import com.eventshigh.nearme.app.data.EventsCollection.Builder;
import com.eventshigh.nearme.app.data.EventsFetcher;
import com.eventshigh.nearme.app.data.EventsFetcher.EventsFetcherCallBack;
import com.eventshigh.nearme.app.utils.DaySelector;
import com.eventshigh.nearme.app.utils.DaySelector.DaySelectionListener;
import com.eventshigh.nearme.app.utils.EventSearchSuggestionsProvider;
import com.eventshigh.nearme.app.utils.LocationPickerDialog;
import com.eventshigh.nearme.app.utils.LocationPickerDialog.OnLocationSelection;
import com.eventshigh.nearme.app.utils.OnBoardingHelper;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.Collections;
import java.util.List;

/**
 * Base activity for location aware events listing. This class implements
 * common methods to get user location and fetch listings when needed.
 * This is an abstract activity and the event UI is left to implementing
 * class.
 * <p/>
 *
 * The parent activity view should have {@link android.widget.LinearLayout}
 * to hold day picker.
 */
public abstract class LocationAwareEventActivity extends BaseActivity {

    // ***********************
    // CONSTANTS
    // ***********************
    public static final String EXTRA_EVENT_FETCHER_PARAM = EventFetcherParam.class.getSimpleName();
    public static final String EXTRA_TAG_NAME_PARAM = "extra.event.tag.name";
    public static final int NUM_MAX_TABS = 10;


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
    // last fetcher used to fetch events.
    private EventsFetcher fetcher;
    // On boarding helper.
    private OnBoardingHelper onBoardingHelper;


    // ***********************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // ***********************

    @Override
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

        // Set the context in term of lastEventFetcherParam. Use Inent
        // to restore the context.
        lastEventFetcherParam = new EventFetcherParam(null, 0, "");

        // See if we have context passed to us within intent.
        Intent intent = getIntent();
        EventFetcherParam param = intent.getParcelableExtra(EXTRA_EVENT_FETCHER_PARAM);
        if (param != null) {
            lastEventFetcherParam = param;
        }
        lastSelectedTag = intent.getStringExtra(EXTRA_TAG_NAME_PARAM);

        // Show query as title.
        if (!lastEventFetcherParam.query.isEmpty()) {
            reportActionToAnalytics("search", lastEventFetcherParam.query);
            EventSearchSuggestionsProvider.saveRecentQuery(this, lastEventFetcherParam.query);

            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setTitle(lastEventFetcherParam.query);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Avoid battery drain by turning of location client.
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        // See http://stackoverflow.com/questions/22924825/view-not-attached-to-window-manager-crash.
        if (fetcher != null) {
            fetcher.destroy();
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Populate the Day selection bar.
        if (lastEventFetcherParam.query.isEmpty()) {
            daySelector.populate((ViewGroup) findViewById(R.id.daySelector));
            daySelector.setSelected(lastEventFetcherParam.day);
        }

        // If location is passed in param, use it. Otherwise ask GoogleApiClient for
        // user location.
        if (lastEventFetcherParam.location == null) {
            googleApiClient.connect();
        } else {
            LatLng location = lastEventFetcherParam.location;
            lastEventFetcherParam.changeLocation(null);
            updateUserLocation(location);
        }
    }

    @Override
    public boolean onSearchRequested() {
        reportActionToAnalytics("onSearchRequested");
        Bundle appData = new Bundle();
        appData.putParcelable(EXTRA_EVENT_FETCHER_PARAM, lastEventFetcherParam);
        startSearch(null, false, appData, false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        if (id == R.id.action_list) {
            switchTo(EventGridActivity.class);
            return true;
        }

        if (id == R.id.action_map) {
            switchTo(MapsActivity.class);
            return true;
        }

        if (id == R.id.action_change_location) {
            askUserForLocation(null);
            return true;
        }

        return super.onOptionsItemSelected(item);
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
     * Updates the user location as reported by LocationClient.
     *
     * @param userLocation user location as reported by location client.
     */
    protected abstract void updateUserLocation(LatLng userLocation);


    // ***********************
    // Helper methods
    // ***********************

    private void updateListingAndShowHelpIfNeeded(List<Event> events) {
        updateNewEvents(events);

        if (!events.isEmpty()) {
            if (onBoardingHelper == null) {
                onBoardingHelper = new OnBoardingHelper(this);
            }

            onBoardingHelper.next();
        }
    }

    private void fetchNewListing() {
        reportActionToAnalytics("fetchNewListing");
        fetcher = new EventsFetcher(LocationAwareEventActivity.this, mEventsFetcherCallBack);
        fetcher.execute(lastEventFetcherParam);
    }

    protected void askUserForLocation(@Nullable final ActionBar actionBar) {
        reportActionToAnalytics("askUserForLocation");
        String countryCode = lastEventFetcherParam.city == null ?
                null : lastEventFetcherParam.city.countryCode;
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
                reportActionToAnalytics("unsupportedCity");
                Toast.makeText(this, R.string.no_event, Toast.LENGTH_SHORT).show();
            }
            lastEventFetcherParam.changeLocation(null);
            basicTabs();
            return true;
        }

        if (!lastEventFetcherParam.changeLocation(userLocation)) {
            basicTabs();
            fetchNewListing();
            return true;
        }

        return false;
    }

    protected void switchTo(Class<?> cls) {
        reportActionToAnalytics("switchView");
        Intent intent = new Intent(this, cls)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_EVENT_FETCHER_PARAM, lastEventFetcherParam);
        if (lastSelectedTag != null) {
            intent.putExtra(EXTRA_TAG_NAME_PARAM, lastSelectedTag);
        }
        startActivity(intent);
    }

    private void basicTabs() {
        events = new Builder(Collections.EMPTY_SET).build();

        ActionBar actionBar = getActionBar();
        if (actionBar != null && actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS) {
            String lastSelectedTagSave = lastSelectedTag;
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

            if (lastSelectedTagSave != null) {
                lastSelectedTag = lastSelectedTagSave;
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
            lastEventFetcherParam.day = dayNo;
            fetchNewListing();
        }
    };

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private EventsFetcherCallBack mEventsFetcherCallBack = new EventsFetcherCallBack() {
        @Override
        public void OnEventsAvailable(EventFetcherParam param, EventsCollection events) {
            lastEventFetcherParam = param;
            LocationAwareEventActivity.this.events = events;

            // Update tabs if needed.
            ActionBar actionBar = getActionBar();
            if (actionBar == null) {
                return;
            }

            List<Pair<String, Integer>> tags = events.getTags();
            if (tags.size() > NUM_MAX_TABS) {
                tags = tags.subList(0, NUM_MAX_TABS);
            }

            if (actionBar.getNavigationMode() == ActionBar.NAVIGATION_MODE_TABS || tags.size() > 1) {
                if (!param.query.isEmpty()) {
                    actionBar.setTitle(param.query);
                }

                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
                actionBar.removeAllTabs();

                int selectedItem = 0;
                if (lastSelectedTag != null) {
                    for (int i = 0; i < tags.size(); i++) {
                        if (tags.get(i).first.equalsIgnoreCase(lastSelectedTag)) {
                            selectedItem = i;
                            break;
                        }
                    }
                }

                for (Pair<String, Integer> tag : tags) {
                    Tab tab = actionBar.newTab()
                            .setText("  " + tag.first + "  \n  (" + tag.second + ")  ")
                            .setTag(tag.first)
                            .setTabListener(mTabListener);
                    EventCategory category = Event.getCategoryFromTag(tag.first);
                    if (category != null) {
                        int iconRes = category.getIconResourceId();
                        if (iconRes != R.drawable.icon_other) {
                            tab.setIcon(iconRes);
                        }
                    }
                    actionBar.addTab(tab, false);
                }

                actionBar.setSelectedNavigationItem(selectedItem);
            } else {
                if (!param.query.isEmpty()) {
                    int numEvents = tags.isEmpty() ? 0 : events.getEvents(0).size();
                    actionBar.setTitle(param.query + " (" + numEvents + ")");
                }

                updateListingAndShowHelpIfNeeded(events.getEvents(0));
            }
        }
    };

    private TabListener mTabListener = new TabListener() {
        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (events != null) {
                lastSelectedTag = tab.getTag().toString();
                List<Event> eventsForTag = events.getEvents(tab.getPosition());
                if (!eventsForTag.isEmpty()) {
                    reportActionToAnalytics("filterByCategory");
                }
                updateListingAndShowHelpIfNeeded(eventsForTag);
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
