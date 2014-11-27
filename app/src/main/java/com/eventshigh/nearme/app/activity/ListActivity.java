package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.utils.DownloadImageTask;
import com.eventshigh.nearme.app.utils.UpdateLocationTask;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.maps.android.SphericalUtil;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListActivity extends LocationAwareEventActivity {
    // log tag used for debugging.
    private static final String LOG_TAG = MapsActivity.class.getSimpleName();

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
        ListView eventListView = (ListView) findViewById(R.id.event_list);
        mEventsListAdapter = new EventListAdapter();
        eventListView.setAdapter(mEventsListAdapter);
        eventListView.setOnItemClickListener(mOnItemClickListener);

        // Automatic Google Analytics reporting.
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(LOG_TAG)
                .setAction("getView")
                .setLabel("")
                .setValue(mEventsListAdapter.numViews)
                .build());
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_map) {
            if (lastEventFetcherParam != null) {
                startActivity(new Intent(this, MapsActivity.class)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
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
        mLocalityView.setText(Utils.capitalize(lastEventFetcherParam.city.toString()));
        new UpdateLocationTask(this, mLocalityView).execute(lastEventFetcherParam.location);

        // Sort the events based on popularity and distance from user location.
        // If event has e**N users going, we reduce 500*N meters from its distance.
        final Map<String, Double> eventToDistanceMap = new HashMap<String, Double>(events.size());
        Collections.sort(events, new Comparator<Event>() {
            @Override
            public int compare(Event lhs, Event rhs) {
                return Double.compare(
                        weightedDistance(lhs, eventToDistanceMap),
                        weightedDistance(rhs, eventToDistanceMap)
                );
            }
        });

        mEventsListAdapter.clear();
        mEventsListAdapter.addAll(events);
    }


    // Find the distance of events from user's position with weight for popular events.
    // If event has e**N users going, we reduce 500*N meters from its distance.
    private double weightedDistance(Event event, Map<String, Double> eventToDistanceMap) {
        Double result = eventToDistanceMap.get(event.id);
        if (result != null) {
            return result;
        }

        double distance = SphericalUtil.computeDistanceBetween(event.location,
                lastEventFetcherParam.location);
        double weight = (event.numPeopleInterested > 0 ? Math.log(event.numPeopleInterested) * 500 : 0)
                + (event.ehRecommended ? 1000 : 0) ;
        double weightedDistance = distance - weight;
        eventToDistanceMap.put(event.id, weightedDistance);
        return weightedDistance;
    }


    // ***********************
    // Callbacks
    // ***********************
    private class EventListAdapter extends ArrayAdapter<Event> {
        public int numViews = 0;

        private EventListAdapter() {
            super(ListActivity.this, R.layout.list_item_event, R.id.event_title);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            numViews ++;

            View view = convertView == null ?
                    getLayoutInflater().inflate(R.layout.list_item_event, parent, false) :
                    convertView;
            EventCard eventCard = new EventCard(view);

            Event event = getItem(position);

            eventCard.bgView.setImageResource(R.drawable.eh_default);
            new DownloadImageTask(eventCard.bgView).execute(event.img_url);
            eventCard.titleView.setText(event.title);
            eventCard.timeView.setText(Utils.getEventTime(event));
            eventCard.numPeopleInterestedView.setText(
                    Integer.toString(event.numPeopleInterested));
            if (event.locality == null) {
                eventCard.venueView.setVisibility(View.GONE);
            } else {
                eventCard.venueView.setVisibility(View.VISIBLE);
                eventCard.venueView.setText(event.locality);
            }

            return view;
        }
    }

    private OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(LOG_TAG)
                    .setAction("onItemClick")
                    .setLabel("")
                    .setValue(1)
                    .build());

            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                    getEventUri(mEventsListAdapter.getItem(position)));
            startActivity(browserIntent);
        }
    };

    private static class EventCard {
        private final ImageView bgView;
        private final TextView titleView;
        private final TextView venueView;
        private final TextView timeView;
        private final TextView numPeopleInterestedView;

        private EventCard(View cardView) {
            bgView = (ImageView) cardView.findViewById(R.id.event_bg);
            titleView = (TextView) cardView.findViewById(R.id.event_title);
            venueView = (TextView) cardView.findViewById(R.id.event_venue);
            timeView = (TextView) cardView.findViewById(R.id.event_time);
            numPeopleInterestedView = (TextView) cardView.findViewById(R.id.num_people_interested);
        }
    }

}
