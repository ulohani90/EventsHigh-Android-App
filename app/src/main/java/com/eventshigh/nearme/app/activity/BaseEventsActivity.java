package com.eventshigh.nearme.app.activity;

import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.data.EventsCollection;
import com.eventshigh.nearme.app.data.EventsCollection.EventTab;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.EventUberPrefetcher;
import com.eventshigh.nearme.app.settings.SettingsActivity;
import com.eventshigh.nearme.app.ui.DatePickerFragment;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;
import com.eventshigh.nearme.app.ui.LocationPickerDialog;
import com.eventshigh.nearme.app.ui.LocationPickerDialog.OnLocationSelection;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.example.android.common.view.SlidingTabLayout;
import com.example.android.common.view.SlidingTabLayout.TabColorizer;
import com.example.android.common.view.SlidingTabPagerAdapter;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.AppRate.OnShowListener;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;

/**
 * Base activity for location aware events listing. This class implements common methods to fetch
 * fetch event listings when needed and asking the parent activity to show events as per user
 * interactions.
 *
 * This class also implements base user interactions like tabs, filters etc.
 */
public abstract class BaseEventsActivity extends BaseActivity {
    private static final String LOG_TAG = BaseEventsActivity.class.getSimpleName();

    // ***********************
    // CONSTANTS
    // ***********************
    public static final String EXTRA_EVENT_TAB_NAME = "extra.event.tab.name";
    public static final int NUM_MAX_PREFETCH = 10;
    public static final int SECONDS_FOR_REFRESH = 600;


    // ***********************
    // MEMBERS
    // ***********************

    // UI elements.
    private ViewSwitcher viewSwitcher;
    private View topProgressBar;
    private SlidingTabLayout slidingTab;
    private ViewPager viewPager;
    protected ImageButton fab;

    // Last city and query for which events are shown.
    protected EventFetcherParam eventFetcherParam;
    // Tag selected from tab bar for which events are shown.
    private String lastSelectedTabName;
    // when was this activity last started on.
    private long lastStartedAt;
    // GoogleApiClient to report the page view.
    private GoogleApiClient client;


    // ***********************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the UI.
        setContentView(R.layout.activity_events);
        viewSwitcher = (ViewSwitcher) findViewById(R.id.view_switcher);
        slidingTab = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        viewPager = (ViewPager) findViewById(R.id.pager);
        topProgressBar = findViewById(R.id.top_progress_bar);

        // Set the context in term of eventFetcherParam. Use Intent
        // to restore the context.
        lastStartedAt = 0;

        // See if we have context passed to us within intent.
        eventFetcherParam = IntentUtils.processIntent(this, getIntent());
        lastSelectedTabName = getIntent().getStringExtra(EXTRA_EVENT_TAB_NAME);

        // Show query as title.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && !eventFetcherParam.query.isEmpty()) {
            actionBar.setTitle(DateTimeUtils.queryToTitle(eventFetcherParam.query));
            if (!EventsHighEndpoints.isDateQuery(eventFetcherParam.query)) {
                reportActionToAnalytics("search", eventFetcherParam.query);
                EventSearchSuggestionsProvider.saveRecentQuery(this, eventFetcherParam.query);
            }
        }

        fab = (ImageButton) findViewById(R.id.fab_switch_view);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(
                    !eventFetcherParam.query.isEmpty() || !isDefaultView());
        }

        // Setup GoogleApiClient
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();
        client.registerConnectionCallbacks(new ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                if (eventFetcherParam != null) {
                    Uri webUri = EventsHighEndpoints.getWebUri(eventFetcherParam);
                    String title = eventFetcherParam.toString();
                    AppIndex.AppIndexApi.view(client, BaseEventsActivity.this, Utils.getAppUri(webUri),
                            title, webUri, null);
                }
            }

            @Override
            public void onConnectionSuspended(int i) {
                // do nothing.
            }
        });

        // The activity could have started either for first time or when user
        // launches the sleeping app or when he returns from details pane.
        // We do not refresh the app if user is in same session or has returned
        // within {@code SECONDS_FOR_REFRESH} seconds.
        if (lastStartedAt < System.currentTimeMillis() - SECONDS_FOR_REFRESH * 1000) {
            // If location is passed in param, use it.
            LatLng location = eventFetcherParam.location;
            if (location == null) {
                City lastCity = GcmRegistration.getInstance(getApplicationContext()).getLastCity();
                if (lastCity != null) {
                    location = lastCity.cityBounds.getCenter();
                }
            }

            if (location == null) {
                askUserForLocation();
            } else {
                eventFetcherParam.changeLocation(null);
                updateUserLocation(location);
            }
        }

        lastStartedAt = System.currentTimeMillis();

        // Show the rate this app in non intrusive way.
        AppRate.with(this)
                .delay(3000).initialLaunchCount(5).retryPolicy(RetryPolicy.EXPONENTIAL)
                .text(R.string.action_share_app).listener(mAppRateOnShowListener)
                .checkAndShow();
    }

    @Override
    protected void onStop() {
        topProgressBar.setVisibility(View.GONE);

        if (client != null && client.isConnected()) {
            if (eventFetcherParam != null) {
                Uri webUri = EventsHighEndpoints.getWebUri(eventFetcherParam);
                AppIndex.AppIndexApi.viewEnd(client, BaseEventsActivity.this, Utils.getAppUri(webUri));
            }
            client.disconnect();
        }

        super.onStop();
    }

    @Override
    public boolean onSearchRequested() {
        reportActionToAnalytics("onSearchRequested");
        Bundle appData = new Bundle();
        appData.putParcelable(IntentUtils.EXTRA_EVENT_FETCHER_PARAM, eventFetcherParam);
        startSearch(null, false, appData, false);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_event, menu);

        // Search View.
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Do not show filterByDate for search.
        if (!eventFetcherParam.query.isEmpty() &&
            !EventsHighEndpoints.isDateQuery(eventFetcherParam.query)) {
            menu.findItem(R.id.action_filter).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_location) {
            askUserForLocation();
            return true;
        }

        if (id == R.id.action_filter) {
            DialogFragment selectDateFragment = new DatePickerFragment();
            if (eventFetcherParam.city != null) {
                Bundle args = new Bundle();
                args.putString(City.class.getName(), eventFetcherParam.city.toString());
                selectDateFragment.setArguments(args);
            }
            selectDateFragment.show(getFragmentManager(), "selectDate");
            return true;
        }

        if (id == R.id.action_shortcut) {
            createShortcut();
            return true;
        }

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        if (id == R.id.action_refresh) {
            reportActionToAnalytics("menuRefresh");
            fetchNewListing(true /* bypass cache*/);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isDataShown()) {
            slidingTab.setViewPager(viewPager);
        }
    }

    // ***********************
    // Delegated methods
    // ***********************

    /**
     * Updates the user location as reported by LocationClient.
     *
     * @param userLocation user location as reported by location client.
     */
    protected void updateUserLocation(@Nullable LatLng userLocation) {
        eventFetcherParam.changeLocation(userLocation);
        if (eventFetcherParam.city == null) {
            if (userLocation != null) {
                reportActionToAnalytics("unsupportedCity");
                Toast.makeText(this, R.string.unsupported_city, Toast.LENGTH_SHORT).show();
            }
            updateEventsCollection(EventsCollection.EMPTY);
            updateEventListing(new ArrayList<Event>(), false);
        } else {
            fetchNewListing(false /* bypass cache*/);
        }
    }

    protected void updateEventListing(List<Event> events, boolean isFavouriteView) {
        // Prefetch first 10 events.
        for (Event event : events.subList(0, Math.min(events.size(), NUM_MAX_PREFETCH))) {
            EventUberPrefetcher.getInstance(getApplicationContext()).prefetch(event.id);
        }
    }

    /**
     * @return true if location should be shown in action bar as subtitle.
     */
    protected abstract boolean showLocationInActionBar();

    /**
     * @return true if the view represented by this activity is default view.
     */
    protected abstract boolean isDefaultView();

    /**
     * @return a new Fragment which will be used to show events list.
     */
    protected abstract Fragment getNewFragment();


    // ***********************
    // Helper methods
    // ***********************

    public void showEventDetails(Event event, int position) {
        reportEventAction(event, "showEventDetails", position);
        Intent detailIntent = new Intent(this, EventDetailActivity.class);
        detailIntent.putExtra(EventDetailFragment.ARG_EVENT_INFO, event);
        startActivity(detailIntent);
    }

    public void showSearchView(String query) {
        EventFetcherParam param = new EventFetcherParam(eventFetcherParam.location, query);
        Intent intent = new Intent(this, this.getClass())
                .putExtra(IntentUtils.EXTRA_EVENT_FETCHER_PARAM, param);
        startActivity(intent);
    }

    protected void askUserForLocation() {
        reportActionToAnalytics("askUserForLocation");
        String countryCode = eventFetcherParam.city == null ?
                null : eventFetcherParam.city.countryCode;
        new LocationPickerDialog().show(this, countryCode, new OnLocationSelection() {
            @Override
            public void onLocationSelection(String locationString, LatLng locationPoint) {
                if (showLocationInActionBar()) {
                    ActionBar actionBar = getSupportActionBar();
                    if (actionBar != null) {
                        actionBar.setSubtitle(locationString);
                    }
                }
                updateUserLocation(locationPoint);
            }
        });
    }

    protected void switchTo(Class<?> cls) {
        reportActionToAnalytics("switchView");
        Intent intent = new Intent(this, cls)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(IntentUtils.EXTRA_EVENT_FETCHER_PARAM, eventFetcherParam);
        if (lastSelectedTabName != null) {
            intent.putExtra(EXTRA_EVENT_TAB_NAME, lastSelectedTabName);
        }
        startActivity(intent);
    }

    protected void fetchNewListing(boolean shouldBypassCache) {
        viewSwitcher.setDisplayedChild(0);
        topProgressBar.setVisibility(View.VISIBLE);

        EventCollectionRequest.submit(this, eventFetcherParam, Priority.IMMEDIATE,
                shouldBypassCache, mEventsFetcherCallBack, mErrorListener);
    }

    private boolean isDataShown() {
        return viewPager.getAdapter() != null && viewPager.getAdapter().getCount() > 0;
    }

    private void updateEventsCollection(EventsCollection events) {
        String tagToSearch = lastSelectedTabName;

        EventsPagerAdapter adapter = new EventsPagerAdapter(getSupportFragmentManager(), events);
        viewPager.setAdapter(adapter);
        slidingTab.setCustomTabColorizer(adapter);
        slidingTab.setCustomTabView(R.layout.tab_title, R.id.tab_title, R.id.num_events);
        slidingTab.setViewPager(viewPager);
        slidingTab.setOnPageChangeListener(adapter);

        if (events.isEmpty()) {
            // nothing to show.
            return;
        }

        int currentItem = 0;
        if (tagToSearch != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                if (adapter.getPageTitle(i).equals(tagToSearch)) {
                    currentItem = i;
                    break;
                }
            }
        }

        if (currentItem == 0) {
            adapter.onPageSelected(0);
        } else {
            viewPager.setCurrentItem(currentItem);
            final View selectedItem =
                    ((LinearLayout) slidingTab.getChildAt(0)).getChildAt(currentItem);
            if (selectedItem != null) {
                Utils.waitForViewVisible(selectedItem, new Runnable() {
                    @Override
                    public void run() {
                        if (selectedItem.getLeft() > 100) {
                            slidingTab.scrollTo(selectedItem.getLeft() - 100, 0);
                        }
                    }
                });
            }
        }
    }

    private void createShortcut() {
        reportActionToAnalytics("createShortcut");

        // Create an intent the shortcut could launch.
        Intent shortcutIntent = new Intent(getApplicationContext(), getClass());
        shortcutIntent.putExtra(SearchManager.QUERY, eventFetcherParam.query);
        if (eventFetcherParam.location != null) {
            shortcutIntent.putExtra(IntentUtils.EXTRA_LATITUDE_PARAM,
                    eventFetcherParam.location.latitude);
            shortcutIntent.putExtra(IntentUtils.EXTRA_LONGITUDE_PARAM,
                    eventFetcherParam.location.longitude);
        }
        shortcutIntent.setAction(Intent.ACTION_SEARCH);

        // Create an intent for creating shortcut and broadcast it.
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, eventFetcherParam.toString());
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                        R.drawable.ic_launcher));
        addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        getApplicationContext().sendBroadcast(addIntent);

        // Go to home screen so that user can see the new icon.
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }


    // ***********************
    // Callbacks
    // ***********************

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private Listener<EventsCollection> mEventsFetcherCallBack = new Listener<EventsCollection>() {
        @Override
        public void onResponse(EventsCollection events, boolean isIntermediate) {
            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (events.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    Toast.makeText(BaseEventsActivity.this, R.string.no_events, Toast.LENGTH_SHORT).show();
                }
            }

            if (!isIntermediate || !events.isEmpty()) {
                updateEventsCollection(events);
            }
        }
    };

    private OnShowListener mAppRateOnShowListener = new OnShowListener() {
        @Override
        public void onRateAppShowing(AppRate appRate, final View view) {
            reportActionToAnalytics("shareAppShown");
            view.findViewById(R.id.dar_rate_element).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareApp();
                    view.setVisibility(View.GONE);
                }
            });
        }

        @Override
        public void onRateAppDismissed() {
            reportActionToAnalytics("shareAppDismissed");
        }

        @Override
        public void onRateAppClicked() {
        }
    };

    public void onRetryButtonClicked(View view) {
        reportActionToAnalytics("retryFetch");
        fetchNewListing(false /* bypass cache*/);
    }

    private class EventsPagerAdapter extends SlidingTabPagerAdapter
            implements OnPageChangeListener, TabColorizer {
        private final EventsCollection events;
        private final List<Pair<EventTab, Integer>> tabs;

        public EventsPagerAdapter(FragmentManager fm, EventsCollection events) {
            super(fm);
            this.events = events;
            this.tabs = events.getTabs();
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = getNewFragment();
            Bundle args = new Bundle();
            ArrayList<Event> eventsToShow = new ArrayList<>();
            eventsToShow.addAll(events.getEvents(position));
            args.putBoolean(EventGridFragment.IS_FAVOURITE_VIEW_PARAMETER, isFavouriteView(position));
            args.putParcelableArrayList(EventGridFragment.EVENTS_LIST_PARAMETER, eventsToShow);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return tabs.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return Utils.capitalize(tabs.get(position).first.toString());
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // do nothing
        }

        @Override
        public void onPageSelected(int position) {
            lastSelectedTabName = tabs.get(position).first.name();
            updateEventListing(events.getEvents(position), isFavouriteView(position));
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // do nothing
        }

        @Override
        public String getNumEvents(int position) {
            return isFavouriteView(position) ? "" : Integer.toString(events.getEvents(position).size());
        }

        private boolean isFavouriteView(int position) {
            return tabs.get(position).first == EventTab.FAVOURITES;
        }

        @Override
        public int getIndicatorColor(int position) {
            return getResources().getColor(tabs.get(position).first.colorId);
        }

        @Override
        public int getDividerColor(int position) {
            return 0x26000000;
        }
    }

    protected ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);
            if (isDataShown()) {
                Toast.makeText(BaseEventsActivity.this, R.string.failed_refresh, Toast.LENGTH_SHORT).show();
            } else {
                viewSwitcher.setDisplayedChild(1);
            }

            Throwable cause = volleyError.getCause();
            if (cause != null) {
                Log.w(LOG_TAG, "Volley Error: " + volleyError.getMessage(), cause);
                reportActionToAnalytics("failedRequest", cause.getClass().getSimpleName());
            } else {
                Log.w(LOG_TAG, "Volley Error: " + volleyError.getMessage());
                reportActionToAnalytics("failedRequest");
            }
        }
    };

    public void reportEventAction(Event event, String actionName, int position) {
        reportActionToAnalytics(actionName,
                lastSelectedTabName == null ? "" : lastSelectedTabName,
                1,
                isFavourite(event) ? "Favourite" : "No-Favourite",
                event.ehRecommended ? "Recommended" : "Non-Recommended",
                eventFetcherParam.query.isEmpty() ? " " : eventFetcherParam.query,
                Integer.toString(position));
    }
}
