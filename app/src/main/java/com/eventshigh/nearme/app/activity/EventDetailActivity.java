package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.task.SingleEventFetcherTask;
import com.eventshigh.nearme.app.task.SingleEventFetcherTask.OnEventFetchHandler;
import com.eventshigh.nearme.app.data.Event;

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
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            reportActionToAnalytics("deepLink", "detail");
            new SingleEventFetcherTask(this, new OnEventFetchHandler() {
                @Override
                public void onEventFetch(Event event) {
                    showEventFragment(event);
                }
            }).execute(intent.getData());
            return;
        }

        Toast.makeText(this, "No event to show!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showEventFragment(Event event) {
        if (actionBar != null) {
            actionBar.setTitle(event.title);
        }

        FragmentTransaction tx = getFragmentManager().beginTransaction();
        tx.replace(R.id.container, EventDetailFragment.newInstance(event));
        tx.commit();
    }

}
