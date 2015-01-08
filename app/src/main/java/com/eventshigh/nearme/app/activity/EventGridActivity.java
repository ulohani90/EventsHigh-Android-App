package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.app.SearchManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.SearchView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.task.UpdateLocationTask;
import com.eventshigh.nearme.app.ui.EventListAdapter;
import com.eventshigh.nearme.app.utils.EventComparator;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * An {@link android.app.Activity} which shows the events in Grid. On Phone,
 * we have one column in portrait mode and two columns in landscape mode.
 * On Tablet, we try to put more columns as per the width offered.
 */
public class EventGridActivity extends LocationAwareEventActivity {
    private EventListAdapter mEventsListAdapter;

    // ***********************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_grid);

        // Setup adapter.
        GridView eventGridView = (GridView) findViewById(R.id.event_grid);
        mEventsListAdapter = new EventListAdapter(this);
        eventGridView.setAdapter(mEventsListAdapter);
        eventGridView.setOnItemClickListener(mOnItemClickListener);
    }

    protected void onStart() {
        super.onStart();

        // Show the Up button in the action bar.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(pref.isMapsViewDefault());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_grid, menu);

        // Search View.
        if (!pref.isMapsViewDefault()) {
            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        } else {
            menu.findItem(R.id.action_search).setVisible(false);
        }

        // Debug Views.
        if (isDebug) {
            menu.findItem(R.id.debug_cache_override).setVisible(true);
        }

        return true;
    }


    // ***********************
    // Helper Methods
    // ***********************

    @Override
    protected void updateNewEvents(List<Event> events) {
        mEventsListAdapter.clear();
        mEventsListAdapter.addAll(events);
    }

    protected void updateUserLocation(LatLng userLocation) {
        ActionBar actionBar = getActionBar();
        if (actionBar != null &&
                (actionBar.getSubtitle() == null || actionBar.getSubtitle().length() == 0)) {
            new UpdateLocationTask(this, actionBar).execute(userLocation);
        }

        if (!refreshListingsIfNeeded(userLocation)) {
            updateListingForUserLocation(userLocation);
        }
    }

    private void updateListingForUserLocation(final LatLng userLocation) {
        // Sort the events based on popularity and distance from user location.
        mEventsListAdapter.sort(new EventComparator(userLocation));
    }

    protected boolean showLocationInActionBar() {
        return true;
    }

    // ***********************
    // Callbacks
    // ***********************

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            showEventDetails(mEventsListAdapter.getItem(position));
        }
    };
}
