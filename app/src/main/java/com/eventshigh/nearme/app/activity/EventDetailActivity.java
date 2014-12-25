package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsFetcher;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * An activity representing a single Event detail screen.
 */
public class EventDetailActivity extends BaseActivity {
    private static final String LOG_TAG = EventDetailActivity.class.getSimpleName();

    private ActionBar actionBar;

    /**********************************
     Activity lifecycle management utilities
     **********************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Show the Up button in the action bar.
        actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Get the event from Intent.
        Event event = getIntent().getParcelableExtra(EventDetailFragment.ARG_EVENT_INFO);
        if (event != null) {
            showEventFragment(event);
            return;
        }

        onNewIntent(getIntent());
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
            NavUtils.navigateUpTo(this, new Intent(this, EventGridActivity.class));
            return true;
        }

        return  false;
    }

    @Override
    protected void onNewIntent(@Nullable Intent intent) {
        if (intent == null) {
            finish();
            return;
        }

        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            reportActionToAnalytics("deepLink", "detail");
            new EventFetcher(this).execute(intent.getData());
        }
    }

    private void showEventFragment(Event event) {
        if (actionBar != null) {
            actionBar.setTitle(event.title);
        }

        FragmentTransaction tx = getFragmentManager().beginTransaction();
        tx.replace(R.id.container, EventDetailFragment.newInstance(event));
        tx.commit();
    }

    public static class EventFetcher extends AsyncTask<Uri, Void, Event> {
        private final EventDetailActivity activity;

        public EventFetcher(EventDetailActivity activity) {
            this.activity = activity;
        }

        @Override
        protected Event doInBackground(Uri... params) {
            Uri eventUri = params[0];
            List<String> eventUriPathSegments = eventUri.getPathSegments();

            if (eventUriPathSegments.size() < 2) {
                Log.w(LOG_TAG, "Invalid data: " + eventUri);
                return null;
            }

            City city;
            try {
                city = City.valueOf(
                        eventUriPathSegments.get(eventUriPathSegments.size() - 2).toUpperCase());
            } catch (IllegalArgumentException e) {
                Log.w(LOG_TAG, "Invalid data, no city: " + eventUri, e);
                return null;
            }

            String eventId = eventUriPathSegments.get(eventUriPathSegments.size() - 1).split("-", 2)[0];
            String url = EventsFetcher.API_ENDPOINT_EVENT.replace("EVENT_ID", eventId);
            try {
                JSONObject eventJSON = Utils.fetchJSON(url);
                return Event.fromJSON(city, eventJSON);
            } catch (IOException | JSONException | ParseException e) {
                Log.w(LOG_TAG, "Failed to fetch: " + url, e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(@Nullable Event result) {
            super.onPostExecute(result);

            if (result == null) {
                // Failed. Show toast and return empty list.
                Toast.makeText(activity, R.string.failed_load, Toast.LENGTH_SHORT).show();
                activity.finish();
                return;
            }

            activity.showEventFragment(result);
        }
    }
}
