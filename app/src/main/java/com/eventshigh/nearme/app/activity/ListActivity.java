package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.utils.DownloadImageTask;
import com.eventshigh.nearme.app.utils.LocationPickerDialog;
import com.eventshigh.nearme.app.utils.LocationPickerDialog.OnLocationSelection;
import com.eventshigh.nearme.app.utils.UpdateLocationTask;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListActivity extends LocationAwareEventActivity {
    // log tag used for debugging.
    private static final String LOG_TAG = ListActivity.class.getSimpleName();

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
        mEventsListAdapter = new EventListAdapter();
        mEventListView.setAdapter(mEventsListAdapter);

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

    private void showEventDetails(Event event) {
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(LOG_TAG)
                .setAction("showEventDetails")
                .setLabel("")
                .setValue(1)
                .build());

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, getEventUri(event));
        startActivity(browserIntent);
    }

    private void shareEvent(Event event) {
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(LOG_TAG)
                .setAction("shareEvent")
                .setLabel("")
                .setValue(1)
                .build());

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                event.title + " | " + getEventUri(event) + " | EventsHigh");
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }


    // ***********************
    // Callbacks
    // ***********************

    private final OnClickListener mLocalityClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            LocationPickerDialog.show(ListActivity.this, new OnLocationSelection() {
                @Override
                public void onLocationSelection(String locationString, LatLng locationPoint) {
                    mLocalityView.setText(locationString);
                    if (!refreshListingsIfNeeded(locationPoint)) {
                        updateListingForUserLocation(locationPoint);
                    }
                }
            });
        }
    };

    private class EventListAdapter extends ArrayAdapter<Event> {
        public int numViews = 0;

        private EventListAdapter() {
            super(ListActivity.this, R.layout.list_item_event, R.id.event_title);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            numViews ++;

            // Build the view, reuse existing if possible.
            View view = convertView == null ?
                    getLayoutInflater().inflate(R.layout.list_item_event, parent, false) :
                    convertView;
            EventCard eventCard = new EventCard(view);
            final Event event = getItem(position);

            // Set the background image.
            eventCard.bgView.setImageResource(R.drawable.eh_default);
            eventCard.bgView.setLayoutParams(new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    mEventListView.getHeight() / 2));
            new DownloadImageTask(eventCard.bgView).execute(event.img_url);

            // Set the title, time etc.
            eventCard.titleView.setText(event.title);
            eventCard.timeView.setText(Utils.getEventTime(event));
            eventCard.numPeopleInterestedView.setText(
                    Integer.toString(event.numPeopleInterested));

            // Set the locality
            if (event.locality == null) {
                eventCard.venueView.setVisibility(View.INVISIBLE);
            } else {
                eventCard.venueView.setVisibility(View.VISIBLE);
                eventCard.venueView.setText(event.locality);
            }

            // Check if its recommended event.
            eventCard.recommendedImageView.setVisibility(event.ehRecommended ? View.VISIBLE :
                    View.INVISIBLE);

            // Set the click listener.
            eventCard.bgView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showEventDetails(event);
                }
            });
            eventCard.shareView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareEvent(event);
                }
            });

            return view;
        }
    }

    private static class EventCard {
        private final ImageView bgView;
        private final ImageView shareView;
        private final ImageView recommendedImageView;
        private final TextView titleView;
        private final TextView venueView;
        private final TextView timeView;
        private final TextView numPeopleInterestedView;

        private EventCard(View cardView) {
            bgView = (ImageView) cardView.findViewById(R.id.event_bg);
            shareView = (ImageView) cardView.findViewById(R.id.event_share);
            recommendedImageView = (ImageView) cardView.findViewById(R.id.event_recommended);
            titleView = (TextView) cardView.findViewById(R.id.event_title);
            venueView = (TextView) cardView.findViewById(R.id.event_venue);
            timeView = (TextView) cardView.findViewById(R.id.event_time);
            numPeopleInterestedView = (TextView) cardView.findViewById(R.id.num_people_interested);
        }
    }

}
