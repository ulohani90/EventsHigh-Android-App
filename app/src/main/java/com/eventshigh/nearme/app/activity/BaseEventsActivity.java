package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
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
    // MEMBERS
    // ***********************

    // UI elements.
    protected FrameLayout eventContainer;
    protected SearchView searchView;


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


    // ***********************
    // Helper methods
    // ***********************

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
