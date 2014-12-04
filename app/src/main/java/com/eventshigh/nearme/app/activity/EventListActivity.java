package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventFetcherParam;
import com.eventshigh.nearme.app.utils.UpdateLocationTask;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * An activity representing a list of Events. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link EventDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link EventListFragment} and the item details
 * (if present) is a {@link EventDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link EventListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class EventListActivity extends LocationAwareEventActivity
        implements EventListFragment.Callbacks {
    // log tag used for debugging.
    private static final String LOG_TAG = EventListActivity.class.getSimpleName();

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private boolean mTwoPane;
    private TextView mLocalityView;
    private EventListFragment mEventListFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_list);

        // Show the Up button in the action bar.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // See if we are in two pane mode.
        mEventListFragment = ((EventListFragment) getFragmentManager()
                .findFragmentById(R.id.event_list));
        if (findViewById(R.id.event_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            mEventListFragment.setIsTwoPane(true);
        }

        // See if we have location passed to us within intent.
        Intent intent = getIntent();
        EventFetcherParam param = null;
        if (intent != null) {
            param = intent.getParcelableExtra(EXTRA_EVENT_FETCHER_PARAM);
        }

        // Setup the local member variables.
        setUpAll(param);

        // Setup locality click listener.
        mLocalityView = (TextView) findViewById(R.id.event_locality_header);
        mLocalityView.setOnClickListener(mLocalityClickListener);
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

        mEventListFragment.updateNewEvents(events);
        if (lastEventFetcherParam != null) {
            mEventListFragment.updateListingForUserLocation(lastEventFetcherParam.location);
        }
    }

    protected void updateUserLocation(LatLng userLocation) {
        if (!refreshListingsIfNeeded(userLocation)) {
            mEventListFragment.updateListingForUserLocation(userLocation);
        }
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

    /**
     * Callback method from {@link EventListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(Event event) {
        reportActionToAnalytics("onItemSelected");

        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putParcelable(EventDetailFragment.ARG_EVENT_INFO, event);
            EventDetailFragment fragment = new EventDetailFragment();
            fragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .replace(R.id.event_detail_container, fragment)
                    .commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            showEventDetails(event);
        }
    }
}
