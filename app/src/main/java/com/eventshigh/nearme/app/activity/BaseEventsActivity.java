package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SearchView;
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
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.data.EventsCollection;
import com.eventshigh.nearme.app.data.EventsCollection.Builder;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.EventUberPrefetcher;
import com.eventshigh.nearme.app.settings.SettingsActivity;
import com.eventshigh.nearme.app.ui.DatePickerFragment;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;
import com.eventshigh.nearme.app.ui.LocationPickerDialog;
import com.eventshigh.nearme.app.ui.LocationPickerDialog.OnLocationSelection;
import com.eventshigh.nearme.app.ui.OnBoardingHelper;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashSet;
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

    // ***********************
    // CONSTANTS
    // ***********************
    public static final String EXTRA_TAG_NAME_PARAM = "extra.event.tag.name";
    public static final int NUM_MAX_TABS = 10;
    public static final int NUM_PREFETCH = 10;
    public static final int SECONDS_FOR_REFRESH = 600;


    // ***********************
    // MEMBERS
    // ***********************

    // root view for showing loading dialog and event contents.
    private ViewSwitcher viewSwitcher;
    // Last city,day for which events are shown.
    protected EventFetcherParam lastEventFetcherParam;
    // Last fetched events collection.
    private EventsCollection events;
    // Tag selected from tab bar for which events are shown.
    private String lastSelectedTag;
    // On boarding helper.
    private OnBoardingHelper onBoardingHelper;
    // when was this activity last started on.
    private long lastStartedAt;


    // ***********************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewSwitcher = new ViewSwitcher(this);
        getLayoutInflater().inflate(getActivityView(), viewSwitcher);
        getLayoutInflater().inflate(R.layout.activity_event_detail, viewSwitcher);
        setContentView(viewSwitcher);

        // Set the context in term of lastEventFetcherParam. Use Inent
        // to restore the context.
        lastStartedAt = 0;

        // See if we have context passed to us within intent.
        lastEventFetcherParam = IntentUtils.processIntent(this, getIntent());
        lastSelectedTag = getIntent().getStringExtra(EXTRA_TAG_NAME_PARAM);

        // Show query as title.
        ActionBar actionBar = getActionBar();
        if (actionBar != null && !lastEventFetcherParam.query.isEmpty()) {
            actionBar.setTitle(DateTimeUtils.queryToTitle(lastEventFetcherParam.query));
            if (!EventsHighEndpoints.isDateQuery(lastEventFetcherParam.query)) {
                reportActionToAnalytics("search", lastEventFetcherParam.query);
                EventSearchSuggestionsProvider.saveRecentQuery(this, lastEventFetcherParam.query);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Show the Up button in the action bar.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(
                    !lastEventFetcherParam.query.isEmpty() || !isDefaultView());
        }

        // The activity could have started either for first time or when user
        // launches the sleeping app or when he returns from details pane.
        // We do not refresh the app if user is in same session or has returned
        // within {@code SECONDS_FOR_REFRESH} seconds.
        if (lastStartedAt < System.currentTimeMillis() - SECONDS_FOR_REFRESH * 1000) {
            // If location is passed in param, use it.
            LatLng location = lastEventFetcherParam.location;
            if (location == null) {
                City lastCity = GcmRegistration.getInstance(getApplicationContext()).getLastCity();
                if (lastCity != null) {
                    location = lastCity.cityBounds.getCenter();
                }
            }

            if (location == null) {
                askUserForLocation();
            } else {
                lastEventFetcherParam.changeLocation(null);
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
    public boolean onSearchRequested() {
        reportActionToAnalytics("onSearchRequested");
        Bundle appData = new Bundle();
        appData.putParcelable(IntentUtils.EXTRA_EVENT_FETCHER_PARAM, lastEventFetcherParam);
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
        if (!lastEventFetcherParam.query.isEmpty() &&
            !EventsHighEndpoints.isDateQuery(lastEventFetcherParam.query)) {
            menu.findItem(R.id.action_filter).setVisible(false);
        }

        // Debug Views.
        if (isDebug) {
            MenuItem item = menu.findItem(R.id.debug_cache_override);
            item.setVisible(true);
            item.setChecked(EventCollectionRequest.shouldBypassCache);
        }

        int disabledMenuId = getDisabledMenuId();
        if (disabledMenuId > 0) {
            menu.findItem(disabledMenuId).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_list) {
            switchTo(EventsGridActivity.class);
            return true;
        }

        if (id == R.id.action_map) {
            switchTo(EventsMapsActivity.class);
            return true;
        }

        if (id == R.id.action_change_location) {
            askUserForLocation();
            return true;
        }

        if (id == R.id.action_filter) {
            DialogFragment selectDateFragment = new DatePickerFragment();
            if (lastEventFetcherParam.city != null) {
                Bundle args = new Bundle();
                args.putString(City.class.getName(), lastEventFetcherParam.city.toString());
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

        if (id == R.id.debug_cache_override) {
            EventCollectionRequest.shouldBypassCache = !item.isChecked();
            item.setChecked(EventCollectionRequest.shouldBypassCache);
            if (item.isChecked()) {
                fetchNewListing();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // ***********************
    // Delegated methods
    // ***********************

    /**
     * Sets new events data. This is called when we get new events data from server or when
     * user applies some filter through tab.
     *
     * @param events a list of events to show to user.
     */
    protected abstract void updateEventListing(List<Event> events);

    /**
     * Updates the user location as reported by LocationClient.
     *
     * @param userLocation user location as reported by location client.
     */
    protected abstract void updateUserLocation(LatLng userLocation);

    /**
     * @return true if location should be shown in action bar as subtitle.
     */
    protected abstract boolean showLocationInActionBar();

    /**
     * @return true if the view represented by this activity is default view.
     */
    protected abstract boolean isDefaultView();

    /**
     * @return true if the view represented by this activity is default view.
     */
    protected abstract int getDisabledMenuId();

    /**
     * @return the resource id for the activity view.
     */
    protected abstract int getActivityView();


    // ***********************
    // Helper methods
    // ***********************

    public void showEventDetails(Event event) {
        showEventDetails(event, lastSelectedTag);
    }

    public void showSearchView(String query) {
        EventFetcherParam param = new EventFetcherParam(lastEventFetcherParam.location, query);
        Intent intent = new Intent(this, this.getClass())
                .putExtra(IntentUtils.EXTRA_EVENT_FETCHER_PARAM, param);
        startActivity(intent);
    }

    private void updateListingAndMore(List<Event> events) {
        updateEventListing(events);
        if (events.isEmpty()) {
            return;
        }

        // Prefetch first 10 events.
        for (Event event : events.subList(0, Math.min(events.size(), NUM_PREFETCH))) {
            EventUberPrefetcher.getInstance(getApplicationContext()).prefetch(event.id);
        }

        // Show on boarding if first time.
        viewSwitcher.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (onBoardingHelper == null) {
                    onBoardingHelper = new OnBoardingHelper(BaseEventsActivity.this);
                }

                onBoardingHelper.next();
            }
        }, 1000);
    }

    @SuppressWarnings("deprecation")
    private void updateEventsCollection(EventsCollection events) {
        this.events = events;

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
            if (!lastEventFetcherParam.query.isEmpty()) {
                int numEvents = tags.isEmpty() ? 0 : events.getEvents(0).size();
                actionBar.setTitle(DateTimeUtils.queryToTitle(lastEventFetcherParam.query) + " (" + numEvents + ")");
            }

            updateListingAndMore(events.getEvents(0));
        }
    }

    private void fetchNewListing() {
        viewSwitcher.setDisplayedChild(1);
        EventCollectionRequest.submit(getApplicationContext(), lastEventFetcherParam,
                Priority.IMMEDIATE, this, mEventsFetcherCallBack, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        viewSwitcher.setDisplayedChild(0);
                        mErrorListener.onErrorResponse(volleyError);
                    }
                });
    }

    protected void askUserForLocation() {
        reportActionToAnalytics("askUserForLocation");
        String countryCode = lastEventFetcherParam.city == null ?
                null : lastEventFetcherParam.city.countryCode;
        new LocationPickerDialog().show(this, countryCode, new OnLocationSelection() {
            @Override
            public void onLocationSelection(String locationString, LatLng locationPoint) {
                if (showLocationInActionBar()) {
                    ActionBar actionBar = getActionBar();
                    if (actionBar != null) {
                        actionBar.setSubtitle(locationString);
                    }
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
                .putExtra(IntentUtils.EXTRA_EVENT_FETCHER_PARAM, lastEventFetcherParam);
        if (lastSelectedTag != null) {
            intent.putExtra(EXTRA_TAG_NAME_PARAM, lastSelectedTag);
        }
        startActivity(intent);
    }

    private void createShortcut() {
        reportActionToAnalytics("createShortcut");

        // Create an intent the shortcut could launch.
        Intent shortcutIntent = new Intent(getApplicationContext(), getClass());
        shortcutIntent.putExtra(SearchManager.QUERY, lastEventFetcherParam.query);
        if (lastEventFetcherParam.location != null) {
            shortcutIntent.putExtra(IntentUtils.EXTRA_LATITUDE_PARAM,
                    lastEventFetcherParam.location.latitude);
            shortcutIntent.putExtra(IntentUtils.EXTRA_LONGITUDE_PARAM,
                    lastEventFetcherParam.location.longitude);
        }
        shortcutIntent.setAction(Intent.ACTION_SEARCH);

        // Create an intent for creating shortcut and broadcast it.
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, lastEventFetcherParam.toString());
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

    @SuppressWarnings("deprecation")
    private void basicTabs() {
        events = new Builder(City.BANGALORE, new HashSet<String>()).build();

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

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private Listener<EventsCollection> mEventsFetcherCallBack = new Listener<EventsCollection>() {
        @Override
        public void onResponse(EventsCollection events) {
            viewSwitcher.setDisplayedChild(0);

            if (events.getTags().isEmpty()) {
                // Failed. Show toast and return empty list.
                Toast.makeText(BaseEventsActivity.this, R.string.no_events, Toast.LENGTH_SHORT).show();
                return;
            }

            updateEventsCollection(events);
        }
    };

    private TabListener mTabListener = new TabListener() {
        @Override
        @SuppressWarnings({"deprecation", "ConstantConditions"})
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (events != null) {
                lastSelectedTag = tab.getTag().toString();
                List<Event> eventsForTag = events.getEvents(tab.getPosition());
                if (!eventsForTag.isEmpty() && getActionBar().getNavigationItemCount() > 1) {
                    reportActionToAnalytics("filterByCategory", lastSelectedTag);
                }
                updateListingAndMore(eventsForTag);
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
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
}
