package com.eventshigh.nearme.app.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.IntentUtils;

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
    public static final int SECONDS_FOR_REFRESH = 600;

    // ***********************
    // MEMBERS
    // ***********************

    // UI elements.
    protected FrameLayout eventContainer;
    protected SearchView searchView;

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
        setContentView(R.layout.activity_events);
        eventContainer = (FrameLayout) findViewById(R.id.event_container);

        // Setup action bar.
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
            setTitle();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // The activity could have started either for first time or when user
        // launches the sleeping app or when he returns from details pane.
        // We do not refresh the app if user is in same session or has returned
        // within {@code SECONDS_FOR_REFRESH} seconds.
        if (lastStartedAt < System.currentTimeMillis() - SECONDS_FOR_REFRESH * 1000) {
            showEvents();
            lastStartedAt = System.currentTimeMillis();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_event, menu);

        // Search View.
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Set visibility.
        menu.findItem(R.id.action_show_map).setVisible(isPlayServicesPresent);
        menu.findItem(getDisabledMenuItem()).setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

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


    // ***********************
    // Delegated methods
    // ***********************

    /**
     * @return the id of menu item to be disabled.
     */
    protected abstract int getDisabledMenuItem();

    /**
     * Fetch and show events.
     */
    protected abstract void showEvents();


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

    protected void setTitle() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            String title = DateTimeUtils.queryToTitle(eventsContext.query);
            if (!eventsContext.dateFilter.isEmpty()) {
                title += " on " +  DateTimeUtils.queryToTitle(eventsContext.dateFilter);
            }
            actionBar.setTitle(title);
        }
    }
}
