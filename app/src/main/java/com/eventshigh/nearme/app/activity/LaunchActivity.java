package com.eventshigh.nearme.app.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.Request.Priority;
import com.android.volley.Response.Listener;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.ExploreEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.task.FetchLocalityTask;
import com.eventshigh.nearme.app.ui.CityListAdapter;
import com.eventshigh.nearme.app.ui.CityListAdapter.OnCitySelectionListener;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.eventshigh.nearme.app.ui.LocationPickerDialog;
import com.eventshigh.nearme.app.ui.LocationPickerDialog.OnLocationSelection;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.AlarmUtils;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

/**
 * Application Main or launch activity.
 */
public class LaunchActivity extends BaseContextActivity {
    // Constants
    private static final long REFRESH_EVENTS_INTERVAL = 3600 * 1000L;

    // UI Elements for this activity.
    private ViewSwitcher viewSwitcher;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle drawerToggle;

    // Client to Google api so that we can fetch the user location if
    // its not passed in intent.
    private GoogleApiClient client;

    // GCM registration helper.
    private GcmRegistration gcmRegistration;

    // Adapter used to fill the eventsContainer.
    private EventsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set View.
        setContentView(R.layout.activity_launch);
        viewSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);
        topProgressBar = findViewById(R.id.top_progress_bar);
        retryView = findViewById(R.id.view_retry);

        // Set the swipe refresh settings.
        final SwipeRefreshLayout swipeRefreshLayout =
                (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                reportActionToAnalytics("swipeRefresh");
                swipeRefreshLayout.setRefreshing(false);
                onRetry(null);
            }
        });

        // Set the events adapter.
        RecyclerView eventsContainer = (RecyclerView) findViewById(R.id.event_container);
        eventsContainer.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventsAdapter(this);
        eventsContainer.setAdapter(adapter);

        // Setup the actionbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup the Drawer Layout.
        drawer = (DrawerLayout) findViewById(R.id.nav_drawer);
        drawerToggle = new ActionBarDrawerToggle(this, drawer, R.string.app_name, R.string.title_activity_settings);
        drawer.setDrawerListener(drawerToggle);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        // Set defaults for preferences.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);

        // Read Preferences
        gcmRegistration = GcmRegistration.getInstance(this);

        // Process the incoming intent.
        eventsContext = IntentUtils.processIntent(this, getIntent());
        if (isFinishing()) {
            return;
        }

        // Setup the weekly alarms which are used for notification.
        AlarmUtils.setWeeklyAlarms(this);
    }

    public void onStart() {
        super.onStart();

        if (isFinishing()) {
            return;
        }

        // Register with GCM if needed. GCM is used for notifications messages.
        if (isPlayServicesPresent) {
            gcmRegistration.updateGcmRegistrationIdIfNeeded();
        }
    }

    protected void onResume() {
        super.onResume();

        // We show the onboarding If this is first activity and there was no
        // location/query passed through intent.
        if (eventsContext.location == null && eventsContext.query.isEmpty() &&
            eventsContext.dateFilter.isEmpty()) {
            if (Preferences.getInstance(this).shouldShowOnBoarding()) {
                startActivity(new Intent(this, OnBoardingActivity.class));
                return;
            }

            if (!isTaskRoot()) {
                finish();
                return;
            }
        }

        // Show next screen.
        showVerifyPhoneSnackbar();
        showNextScreen();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_launch, menu);

        // Search View.
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();

        if (id == R.id.action_change_location) {
            askUserForLocation();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(Gravity.START)) {
            drawer.closeDrawer(Gravity.START);
        } else {
            super.onBackPressed();
        }
    }

    protected boolean isDataShown() {
        return adapter.getItemCount() > 0;
    }


    // ***********************
    // Callbacks
    // ***********************

    public void onRetry(View view) {
        if (view != null) {
            reportActionToAnalytics("retry");
        }

        topProgressBar.setVisibility(View.VISIBLE);
        ExploreEventsRequest.submit(this, eventsContext, Priority.IMMEDIATE, false, mListener,
                mErrorListener);
    }

    public void showSearchView(String query) {
        reportActionToAnalytics("showSearchView", query);
        launch(new EventsContext(eventsContext.location, query.toLowerCase()));
    }

    // Callback for GoogleClientApi. This is called when googleClientApi is ready to accept
    // requests. We set the user location if needed and start next activity.
    private ConnectionCallbacks mConnectionCallbacks = new ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(client);
            if (location != null) {
                LatLng latLng = LocationUtils.locationToLatLng(location);
                eventsContext.changeLocation(latLng);
                if (eventsContext.city != null) {
                    gcmRegistration.setLastCity(eventsContext.city, latLng);
                } else {
                    reportActionToAnalytics("unsupportedCity");
                }
            }

            // Start the next activity if possible or ask user for city.
            client.disconnect();
            mOnConnectionFailedListener.onConnectionFailed(null);
        }

        @Override
        public void onConnectionSuspended(int i) {
            // do nothing.
        }
    };

    private OnConnectionFailedListener mOnConnectionFailedListener = new OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            // Set the location from lastCity if needed.
            if (eventsContext.city == null) {
                City lastCity = gcmRegistration.getLastCity();
                if (lastCity != null) {
                    reportActionToAnalytics("usedLastCity");
                    eventsContext.changeLocation(lastCity.cityBounds.getCenter());
                }
            }

            // If we have user location, start next activity.
            if (eventsContext.city != null) {
                showNextScreen();
                return;
            }

            // We do not have user location. Lets populate the City chooser and let user
            // select the city.
            reportActionToAnalytics("locationFailed");
            viewSwitcher.setDisplayedChild(1);
            ListView citySelector = (ListView) findViewById(R.id.city_selector);
            citySelector.setAdapter(new CityListAdapter(LaunchActivity.this, mCitySelectionListener));
            if (connectionResult != null) {
                Toast.makeText(LaunchActivity.this, R.string.failed_location, Toast.LENGTH_SHORT).show();
            }
        }
    };

    private Listener<MyEvents> mListener = new Listener<MyEvents>() {
        @Override
        public void onResponse(MyEvents events, boolean isIntermediate) {
            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);
            }
            retryView.setVisibility(View.GONE);
            lastFetchTimestamp = System.currentTimeMillis();
            adapter.setExploreEvents(events);
        }
    };


    // ***********************
    // Helper methods
    // ***********************

    private long lastFetchTimestamp = 0;
    private void refreshIfOldData() {
        if (lastFetchTimestamp + REFRESH_EVENTS_INTERVAL < System.currentTimeMillis()) {
            onRetry(null);
        }
    }

    private void askUserForLocation() {
        reportActionToAnalytics("askUserForLocation");
        String countryCode = eventsContext.city == null ?
                null : eventsContext.city.countryCode;
        new LocationPickerDialog().show(this, countryCode, new OnLocationSelection() {
            @Override
            public void onLocationSelection(String locationString, LatLng locationPoint) {
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setSubtitle(locationString);
                }
                eventsContext.changeLocation(locationPoint);
                onRetry(null);
            }
        });
    }

    private void showNextScreen() {
        // If we do not have user city, use GoogleLocation api to get user location.
        if (eventsContext.city == null) {
            client = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mOnConnectionFailedListener)
                    .build();
            client.connect();
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null &&
            (actionBar.getSubtitle() == null || actionBar.getSubtitle().length() == 0)) {
            new FetchLocalityTask(this, this).execute(eventsContext.location);
        }

        // If we do not have query, show explore screen.
        if (eventsContext.query.isEmpty() && eventsContext.dateFilter.isEmpty()) {
            refreshIfOldData();
        } else {
            launch(eventsContext);
            finish();
        }
    }

    // Launch the target activity.
    private void launch(EventsContext eventsContext) {
        Intent outIntent = new Intent(this, EventsGridActivity.class);
        outIntent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);

        startActivity(outIntent);
    }

    private final OnCitySelectionListener mCitySelectionListener = new OnCitySelectionListener() {
        @Override
        public void onCitySelection(City city) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setSubtitle(Utils.capitalize(city.name()));
            }
            eventsContext.changeLocation(city.cityBounds.getCenter());
            gcmRegistration.setLastCity(city, null);
            showNextScreen();
        }
    };
}
