package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.utils.EventListAdapter;
import com.eventshigh.nearme.app.utils.UpdateLocationTask;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListActivity extends LocationAwareEventActivity {
    private ListView mEventListView;
    private EventListAdapter mEventsListAdapter;
    private TextView mLocalityView;

    // ***********************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        // See if we have location passed to us within intent.
        Intent intent = getIntent();
        EventFetcherParam param = null;
        if (intent != null) {
            param = intent.getParcelableExtra(EXTRA_EVENT_FETCHER_PARAM);
        }

        // Setup the local member variables.
        setUpAll(param);

        // Setup adapter.
        mLocalityView = (TextView) findViewById(R.id.event_locality_header);
        mLocalityView.setOnClickListener(mLocalityClickListener);
        mEventListView = (ListView) findViewById(R.id.event_list);
        mEventsListAdapter = new EventListAdapter(this, true);
        mEventListView.setAdapter(mEventsListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_map) {
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
        // If event has e**N users going, we reduce 500*N meters from its distance.
        final Map<String, Double> eventToDistanceMap = new HashMap<String, Double>(mEventsListAdapter.getCount());
        mEventsListAdapter.sort(new Comparator<Event>() {
            @Override
            public int compare(Event lhs, Event rhs) {
                return Double.compare(
                        weightedDistance(lhs, userLocation, eventToDistanceMap),
                        weightedDistance(rhs, userLocation, eventToDistanceMap)
                );
            }
        });
    }

    // Find the distance of events from user's position with weight for popular events.
    // If event has e**N users going, we reduce 500*N meters from its distance.
    private static double weightedDistance(Event event, LatLng userLocation, Map<String, Double> eventToDistanceMap) {
        Double result = eventToDistanceMap.get(event.id);
        if (result != null) {
            return result;
        }

        double distance = SphericalUtil.computeDistanceBetween(event.location, userLocation);
        double weight = (event.numPeopleInterested > 0 ? Math.log(event.numPeopleInterested) * 500 : 0)
                + (event.ehRecommended ? 1000 : 0) ;
        double weightedDistance = distance - weight;
        eventToDistanceMap.put(event.id, weightedDistance);
        return weightedDistance;
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
}
