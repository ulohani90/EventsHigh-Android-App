package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.android.volley.Request.Priority;
import com.android.volley.Response.Listener;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.network.EventRequest;

/**
 * An activity representing a single Event detail screen. This activity can be called from deep
 * link or from Events{Grid,Maps}Activity. In both cases, event data is not available so
 * this activity fetches the event data and shows it using the EventDetailFragment.
 */
public class EventDetailActivity extends BaseActivity {
    private ActionBar actionBar;

    /**********************************
     Activity lifecycle management utilities
     **********************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Show the Up button in the action bar.
        actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setContentView(R.layout.activity_event_detail);

        String action = intent.getAction();

        boolean showEvent = false;
        if (Intent.ACTION_VIEW.equals(action)) {
            reportActionToAnalytics("deepLink", "detail");
            showEvent = true;
        }

        if (NOTIFICATION_ACTION.equals(action)) {
            reportActionToAnalytics("openNotification");
            showEvent = true;
        }

        // Get the event from Intent.
        Event event = intent.getParcelableExtra(EventDetailFragment.ARG_EVENT_INFO);
        if (event != null) {
            showEvent = true;
            intent.setData(event.getEventDetailsURI());
        }

        if (showEvent) {
            EventRequest.submit(getApplicationContext(), intent.getData(), Priority.IMMEDIATE, this,
                    mEventListener, mErrorListener);
        } else {
            Toast.makeText(this, "No event to show!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private Listener<Event> mEventListener = new Listener<Event>() {
        @Override
        public void onResponse(Event event) {
            if (actionBar != null) {
                actionBar.setTitle(event.title);
            }

            FragmentTransaction tx = getFragmentManager().beginTransaction();
            tx.replace(R.id.container, EventDetailFragment.newInstance(event));
            tx.commit();
        }
    };
}
