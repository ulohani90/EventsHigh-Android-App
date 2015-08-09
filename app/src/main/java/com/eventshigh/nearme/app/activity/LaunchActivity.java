package com.eventshigh.nearme.app.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.ui.CityListAdapter;
import com.eventshigh.nearme.app.ui.CityListAdapter.OnCitySelectionListener;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.AlarmUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.plus.PlusOneButton;
import com.google.android.gms.plus.PlusOneButton.OnPlusOneClickListener;

/**
 * Application Main or launch activity.
 */
public class LaunchActivity extends BaseContextActivity {
    // Constants
    public static final String DEFAULT_TAB_PARAM = LaunchActivity.class.getName() + "_default_tab";

    // UI Elements for this activity.
    private DrawerLayout drawer;
    private TabLayout tabsView;
    private ViewPager viewPager;
    private ListView citySelector;
    private ActionBarDrawerToggle drawerToggle;

    // Client to Google api so that we can fetch the user location if
    // its not passed in intent.
    private GoogleApiClient client;

    // GCM registration helper.
    private GcmRegistration gcmRegistration;

    // Tabs.
    private int defaultTab = 1;
    public static final String MY_EVENTS_TAB = EventsHighEndpoints.QUERY_MY_EVENT;
    public static final String EXPLORE_TAB = "explore";
    public static final String NOTIFICATIONS_TAB = "Notifications";
    public static final String THIS_WEEK_TAB = "this week";
    public final String[] TABS = {
            MY_EVENTS_TAB,
            EXPLORE_TAB,
            THIS_WEEK_TAB,
            NOTIFICATIONS_TAB,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set View.
        setContentView(R.layout.activity_launch);
        drawer = (DrawerLayout) findViewById(R.id.nav_drawer);
        tabsView = (TabLayout) findViewById(R.id.tabs);
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        citySelector = (ListView) findViewById(R.id.city_selector);

        // Setup the actionbar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup the Drawer Layout.
        drawerToggle = new ActionBarDrawerToggle(this, drawer, R.string.app_name, R.string.title_activity_settings);
        drawer.setDrawerListener(drawerToggle);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        // Set defaults for preferences.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);

        // Read Preferences
        gcmRegistration = GcmRegistration.getInstance(this);

        // Process the incoming intent.
        eventsContext = IntentUtils.processIntent(this, getIntent());
        String tabName= getIntent().getStringExtra(DEFAULT_TAB_PARAM);
        if (tabName != null) {
            for (int i = 0 ; i < TABS.length; i++) {
                if (TABS[i].equalsIgnoreCase(tabName)) {
                    defaultTab = i;
                    break;
                }
            }
        }
        if (isFinishing()) {
            return;
        }

        // Setup the weekly alarms which are used for notification.
        AlarmUtils.setMyEventsAlarm(this);
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

            String action = getIntent().getAction();
            if (!isTaskRoot() && (action == null || !action.startsWith(NOTIFICATION_ACTION))) {
                finish();
                return;
            }
        }

        // Show next screen.
        showNextScreen();

        // Setup the Google+ Button.
        PlusOneButton plusOneButton = (PlusOneButton) findViewById(R.id.plus_one_button);
        plusOneButton.initialize("https://play.google.com/store/apps/details?id=" + getPackageName(),
                PLUS_ONE_REQUEST_CODE);
        plusOneButton.setOnPlusOneClickListener(new OnPlusOneClickListener() {
            @Override
            public void onPlusOneClick(Intent intent) {
                reportActionToAnalytics("plusOne");
                startActivityForResult(intent, PLUS_ONE_REQUEST_CODE);
            }
        });
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

        // Set visibility.
        menu.findItem(R.id.action_show_map).setVisible(isPlayServicesPresent);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void cityChanged(City city) {
        drawer.closeDrawer(GravityCompat.START);
        reportActionToAnalytics("cityChanged");

        eventsContext.changeLocation(city.cityBounds.getCenter());
        showExploreScreen();
    }


    // ***********************
    // Callbacks
    // ***********************

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
            tabsView.setVisibility(View.GONE);
            viewPager.setVisibility(View.GONE);
            citySelector.setVisibility(View.VISIBLE);
            citySelector.setAdapter(new CityListAdapter(LaunchActivity.this, mCitySelectionListener));
            if (connectionResult != null) {
                showMessage(R.string.failed_location);
            }
        }
    };


    // ***********************
    // Helper methods
    // ***********************

    private void refreshIfOldData() {
        City userCity = gcmRegistration.getLastCity();
        if (eventsContext.city != null && userCity != null &&
                eventsContext.city != userCity) {
            cityChanged(userCity);
            return;
        }

        if (viewPager.getAdapter() == null) {
            showExploreScreen();
        }
    }

    private void showExploreScreen() {
        ExploreScreenPagerAdapter adapter = new ExploreScreenPagerAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(defaultTab, false);

        tabsView.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabsView.setupWithViewPager(viewPager);
        tabsView.setScrollPosition(defaultTab, 0, true);
        tabsView.setOnTabSelectedListener(adapter);
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

        // If we do not have query, show explore screen.
        if (eventsContext.query.isEmpty() && eventsContext.dateFilter.isEmpty()) {
            refreshIfOldData();
        } else {
            Intent outIntent = new Intent(this, EventsGridActivity.class);
            outIntent.setAction(Intent.ACTION_VIEW);
            outIntent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);
            startActivity(outIntent);
            finish();
        }
    }

    private final OnCitySelectionListener mCitySelectionListener = new OnCitySelectionListener() {
        @Override
        public void onCitySelection(City city) {
            eventsContext.changeLocation(city.cityBounds.getCenter());
            gcmRegistration.setLastCity(city, null);

            citySelector.setVisibility(View.GONE);
            tabsView.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.VISIBLE);
            showNextScreen();
        }
    };

    /**
     * An SlidingTabPagerAdapter which populates tabs and content for LaunchActivity.
     */
    private class ExploreScreenPagerAdapter extends FragmentPagerAdapter
            implements TabLayout.OnTabSelectedListener {
        private EventsFragment myEventsFragment;

        public ExploreScreenPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            if (TABS[position].equals(MY_EVENTS_TAB)) {
                EventsContext myEventsContext = new EventsContext(eventsContext.location,
                    EventsHighEndpoints.QUERY_MY_EVENT);
                myEventsFragment = EventsFragment.getInstance(myEventsContext, false, true);
                return myEventsFragment;
            }

            if (TABS[position].equals(EXPLORE_TAB)) {
                return ExploreFragment.getInstance(eventsContext);
            }

            if (TABS[position].equals(NOTIFICATIONS_TAB)) {
                return new StreamFragment();
            }

            return ThisWeekFragment.getInstance(eventsContext, true, 7);
        }

        @Override
        public int getCount() {
            return TABS.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TABS[position];
        }

        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            showActionBar();

            int position = tab.getPosition();
            if (TABS[position].equals(MY_EVENTS_TAB) && myEventsFragment != null) {
                myEventsFragment.onResume();
            }

            viewPager.setCurrentItem(position);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            // do nothing.
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            // do nothing.
        }
    }
}
