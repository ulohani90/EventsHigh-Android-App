package com.eventshigh.nearme.app.activity;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.EventDetailFragment.MyScrollListener;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.network.EventRequest;

/**
 * An activity representing a single Event detail screen. This activity can be called from deep
 * link or from Events{Grid,Maps}Activity. In both cases, event data is not available so
 * this activity fetches the event data and shows it using the EventDetailFragment.
 */
public class EventDetailActivity extends BaseActivity implements MyScrollListener {

    private FrameLayout container;
    private Toolbar toolbar;

    /**********************************
     Activity lifecycle management utilities
     **********************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_event_detail);
        container = (FrameLayout) findViewById(R.id.container);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        toolbar.setNavigationOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateUp();
            }
        });

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        container.removeAllViews();
        toolbar.setTitle(getString(R.string.loading));
        toolbar.setAlpha(1.0f);
        getLayoutInflater().inflate(R.layout.view_loading, container);

        boolean showEvent = false;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            reportActionToAnalytics("deepLink", "detail");
            showEvent = true;
        }

        if (BaseActivity.NOTIFICATION_ACTION.equals(action)) {
            reportActionToAnalytics("openNotification", intent.getDataString());
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
                    mEventListener, new ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            Toast.makeText(EventDetailActivity.this, R.string.failed_load, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
        } else {
            Toast.makeText(this, "No event to show!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void setScroll(int scrollValue) {
        float opacity = Math.min(1.0f, scrollValue * 3f / getResources().getDisplayMetrics().heightPixels);
        toolbar.setAlpha(opacity);
    }

    private Listener<Event> mEventListener = new Listener<Event>() {
        @Override
        public void onResponse(Event event, boolean isIntermediate) {
            toolbar.setTitle(event.title);
            toolbar.setAlpha(0f);

            FragmentTransaction tx = getFragmentManager().beginTransaction();
            tx.replace(R.id.container, EventDetailFragment.newInstance(event));
            tx.commit();
        }
    };

}

