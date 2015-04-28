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
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request.Priority;
import com.android.volley.Response.Listener;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.network.BaseEventListRequest.EventCollection;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.EventUberPrefetcher;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.user.Preferences;
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
public abstract class BaseEventsActivity extends BaseContextActivity {
    // ***********************
    // CONSTANTS
    // ***********************
    public static final int NUM_MAX_PREFETCH = 10;
    public static final int SECONDS_FOR_REFRESH = 600;
    public static final int MAX_TIMES_TO_SHOW_MY_EVENTS_CLUE = 2;

    // ***********************
    // MEMBERS
    // ***********************

    // UI elements.
    protected Toolbar toolbar;
    protected FrameLayout eventContainer;
    protected SearchView searchView;

    private SlidingTabLayout dateFilter;
    private  View noMyEventsView;
    private View myEventsClueView;
    private TextView myEventsClueTextView;

    // Last city and query for which events are shown.
    private boolean isDataShown = false;
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
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dateFilter = (SlidingTabLayout) findViewById(R.id.date_filter);
        topProgressBar = findViewById(R.id.top_progress_bar);
        eventContainer = (FrameLayout) findViewById(R.id.event_container);
        myEventsClueView = findViewById(R.id.my_events_clue);
        myEventsClueTextView = (TextView) myEventsClueView.findViewById(R.id.my_events_clue_text);
        noMyEventsView = findViewById(R.id.view_no_my_event);
        retryView = findViewById(R.id.view_retry);

        // Setup My Events Clue
        myEventsClueView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Event event = (Event) myEventsClueTextView.getTag();
                if (event == null) {
                    showSearchView(EventsHighEndpoints.QUERY_MY_EVENT);
                } else {
                    shareEvent(event, null);
                }
            }
        });
        findViewById(R.id.my_events_clue_close).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMyEventsClue();
            }
        });


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
            eventsContext.dateFilter = "";
            setTitle();
        }

        // See if date filter is passed.
        showDateFilter();
    }

    protected void setTitle() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(DateTimeUtils.queryToTitle(eventsContext.query));
        }
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
        showVerifyPhoneSnackbar();
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

    protected boolean isDataShown() {
        return isDataShown;
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
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Do not show filterByDate for search.
        menu.findItem(R.id.action_filter).setVisible(
                eventsContext.query.isEmpty() || EventsHighEndpoints.isDateQuery(eventsContext.query));

        // Set visibility.
        menu.findItem(R.id.action_show_map).setVisible(isPlayServicesPresent);
        menu.findItem(getDisabledMenuItem()).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_filter) {
            reportActionToAnalytics("applyFilters");
            Intent filterActivityIntent = new Intent(this, ShowFiltersActivity.class);
            filterActivityIntent.putStringArrayListExtra(ShowFiltersActivity.PARAM_FILTERS,
                    eventsContext.categoryFilters);
            startActivityForResult(filterActivityIntent, 0);
            return true;
        }

        if (id == R.id.action_show_map) {
            reportActionToAnalytics("switchToMaps");
            switchTo(EventsMapsActivity.class);
            return true;
        }

        if (id == R.id.action_show_list) {
            reportActionToAnalytics("switchToList");
            switchTo(EventsGridActivity.class);
            return true;
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
     * @return the id of menu item to be disabled.
     */
    protected abstract int getDisabledMenuItem();


    // ***********************
    // Helper methods
    // ***********************

    protected void switchTo(Class<?> cls) {
        reportActionToAnalytics("switchView");
        Intent intent = new Intent(this, cls)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, eventsContext);
        startActivity(intent);
    }

    protected void fetchNewListing(boolean shouldBypassCache) {
        topProgressBar.setVisibility(View.VISIBLE);
        noMyEventsView.setVisibility(View.GONE);
        retryView.setVisibility(View.GONE);

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

    public void showMyEventsClue(@Nullable Event event) {
        Preferences preferences = Preferences.getInstance(this);
        boolean showShareTip = EventsHighEndpoints.isMyEventQuery(eventsContext.query) ||
                (event != null && preferences.getNumTimesMyEventsClueShown() >= MAX_TIMES_TO_SHOW_MY_EVENTS_CLUE);

        if (showShareTip) {
            myEventsClueTextView.setText(R.string.ui_share_event_clue);
            myEventsClueTextView.setTag(event);
        } else {
            myEventsClueTextView.setText(R.string.ui_my_events_clue);
            if (event != null) {
                preferences.incrementNumTimesMyEventsClueShown();
            }
        }

        myEventsClueView.setVisibility(View.VISIBLE);
        myEventsClueView.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideMyEventsClue();
            }
        }, 5000);
    }

    public void hideMyEventsClue() {
        myEventsClueView.setVisibility(View.GONE);
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

    private Listener<MyEvents> mMyEventsFetcherCallBack = new Listener<MyEvents>() {
        @Override
        public void onResponse(MyEvents myEvents, boolean isIntermediate) {
            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (myEvents.isEmpty()) {
                    retryView.setVisibility(View.GONE);
                    noMyEventsView.setVisibility(View.VISIBLE);
                }
            }

            if (!isIntermediate || !myEvents.isEmpty()) {
                updateMyEvents(myEvents);
            }
        }
    };

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private Listener<EventCollection> mEventsFetcherCallBack = new Listener<EventCollection>() {
        @Override
        public void onResponse(EventCollection eventCollection, boolean isIntermediate) {
            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (eventCollection.events.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    Toast.makeText(BaseEventsActivity.this, R.string.no_events, Toast.LENGTH_SHORT).show();
                }
            }

            if (!isIntermediate || !eventCollection.events.isEmpty()) {
                updateEventsCollection(eventCollection.events);
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
            selected.setBackgroundResource(R.drawable.eh_oval);
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
                    getLayoutInflater().inflate(R.layout.view_tab_date, parent, false));
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
}
