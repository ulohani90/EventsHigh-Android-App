package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsCollection;
import com.eventshigh.nearme.app.task.ShowLocalityTask;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.google.android.gms.maps.model.LatLng;

/**
 * An {@link com.eventshigh.nearme.app.activity.BaseEventsActivity} which shows the events in Grid.
 * On Phone, we have one column in portrait mode and two columns in landscape mode. On Tablet,
 * we try to put more columns as per the width offered.
 */
public class EventsGridActivity extends BaseEventsActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private EventsAdapter eventsAdapter;


    // ***********************
    // Delegated Methods from {@link BaseEventsActivity}
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the UI.
        getLayoutInflater().inflate(R.layout.fragment_event_grid, eventContainer);

        eventsAdapter = new EventsAdapter(this);
        RecyclerView eventGridView = (RecyclerView) findViewById(R.id.event_grid);
        eventGridView.setAdapter(eventsAdapter);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reportActionToAnalytics("swipeRefresh");
                swipeRefreshLayout.setRefreshing(false);
                fetchNewListing(true /* bypass cache*/);
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.app_color);
    }

    @Override
    protected void onResume() {
        super.onResume();

        eventsAdapter.removeDismissedEvents(eventsMarkerEditor.getEventsMarkerManager());
        eventsAdapter.notifyDataSetChanged();
    }

    @Override
    protected boolean shouldIncludeWithoutLocation() {
        return true;
    }

    @Override
    protected void updateEventsCollection(EventsCollection events) {
        super.updateEventsCollection(events);
        eventsAdapter.setEvents(events);
    }

    @Override
    protected void updateUserLocation(@Nullable LatLng userLocation) {
        if (userLocation != null) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar.getSubtitle() == null || actionBar.getSubtitle().length() == 0) {
                new ShowLocalityTask(this, actionBar).execute(userLocation);
            }
        }

        super.updateUserLocation(userLocation);
    }

    @Override
    protected void remove(Event event) {
        eventsAdapter.removeEvent(event);
    }


    // ***********************
    // Callbacks
    // ***********************

    // Called when fab icon is pressed
    public void onSwitchView(View view) {
        switchTo(EventsMapsActivity.class);
    }
}
