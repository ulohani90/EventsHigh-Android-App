package com.eventshigh.nearme.app.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
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
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.Editor;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.EventUberPrefetcher;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.EventSearchSuggestionsProvider;
import com.eventshigh.nearme.app.user.Account;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
    public static final int NUM_MAX_PREFETCH = 10;
    public static final int SECONDS_FOR_REFRESH = 600;


    // ***********************
    // MEMBERS
    // ***********************

    // UI elements.
    private ViewSwitcher viewSwitcher;
    private View topProgressBar;
    private SlidingTabLayout dateFilter;
    protected FrameLayout eventContainer;
    protected ImageButton fab;

    private View followButton;
    private View followingButton;

    // Last city and query for which events are shown.
    protected EventsContext eventsContext;
    private boolean isDataShown = false;
    // when was this activity last started on.
    private long lastStartedAt;
    // GoogleApiClient to report the page view.
    private GoogleApiClient client;
    protected Editor eventsMarkerEditor;


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
        dateFilter = (SlidingTabLayout) findViewById(R.id.date_filter);
        topProgressBar = findViewById(R.id.top_progress_bar);
        eventContainer = (FrameLayout) findViewById(R.id.event_container);
        fab = (ImageButton) findViewById(R.id.fab_switch_view);
        followButton = findViewById(R.id.follow_button);
        followingButton = findViewById(R.id.following_button);

        // Set the context in term of eventFetcherParam. Use Intent
        // to restore the context.
        lastStartedAt = 0;

        // See if we have context passed to us within intent.
        eventsContext = IntentUtils.processIntent(this, getIntent());
        if (eventsContext.location == null) {
            City lastCity = GcmRegistration.getInstance(this).getLastCity();
            if (lastCity != null) {
                reportActionToAnalytics("usedLastCity");
                eventsContext.changeLocation(lastCity.cityBounds.getCenter());
            }
        }

        // Show query as title.
        if (!eventsContext.query.isEmpty()) {
            getSupportActionBar().setTitle(DateTimeUtils.queryToTitle(eventsContext.query));
            eventsContext.dateFilter = "";
        }

        boolean showFollowScreen = !eventsContext.query.isEmpty() &&
                !EventsHighEndpoints.isDateQuery(eventsContext.query) &&
                !EventsHighEndpoints.isMyEventQuery(eventsContext.query);
        View followWidget = findViewById(R.id.follow_widget);
        followWidget.setVisibility(showFollowScreen ? View.VISIBLE : View.GONE);
        if (showFollowScreen) {
            reportActionToAnalytics("search", eventsContext.query);
            EventSearchSuggestionsProvider.saveRecentQuery(this, eventsContext.query);

            final Account account = new Account(this);
            TextView followWidgetTitle = (TextView) findViewById(R.id.follow_title);
            followWidgetTitle.setText(Utils.capitalize(eventsContext.query));
            setFollowButtons(account.isFollowing(eventsContext.query));

            followButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportActionToAnalytics("addFollowing", eventsContext.query);
                    account.setIsFollowing(eventsContext.query, true);
                    setFollowButtons(true);
                }
            });
            followingButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportActionToAnalytics("removeFollowing", eventsContext.query);
                    account.setIsFollowing(eventsContext.query, false);
                    setFollowButtons(false);
                }
            });
        }

        // See if date filter is passed.
        showDateFilter();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Setup GoogleApiClient
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.APP_INDEX_API).build();
        client.registerConnectionCallbacks(new ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                if (eventsContext != null) {
                    Uri webUri = EventsHighEndpoints.getWebUri(eventsContext);
                    String title = eventsContext.toString();
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
            LatLng location = eventsContext.location;
            eventsContext.changeLocation(null);
            updateUserLocation(location);
        }

        lastStartedAt = System.currentTimeMillis();

        // Initialize the EventsMarkerManager.Editor.
        eventsMarkerEditor = EventsMarkerManager.getInstance(this).getEditor();

        // Show the verify phone snakbar if needed.
        if (Account.isPhoneVerifyPending(this)) {
            showVerifyPhoneSnackbar();
        }

        // Set the visibility of fab icon.
        fab.setVisibility(isPlayServicesPresent ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onStop() {
        topProgressBar.setVisibility(View.GONE);
        eventsMarkerEditor.close();

        if (client != null && client.isConnected()) {
            if (eventsContext != null) {
                Uri webUri = EventsHighEndpoints.getWebUri(eventsContext);
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
        appData.putParcelable(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);
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
        menu.findItem(R.id.action_filter_date).setVisible(eventsContext.query.isEmpty());
        menu.findItem(R.id.action_filter).setVisible(
                eventsContext.query.isEmpty() || EventsHighEndpoints.isDateQuery(eventsContext.query));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_filter_date) {
            if (eventsContext.dateFilter.isEmpty()) {
                eventsContext.setDateFilter(Calendar.getInstance());
                showDateFilter();
            } else {
                eventsContext.dateFilter = "";
                dateFilter.setVisibility(View.GONE);
            }
            fetchNewListing(false);
            return true;
        }

        if (id == R.id.action_filter) {
            Intent filterActivityIntent = new Intent(this, ShowFiltersActivity.class);
            filterActivityIntent.putStringArrayListExtra(ShowFiltersActivity.PARAM_FILTERS,
                    eventsContext.categoryFilters);
            startActivityForResult(filterActivityIntent, 0);
            return true;
        }

        if (id == R.id.action_shortcut) {
            createShortcut();
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
        showDateFilter();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data.hasExtra(ShowFiltersActivity.PARAM_FILTERS)) {
            eventsContext.categoryFilters =
                    data.getStringArrayListExtra(ShowFiltersActivity.PARAM_FILTERS);
            fetchNewListing(false);
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
        eventsContext.changeLocation(userLocation);
        if (eventsContext.city == null) {
            if (userLocation != null) {
                reportActionToAnalytics("unsupportedCity");
                Toast.makeText(this, R.string.unsupported_city, Toast.LENGTH_SHORT).show();
            }
            updateEventsCollection(new ArrayList<Event>());
        } else {
            fetchNewListing(false /* bypass cache*/);
        }
    }

    protected void updateEventsCollection(List<Event> events) {
        isDataShown = true;

        // Prefetch first 10 events.
        for (Event event : events.subList(0, Math.min(events.size(), NUM_MAX_PREFETCH))) {
            EventUberPrefetcher.getInstance(this).prefetch(event.id);
        }
    }

    protected void updateMyEvents(MyEvents myEvents) {
        isDataShown = true;

        // Prefetch first 10 events.
        int numPrefetched = 0;
        for (Pair<String, List<Event>> myEventEntry : myEvents) {
            for (Event event : myEventEntry.second) {
                if (numPrefetched >= NUM_MAX_PREFETCH) {
                    break;
                }
                EventUberPrefetcher.getInstance(this).prefetch(event.id);
                numPrefetched ++;
            }
        }
    }

    /**
     * @return true if events without location information are shown.
     */
    protected abstract boolean shouldIncludeWithoutLocation();

    /**
     * Removes an event from view as it was marked as "not interested".
     * @param event event to remove.
     */
    protected abstract void remove(Event event);


    // ***********************
    // Helper methods
    // ***********************

    public @Nullable EventMark getEventMark(Event event) {
        return eventsMarkerEditor.getEventsMarkerManager().getEventMark(event.id);
    }

    public void recordEventMark(Event event, @Nullable EventMark mark) {
        if (EventMark.isDismissed(mark)) {
            remove(event);
        }
        eventsMarkerEditor.recordEventMark(event, mark);
    }

    public void reportEventAction(Event event, String actionName, int position) {
        reportActionToAnalytics(actionName,
                eventsContext.dateFilter,
                1,
                isFavourite(event) ? "Favourite" : "No-Favourite",
                event.ehRecommended ? "Recommended" : "Non-Recommended",
                eventsContext.query.isEmpty() ? " " : eventsContext.query,
                Integer.toString(position));
    }

    public void showEventDetails(Event event, int position) {
        reportEventAction(event, "showEventDetails", position);
        showEventDetails(event);
    }

    public void showSearchView(String query) {
        reportActionToAnalytics("showSearchView");
        EventsContext param = new EventsContext(eventsContext.location, query);
        Intent intent = new Intent(this, this.getClass())
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
        startActivity(intent);
    }

    protected void switchTo(Class<?> cls) {
        reportActionToAnalytics("switchView");
        Intent intent = new Intent(this, cls)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);
        startActivity(intent);
    }

    protected void fetchNewListing(boolean shouldBypassCache) {
        topProgressBar.setVisibility(View.VISIBLE);
        viewSwitcher.setDisplayedChild(0);

        // Stop all requests associated with this activity and then submit new request.
        VolleyHelper.getRequestQueue(this).cancelAll(this);
        if (EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
            new MyEventsRequest(this, eventsContext, Priority.IMMEDIATE,
                    shouldBypassCache, shouldIncludeWithoutLocation(),
                    mMyEventsFetcherCallBack, mErrorListener).execute();
        } else {
            EventCollectionRequest.submit(this, eventsContext, Priority.IMMEDIATE,
                    shouldBypassCache, shouldIncludeWithoutLocation(),
                    mEventsFetcherCallBack, mErrorListener);
        }
    }

    private void createShortcut() {
        reportActionToAnalytics("createShortcut");

        // Create an intent the shortcut could launch.
        Intent shortcutIntent = new Intent(getApplicationContext(), getClass());
        shortcutIntent.putExtra(SearchManager.QUERY, eventsContext.query);
        if (eventsContext.location != null) {
            shortcutIntent.putExtra(IntentUtils.EXTRA_LATITUDE_PARAM,
                    eventsContext.location.latitude);
            shortcutIntent.putExtra(IntentUtils.EXTRA_LONGITUDE_PARAM,
                    eventsContext.location.longitude);
        }
        shortcutIntent.setAction(Intent.ACTION_SEARCH);

        // Create an intent for creating shortcut and broadcast it.
        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                eventsContext.query.isEmpty() ? getString(R.string.app_name)
                        : Utils.capitalize(eventsContext.query));
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

    public void onRetry(View view) {
        reportActionToAnalytics("retryFetch");
        fetchNewListing(false /* bypass cache*/);
    }

    public void onFindEvents(View view) {
        reportActionToAnalytics("findEvents");
        showSearchView("");
        finish();
    }

    public void onExploreCategories(View view) {
        reportActionToAnalytics("exploreCategories");
        finish();
    }

    private Listener<MyEvents> mMyEventsFetcherCallBack = new Listener<MyEvents>() {
        @Override
        public void onResponse(MyEvents myEvents, boolean isIntermediate) {
            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (myEvents.isEmpty()) {
                    viewSwitcher.setDisplayedChild(1);
                    findViewById(R.id.view_retry).setVisibility(View.GONE);
                    findViewById(R.id.view_no_my_event).setVisibility(View.VISIBLE);
                }
            }

            if (!isIntermediate || !myEvents.isEmpty()) {
                updateMyEvents(myEvents);
            }
        }
    };

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private Listener<List<Event>> mEventsFetcherCallBack = new Listener<List<Event>>() {
        @Override
        public void onResponse(List<Event> events, boolean isIntermediate) {
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

    private ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);
            if (isDataShown) {
                Toast.makeText(BaseEventsActivity.this, R.string.failed_refresh, Toast.LENGTH_SHORT).show();
            } else {
                viewSwitcher.setDisplayedChild(1);
                findViewById(R.id.view_retry).setVisibility(View.VISIBLE);
                findViewById(R.id.view_no_my_event).setVisibility(View.GONE);
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

    private void showDateFilter() {
        if (!eventsContext.dateFilter.isEmpty()) {
            DatesPagerAdapter adapter = new DatesPagerAdapter(getSupportFragmentManager());
            ViewPager dummyViewPager = new ViewPager(this);
            dummyViewPager.setVisibility(View.GONE);
            dummyViewPager.setAdapter(adapter);

            dateFilter.setVisibility(View.VISIBLE);
            dateFilter.setViewPager(dummyViewPager);
            dateFilter.setOnPageChangeListener(adapter);
            dateFilter.setCustomTabColorizer(adapter);
            dateFilter.scrollTo(eventsContext.dateFilter);
        }
    }

    private class DatesPagerAdapter extends SlidingTabPagerAdapter
            implements OnPageChangeListener, TabColorizer {
        private static final int NUM_DAYS = 14;

        private class DateTabView {
            private final View root;
            public final TextView weekDayView;
            public final TextView dayOfMonthView;

            public DateTabView(View root) {
                this.root  = root;
                weekDayView = (TextView) root.findViewById(R.id.weekday);
                dayOfMonthView = (TextView) root.findViewById(R.id.dayofmonth);
            }
        }

        private final Calendar today = DateTimeUtils.toMidnight(Calendar.getInstance(), null);
        private final List<DateTabView> dateTabViews = new ArrayList<>(NUM_DAYS);
        private int lastPosition = -1;

        public DatesPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return new Fragment();
        }

        @Override
        public int getCount() {
            return NUM_DAYS;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return EventsContext.formatDateFilter(getDate(position));
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // do nothing
        }

        @Override
        public void onPageSelected(int position) {
            if (lastPosition >= 0) {
                TextView last = dateTabViews.get(lastPosition).dayOfMonthView;
                last.setTextColor(0xff797979);
                last.setBackgroundResource(android.R.color.transparent);
                last.setTypeface(null, Typeface.NORMAL);
            }

            TextView selected = dateTabViews.get(position).dayOfMonthView;
            selected.setTextColor(getResources().getColor(android.R.color.white));
            selected.setBackgroundResource(R.drawable.shape_oval);
            selected.setTypeface(null, Typeface.BOLD);
            lastPosition = position;

            String oldDateFilter = eventsContext.dateFilter;
            eventsContext.setDateFilter(getDate(position));
            if (! oldDateFilter.equals(eventsContext.dateFilter)) {
                fetchNewListing(false);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // do nothing
        }

        @Override
        public View getView(int position, ViewGroup parent) {
            Calendar calendar = getDate(position);
            DateTabView dateTabView = new DateTabView(
                    getLayoutInflater().inflate(R.layout.tab_date, parent, false));
            dateTabView.weekDayView.setText(
                    calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US));
            dateTabView.dayOfMonthView.setText(
                    Integer.toString(calendar.get(Calendar.DAY_OF_MONTH)));

            dateTabViews.add(position, dateTabView);
            return dateTabView.root;
        }

        @Override
        public int getIndicatorColor(int position) {
            return 0x00000000;
        }

        @Override
        public int getDividerColor(int position) {
            return 0x26000000;
        }

        private Calendar getDate(int position) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(today.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, position);
            return calendar;
        }
    }

    private void setFollowButtons(boolean isFollowing) {
        followButton.setVisibility(isFollowing ? View.GONE : View.VISIBLE);
        followingButton.setVisibility(isFollowing ? View.VISIBLE : View.GONE);
        followButton.setSelected(true);
    }
}
