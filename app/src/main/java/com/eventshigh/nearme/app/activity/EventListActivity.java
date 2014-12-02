package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.util.Log;
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
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
            mEventListFragment.setActivateOnItemClick(true);
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

        // Automatic Google Analytics reporting.
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
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
        mEventListFragment.updateListingForUserLocation(
                lastEventFetcherParam == null ? null : lastEventFetcherParam.location);
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
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(LOG_TAG)
                    .setAction("LocalityClickListener")
                    .setLabel("")
                    .setValue(1)
                    .build());

            askUserForLocation();
        }
    };

    /**
     * Callback method from {@link EventListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(Event event) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putParcelable(EventDetailFragment.ARG_ITEM_ID, event);
            EventDetailFragment fragment = new EventDetailFragment();
            fragment.setArguments(arguments);
            getFragmentManager().beginTransaction()
                    .replace(R.id.event_detail_container, fragment)
                    .commit();
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, EventDetailActivity.class);
            detailIntent.putExtra(EventDetailFragment.ARG_ITEM_ID, event);
            startActivity(detailIntent);
        }
    }

    @Override
    public void shareEvent(View eventView, Event event) {
        View shareView = eventView.findViewById(R.id.event_share);
        shareView.setVisibility(View.INVISIBLE);
        shareEvent(this, eventView, event);
        shareView.setVisibility(View.VISIBLE);
    }

    public static void shareEvent(Activity activity, View eventView, Event event) {
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(activity.getClass().getSimpleName())
                .setAction("shareEvent")
                .setLabel("")
                .setValue(1)
                .build());

        eventView.setDrawingCacheEnabled(true);
        Bitmap bitmap = eventView.getDrawingCache();

        try {
            File file = File.createTempFile("event", ".jpg", activity.getCacheDir());
            FileOutputStream oStream = new FileOutputStream(file);
            bitmap.compress(CompressFormat.JPEG, 90, oStream);
            oStream.close();

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(activity,
                            "com.eventshigh.nearme.app.fileprovider", file));
            sendIntent.putExtra(Intent.EXTRA_TITLE, event.title);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    event.getEventDetailsURI() + " (shared via EventsHigh)");
            sendIntent.setType("image/jpeg");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(sendIntent);
        } catch (IOException e) {
            Toast.makeText(activity, R.string.failed_save, Toast.LENGTH_SHORT).show();
            Log.w(LOG_TAG, "failed to create file for sharing", e);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.failed_share, Toast.LENGTH_SHORT).show();
            Log.w(LOG_TAG, "failed sharing", e);
        }
    }

}
