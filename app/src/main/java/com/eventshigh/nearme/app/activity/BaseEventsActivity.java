package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
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
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.data.EventsCollection;
import com.eventshigh.nearme.app.data.EventsCollection.TaggedEvents;
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
import com.eventshigh.nearme.app.utils.Utils;
import com.example.android.common.view.SlidingTabLayout;
import com.example.android.common.view.SlidingTabPagerAdapter;
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

    // ***********************
    // CONSTANTS
    // ***********************
    public static final String EXTRA_TAG_NAME_PARAM = "extra.event.tag.name";
    public static final int NUM_MAX_TABS = 10;
    public static final int NUM_MAX_PREFETCH = 10;
    public static final int SECONDS_FOR_REFRESH = 600;


    // ***********************
    // MEMBERS
    // ***********************

    // UI elements for showing loading dialog and event contents through ViewPager.
    private ViewSwitcher viewSwitcher;
    private ViewPager viewPager;
    private SlidingTabLayout slidingTab;

    // The view that shows a loading message to the user when events are being fetched
    private View loadingMessageView;
    // The view that shows an error message to the user when the events fetch request fails
    private View errorMessageView;
    // The progress bar just below action bar that is visible when there is a pending event fetch
    // request
    private View topProgressBar;

    // Last city and query for which events are shown.
    protected EventFetcherParam lastEventFetcherParam;
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

        // Setup the UI.
        viewSwitcher = new ViewSwitcher(this);
        getLayoutInflater().inflate(R.layout.activity_events, viewSwitcher);
        View messageView = getLayoutInflater().inflate(R.layout.view_loading_retry, viewSwitcher);
        loadingMessageView = messageView.findViewById(R.id.loading_message);
        errorMessageView = messageView.findViewById(R.id.error_message);
        setContentView(viewSwitcher);
        slidingTab = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        viewPager = (ViewPager) findViewById(R.id.pager);
        topProgressBar = findViewById(R.id.top_progress_bar);

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

        if (id == R.id.action_refresh) {
            fetchNewListing(false /** show loading view */, true /* bypass cache */);
        }

        return super.onOptionsItemSelected(item);
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
        lastEventFetcherParam.changeLocation(userLocation);
        if (lastEventFetcherParam.city == null) {
            if (userLocation != null) {
                reportActionToAnalytics("unsupportedCity");
                Toast.makeText(this, R.string.unsupported_city, Toast.LENGTH_SHORT).show();
            }
            updateEventsCollection(new EventsCollection(new ArrayList<TaggedEvents>()));
            updateEventListing(new ArrayList<Event>());
        } else {
            fetchNewListing(true /** show loading view */, false /* bypass cache */);
        }
    }

    protected void updateEventListing(List<Event> events) {
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
     * @return true if the view represented by this activity is default view.
     */
    protected abstract int getDisabledMenuId();

    /**
     * @return a new Fragment which will be used to show events list.
     */
    protected abstract Fragment getNewFragment();


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

    private void updateEventsCollection(EventsCollection events) {
        String tagToSearch = lastSelectedTag;

        EventCollectionPagerAdapter adapter =
                new EventCollectionPagerAdapter(getSupportFragmentManager(), events);
        viewPager.setAdapter(adapter);
        slidingTab.setCustomTabView(R.layout.tab_title, R.id.tab_title, R.id.num_events);
        slidingTab.setViewPager(viewPager);
        slidingTab.setOnPageChangeListener(adapter);

        if (events.getTags().isEmpty()) {
            // nothing to show.
            return;
        }

        int currentItem = 0;
        if (tagToSearch != null) {
            List<Pair<String, Integer>> tags = events.getTags();
            for (int i = 0; i < tags.size(); i++) {
                if (tags.get(i).first.equals(tagToSearch)) {
                    currentItem = i;
                    break;
                }
            }
        }

        if (currentItem == 0) {
            updateEventListing(events.getEvents(0));
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
                }, 100);
            }
        }

        // Show on boarding if first time.
        viewPager.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (onBoardingHelper == null) {
                    onBoardingHelper = new OnBoardingHelper(BaseEventsActivity.this);
                }

                onBoardingHelper.next();
            }
        }, 1500);
    }

    private void fetchNewListing(boolean shouldBypassCache,
                                 final ErrorListener errorListener) {
        EventCollectionRequest.submit(getApplicationContext(), lastEventFetcherParam,
                Priority.IMMEDIATE, this, shouldBypassCache, mEventsFetcherCallBack,
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        errorListener.onErrorResponse(volleyError);
                        mErrorListener.onErrorResponse(volleyError);
                    }
                });
    }

    private void fetchNewListing(final boolean showLoadingView, boolean shouldBypassCache) {
        if (showLoadingView) {
            viewSwitcher.setDisplayedChild(1);
            loadingMessageView.setVisibility(View.VISIBLE);
            errorMessageView.setVisibility(View.GONE);
        } else {
            topProgressBar.setVisibility(View.VISIBLE);
        }
        fetchNewListing(shouldBypassCache,
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (showLoadingView) {
                            viewSwitcher.setDisplayedChild(1);
                            loadingMessageView.setVisibility(View.GONE);
                            errorMessageView.setVisibility(View.VISIBLE);
                        } else {
                            topProgressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }

    public void fetchNewListing(final ErrorListener errorListener) {
        fetchNewListing(true /* bypass cache */, errorListener);
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


    // ***********************
    // Callbacks
    // ***********************

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private Listener<EventsCollection> mEventsFetcherCallBack = new Listener<EventsCollection>() {
        @Override
        public void onResponse(EventsCollection events, boolean isIntermediate) {
            viewSwitcher.setDisplayedChild(0);
            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);
            }

            if (events.getTags().isEmpty()) {
                // Failed. Show toast and return empty list.
                Toast.makeText(BaseEventsActivity.this, R.string.no_events, Toast.LENGTH_SHORT).show();
            }

            updateEventsCollection(events);
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
        fetchNewListing(true /** show loading view */, true /* bypass cache */);
    }

    private class EventCollectionPagerAdapter extends SlidingTabPagerAdapter
            implements OnPageChangeListener {
        private final EventsCollection events;
        private final List<Pair<String, Integer>> tags;

        public EventCollectionPagerAdapter(FragmentManager fm, EventsCollection events) {
            super(fm);
            this.events = events;
            this.tags = events.getTags();
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = getNewFragment();
            Bundle args = new Bundle();
            ArrayList<Event> eventsToShow = new ArrayList<>();
            eventsToShow.addAll(events.getEvents(i));
            args.putParcelableArrayList(EventGridFragment.EVENTS_LIST_PARAMETER, eventsToShow);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            return Math.min(tags.size(), NUM_MAX_TABS);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return tags.get(position).first;
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // do nothing
        }

        @Override
        public void onPageSelected(int position) {
            lastSelectedTag = tags.get(position).first;
            updateEventListing(events.getEvents(position));
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // do nothing
        }

        @Override
        public String getNumEvents(int position) {
            return Integer.toString(events.getEvents(position).size());
        }
    }
}
