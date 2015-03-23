package com.eventshigh.nearme.app.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest;
import com.eventshigh.nearme.app.task.ShowLocalityTask;
import com.eventshigh.nearme.app.ui.CityListAdapter;
import com.eventshigh.nearme.app.ui.CityListAdapter.OnCitySelectionListener;
import com.eventshigh.nearme.app.ui.FailedRetryAdapter;
import com.eventshigh.nearme.app.ui.FeaturedEventsAdapter;
import com.eventshigh.nearme.app.ui.LoadingAdapter;
import com.eventshigh.nearme.app.ui.LocationPickerDialog;
import com.eventshigh.nearme.app.ui.LocationPickerDialog.OnLocationSelection;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.AlarmUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;
import java.util.List;

/**
 * A placeholder {@link android.app.Activity} which is responsible for launching
 * either {@link EventsMapsActivity} or {@link EventsGridActivity} based on user preference.
 *
 * For now, this activity sets the preference 50%-50% for first time and then use this
 * preference in future.
 */
public class LaunchActivity extends BaseActivity {
    // Constants
    private static final int EXPLORE_CARD_WIDTH_DP = 160;
    private static final int MIN_EXPLORE_CARD_IN_ROW = 2;
    private static final long REFRESH_FEATURED_EVENTS_INTERVAL = 3600 * 1000L;
    private static final int MARGIN_DP = android.os.Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP ? 10 : 2;
    public static final String[] EXPLORE_TAGS = {
            IntentUtils.QUERY_ALL,
            EventCategory.MUSIC.categoryName,
            EventCategory.PARTIES.categoryName,
            EventCategory.THEATRE.categoryName,
            EventCategory.KIDS.categoryName,
            EventCategory.TECH.categoryName,
            EventCategory.SPORTS.categoryName,
            EventCategory.HEALTH_WELLNESS.categoryName,
            EventCategory.DANCE.categoryName,
            EventCategory.ART.categoryName,
            EventCategory.FOOD.categoryName,
            EventCategory.LITERATURE.categoryName
    };


    // UI Elements for this activity.
    private ViewSwitcher viewSwitcher;
    private LinearLayout dotsView;
    private ViewPager featuredEventsPager;
    private DrawerLayout drawer;
    private ActionBarDrawerToggle drawerToggle;

    // Client to Google api so that we can fetch the user location if
    // its not passed in intent.
    private GoogleApiClient client;

    // GCM registration helper.
    private GcmRegistration gcmRegistration;

    // Context for next activity.
    private EventsContext eventsContext;
    private EventsContext lastEventsContext;

    // User preferences.
    protected Preferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set View.
        setContentView(R.layout.activity_launch);
        viewSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);
        dotsView = (LinearLayout) findViewById(R.id.dots_parent);
        featuredEventsPager = (ViewPager) findViewById(R.id.featured_events_pager);

        // Set the My Events.
        View myEventsHeader = findViewById(R.id.my_events);
        myEventsHeader.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reportActionToAnalytics("myEvents");
                eventsContext.query = EventsHighEndpoints.QUERY_MY_EVENT;
                showNextScreen(true);
            }
        });
        ((ImageView) myEventsHeader.findViewById(R.id.header_bg)).setImageResource(
                R.drawable.infograph_myeventsbar);
        ((TextView)myEventsHeader.findViewById(R.id.header)).setText(
                Utils.capitalize(EventsHighEndpoints.QUERY_MY_EVENT));

        // Setup the actionbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup the Drawer Layout.
        drawer = (DrawerLayout) findViewById(R.id.nav_drawer);
        drawerToggle = new ActionBarDrawerToggle(this, drawer, R.string.app_name, R.string.title_activity_settings);
        drawer.setDrawerListener(drawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Set defaults for preferences.
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        PreferenceManager.setDefaultValues(this, R.xml.pref_notification, false);

        // Read Preferences
        pref = Preferences.getInstance(this);
        gcmRegistration = GcmRegistration.getInstance(this);

        // Process the incoming intent.
        eventsContext = IntentUtils.processIntent(this, getIntent());

        // Setup the weekly alarms which are used for notification.
        AlarmUtils.setWeeklyAlarms(this);
    }

    public void onStart() {
        super.onStart();

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
            if (pref.shouldShowOnBoarding()) {
                startActivity(new Intent(this, OnBoardingActivity.class));
                return;
            }

            if (showOffer()) {
                return;
            }

            if (!isTaskRoot()) {
                finish();
                return;
            }
        }

        // Redraw the featured events module so that if event was marked as favourite
        // it will be reflected.
        PagerAdapter adapter = featuredEventsPager.getAdapter();
        if (adapter != null) {
            int currentItem = featuredEventsPager.getCurrentItem();
            featuredEventsPager.setAdapter(adapter);
            featuredEventsPager.setCurrentItem(currentItem);
        }

        // Show next screen.
        showVerifyPhoneSnackbar();
        showNextScreen(false);
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


    // ***********************
    // Callbacks
    // ***********************

    public void onRetry(View view) {
        if (view != null) {
            reportActionToAnalytics("retry");
        }
        dotsView.removeAllViews();
        if (featuredEventsPager.getAdapter() == null ||
            featuredEventsPager.getAdapter() instanceof FailedRetryAdapter) {
            featuredEventsPager.setAdapter(new LoadingAdapter(this));
        }

        FeaturedEventsRequest.submit(this, eventsContext, Priority.IMMEDIATE, false, mFeaturedEventsListener,
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Toast.makeText(LaunchActivity.this, R.string.failed_load, Toast.LENGTH_SHORT).show();
                        featuredEventsPager.setAdapter(new FailedRetryAdapter(LaunchActivity.this));
                    }
                });
    }

    public void showToday(View view) {
        reportActionToAnalytics("showToday");
        eventsContext.setDateFilter(Calendar.getInstance());
        showNextScreen(true);
    }

    public void showTomorrow(View view) {
        reportActionToAnalytics("showTomorrow");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        eventsContext.setDateFilter(calendar);
        showNextScreen(true);
    }

    public void showThisWeekend(View view) {
        reportActionToAnalytics("showThisWeekend");
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        if (dayOfWeek > Calendar.SUNDAY && dayOfWeek < Calendar.FRIDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, Calendar.FRIDAY - dayOfWeek);
        }
        eventsContext.setDateFilter(calendar);
        showNextScreen(true);
    }


    public void showSearchView(String query) {
        eventsContext.query = query;
        showNextScreen(true);
    }

    // Callback for GoogleClientApi. This is called when googleClientApi is
    // ready to accept requests. We set the user location if needed and start
    // next activity.
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
                showNextScreen(false);
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


    // ***********************
    // Helper methods
    // ***********************

    private boolean showOffer() {
        String offerURI = pref.getOfferURI();
        if (offerURI == null) {
            return false;
        }

        reportActionToAnalytics("openOffer");
        IntentUtils.processContestViewIntent(this, Uri.parse(offerURI), null);
        return true;
    }

    private void askUserForLocation() {
        reportActionToAnalytics("askUserForLocation");
        String countryCode = eventsContext.city == null ?
                null : eventsContext.city.countryCode;
        new LocationPickerDialog().show(this, countryCode, new OnLocationSelection() {
            @Override
            public void onLocationSelection(String locationString, LatLng locationPoint) {
                getSupportActionBar().setSubtitle(locationString);
                eventsContext.changeLocation(locationPoint);
                lastEventsContext.changeLocation(locationPoint);
                featuredEventsPager.setAdapter(new LoadingAdapter(LaunchActivity.this));
                onRetry(null);
            }
        });
    }

    private void showNextScreen(boolean isUserAction) {
        // If we do not have user city, use GoogleLocation api to get user location.
        if (eventsContext.city == null) {
            lastEventsContext = new EventsContext(eventsContext);
            client = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(mConnectionCallbacks)
                    .addOnConnectionFailedListener(mOnConnectionFailedListener)
                    .build();
            client.connect();
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar.getSubtitle() == null || actionBar.getSubtitle().length() == 0) {
            new ShowLocalityTask(this, actionBar).execute(eventsContext.location);
        }

        // If we do not have query, show explore screen.
        if (!isUserAction && eventsContext.query.isEmpty() && eventsContext.dateFilter.isEmpty()) {
            if (showOffer()) {
                return;
            }

            lastEventsContext = new EventsContext(eventsContext);
            showExploreScreen();
            return;
        }

        // Launch the target activity.
        Class target = isPlayServicesPresent && pref.isMapsViewDefault() ?
                EventsMapsActivity.class : EventsGridActivity.class;
        Intent outIntent = new Intent(this, target);
        outIntent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);
        if (lastEventsContext != null) {
            eventsContext = lastEventsContext;
        }

        startActivity(outIntent);
        if (!isUserAction) {
            finish();
        }
    }

    private long exploreScreenPopulatedTimestamp = 0;
    private void showExploreScreen() {
        viewSwitcher.setDisplayedChild(0);

        if (exploreScreenPopulatedTimestamp == 0) {
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();

            FrameLayout.LayoutParams param = (FrameLayout.LayoutParams) featuredEventsPager.getLayoutParams();
            param.height = Math.min(displayMetrics.heightPixels,
                    Utils.dpToPx(this, 100) + 9 * displayMetrics.widthPixels / 16);
            featuredEventsPager.setLayoutParams(param);

            int spacing = Utils.dpToPx(this, MARGIN_DP);
            int widthPixels = displayMetrics.widthPixels;
            int numColumns = Math.max(MIN_EXPLORE_CARD_IN_ROW,
                    (widthPixels - spacing * 2) / Utils.dpToPx(this, EXPLORE_CARD_WIDTH_DP));

            int size = (widthPixels - spacing * (numColumns + 1)) / numColumns;
            LayoutParams exploreCardLP = new LayoutParams(size, size);
            exploreCardLP.setMargins(0, spacing, spacing, 0);

            LayoutParams rowLP = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            rowLP.setMargins(spacing, 0, 0, 0);

            LinearLayout exploreLayout = (LinearLayout) findViewById(R.id.explore_layout);
            LinearLayout last = new LinearLayout(this);
            for (int i = 0; i < EXPLORE_TAGS.length; i++) {
                if (i % numColumns == 0) {
                    last = new LinearLayout(this);
                    last.setLayoutParams(rowLP);
                    exploreLayout.addView(last);
                }

                last.addView(getExploreCard(EXPLORE_TAGS[i], exploreCardLP, last));
            }
        }

        // Submit the request to populate Featured Events.
        if (exploreScreenPopulatedTimestamp + REFRESH_FEATURED_EVENTS_INTERVAL <
                System.currentTimeMillis()) {
            onRetry(null);
        }
        exploreScreenPopulatedTimestamp = System.currentTimeMillis();
    }

    private final OnCitySelectionListener mCitySelectionListener = new OnCitySelectionListener() {
        @Override
        public void onCitySelection(City city) {
            getSupportActionBar().setSubtitle(Utils.capitalize(city.name()));
            eventsContext.changeLocation(city.cityBounds.getCenter());
            gcmRegistration.setLastCity(city, null);
            showNextScreen(false);
        }
    };

    private View getExploreCard(final String tagName, LayoutParams lp, ViewGroup parent) {
        final View view = getLayoutInflater().inflate(R.layout.explore_card, parent, false);
        view.setLayoutParams(lp);
        ((ImageView) view.findViewById(R.id.explore_image)).setImageResource(
                getInfoGraphId(tagName));

        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                reportActionToAnalytics("exploreCategory", tagName);
                showSearchView(tagName);
            }
        });
        return view;
    }

    private static int getInfoGraphId(String tag) {
        try {
            return R.drawable.class.getField("infograph_" +
                    EventCategory.toCategoryParsableString(tag).toLowerCase()).getInt(null);
        } catch (IllegalAccessException| NoSuchFieldException e) {
            // Ignore
        }

        return R.drawable.eh_default_event_list;
    }

    private Listener<List<Event>> mFeaturedEventsListener = new Listener<List<Event>>() {
        @Override
        public void onResponse(final List<Event> events, boolean isIntermediate) {
            FeaturedEventsAdapter featuredEventsAdapter =
                    new FeaturedEventsAdapter(LaunchActivity.this, events);

            LayoutInflater layoutInflater = getLayoutInflater();
            dotsView.removeAllViews();
            for (int i = 0; i < featuredEventsAdapter.getCount(); i++) {
                View view = layoutInflater.inflate(R.layout.explore_dot, dotsView, false);
                view.setSelected(i == 0);
                dotsView.addView(view);
            }

            featuredEventsPager.setAdapter(featuredEventsAdapter);
            featuredEventsPager.setOnPageChangeListener(new OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset,
                                           int positionOffsetPixels) {
                    // do nothing.
                }

                @Override
                public void onPageSelected(int position) {
                    if (position != 0) {
                        reportActionToAnalytics("featuredSwipe");
                    }
                    for (int i = 0; i < dotsView.getChildCount(); i++) {
                        dotsView.getChildAt(i).setSelected(i == position);
                    }
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    // do nothing.
                }
            });
        }
    };
}
