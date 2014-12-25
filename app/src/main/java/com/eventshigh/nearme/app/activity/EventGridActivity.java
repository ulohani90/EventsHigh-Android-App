package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.EventComparator;
import com.eventshigh.nearme.app.utils.EventListAdapter;
import com.eventshigh.nearme.app.utils.UpdateLocationTask;
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

        // Show the Up button in the action bar.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(LaunchActivity.isMapsViewDefault(this));
        }

        // Setup adapter.
        GridView eventGridView = (GridView) findViewById(R.id.event_grid);
        mEventsListAdapter = new EventListAdapter(this);
        eventGridView.setAdapter(mEventsListAdapter);
        eventGridView.setOnItemClickListener(mOnItemClickListener);

        // Setup the local member variables.
        setUpAll();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        if (id == R.id.action_map) {
            switchTo(MapsActivity.class);
            return true;
        }

        if (id == R.id.action_change_location) {
            askUserForLocation(getActionBar());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // ***********************
    // Helper Methods
    // ***********************

    @Override
    protected void updateNewEvents(List<Event> events) {
        mEventsListAdapter.clear();
        mEventsListAdapter.addAll(events);
        mEventsListAdapter.setShowDate(!lastEventFetcherParam.query.isEmpty());
        updateListingForUserLocation(
                lastEventFetcherParam.location == null ? null : lastEventFetcherParam.location);

        super.updateNewEvents(events);
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
