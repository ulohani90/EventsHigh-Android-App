package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.utils.EventComparator;
import com.eventshigh.nearme.app.utils.EventListAdapter;
import com.eventshigh.nearme.app.utils.UpdateLocationTask;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * An {@link android.app.Activity} which shows the events in Grid. On Phone,
 * we have one column in portrait mode and two columns in landscape mode.
 * On Tablet, we try to put more columns as per the width offered.
 */
public class EventGridActivity extends LocationAwareEventActivity {
    private EventListAdapter mEventsListAdapter;
    private TextView mLocalityView;

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
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // See if we have location passed to us within intent.
        Intent intent = getIntent();
        EventFetcherParam param = null;
        if (intent != null) {
            param = intent.getParcelableExtra(EXTRA_EVENT_FETCHER_PARAM);
        }

        // Setup the local member variables.
        setUpAll(param);

        // Setup the locality view.
        mLocalityView = (TextView) findViewById(R.id.event_locality_header);
        mLocalityView.setOnClickListener(mLocalityClickListener);

        // Setup adapter.
        GridView eventGridView = (GridView) findViewById(R.id.event_grid);
        mEventsListAdapter = new EventListAdapter(this);
        eventGridView.setAdapter(mEventsListAdapter);
        eventGridView.setOnItemClickListener(mOnItemClickListener);
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
            if (lastEventFetcherParam != null) {
                startActivity(new Intent(this, MapsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(EXTRA_EVENT_FETCHER_PARAM, lastEventFetcherParam)
                );
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // ***********************
    // Helper Methods
    // ***********************

    @Override
    protected void updateNewEvents(List<Event> events) {
        // Show the location.
        if (lastEventFetcherParam != null) {
            mLocalityView.setText(Utils.capitalize(lastEventFetcherParam.city.toString()));
            new UpdateLocationTask(this, mLocalityView).execute(lastEventFetcherParam.location);
        } else {
            Toast.makeText(this, R.string.failed, Toast.LENGTH_SHORT).show();
        }

        mEventsListAdapter.clear();
        mEventsListAdapter.addAll(events);
        updateListingForUserLocation(
                lastEventFetcherParam == null ? null : lastEventFetcherParam.location);
    }

    protected void updateUserLocation(LatLng userLocation) {
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

    private final OnClickListener mLocalityClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            askUserForLocation(mLocalityView);
        }
    };

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            showEventDetails(mEventsListAdapter.getItem(position));
        }
    };
}
