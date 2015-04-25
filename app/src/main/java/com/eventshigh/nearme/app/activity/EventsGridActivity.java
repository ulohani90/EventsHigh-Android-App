package com.eventshigh.nearme.app.activity;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.android.volley.Request.Priority;
import com.android.volley.Response.Listener;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.network.OffersRequest;
import com.eventshigh.nearme.app.task.ShowLocalityTask;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * An {@link com.eventshigh.nearme.app.activity.BaseEventsActivity} which shows the events in Grid.
 * On Phone, we have one column in portrait mode and two columns in landscape mode. On Tablet,
 * we try to put more columns as per the width offered.
 */
public class EventsGridActivity extends BaseEventsActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private AutofitRecyclerView eventGridView;
    private EventsAdapter eventsAdapter;
    private boolean showFollowCard;

    // ***********************
    // Delegated Methods from {@link BaseEventsActivity}
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the UI.
        getLayoutInflater().inflate(R.layout.activity_event_grid, eventContainer);

        eventsAdapter = new EventsAdapter(this);
        eventGridView = (AutofitRecyclerView) findViewById(R.id.event_grid);
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
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);

        // Should we show follow widget?
        showFollowCard = !eventsContext.query.isEmpty() &&
                !EventsHighEndpoints.isDateQuery(eventsContext.query) &&
                !EventsHighEndpoints.isMyEventQuery(eventsContext.query) &&
                !EventsHighEndpoints.isFeaturedEventQuery(eventsContext.query);

        if (showFollowCard) {
            // Hide the regular toolbar and show the follow toolbar
            toolbar.setVisibility(View.GONE);
            toolbar = (Toolbar) findViewById(R.id.follow_toolbar);
            setSupportActionBar(toolbar);
            ToolbarColorizeHelper.colorizeToolbar(toolbar,
                getResources().getColor(android.R.color.black), this);
            clearTitleSubTitle();
            toolbar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected boolean shouldIncludeWithoutLocation() {
        return true;
    }

    @Override
    protected void updateEventsCollection(List<Event> events) {
        super.updateEventsCollection(events);
        eventsAdapter.setEvents(events);
        if (showFollowCard) {
            eventsAdapter.addFollowCard(eventsContext.query);

            final int invisibleAt = Utils.dpToPx(this, 150);
            final int visibleAt = Utils.dpToPx(this, 250);
            final View followToolbarBackground = findViewById(R.id.follow_toolbar_background);
            followToolbarBackground.setAlpha(0);
            followToolbarBackground.setVisibility(View.VISIBLE);

            eventGridView.setOnScrollListener(new OnScrollListener() {
                private int y;

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    // do nothings.
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    y += dy;
                    float alpha = ((float) y - invisibleAt) / (visibleAt - invisibleAt);
                    if (alpha < 0) {
                        alpha = 0;
                    } else if (alpha > 1) {
                        alpha = 1;
                    }
                    followToolbarBackground.setAlpha(alpha);

                    // Change the color of toolbar icons and text
                    if (alpha < 0.5) {
                        ToolbarColorizeHelper.colorizeToolbar(toolbar,
                            getResources().getColor(android.R.color.black),
                            EventsGridActivity.this);
                        clearTitleSubTitle();
                    } else {
                        ToolbarColorizeHelper.colorizeToolbar(toolbar,
                            getResources().getColor(android.R.color.white),
                            EventsGridActivity.this);
                        setTitle();
                    }
                }
            });
        }

        eventGridView.scrollToPosition(0);
        OffersRequest.submit(this, Priority.NORMAL, new Listener<Offer>() {
            @Override
            public void onResponse(Offer offer, boolean isIntermediate) {
                eventsAdapter.addOffer(offer);
            }
        });
    }

    @Override
    protected void updateMyEvents(MyEvents myEvents) {
        super.updateMyEvents(myEvents);
        eventsAdapter.setMyEvents(myEvents);
    }

    @Override
    protected void updateUserLocation(@Nullable LatLng userLocation) {
        if (userLocation != null) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null &&
                (actionBar.getSubtitle() == null || actionBar.getSubtitle().length() == 0)) {
                new ShowLocalityTask(this, actionBar).execute(userLocation);
            }
        }

        super.updateUserLocation(userLocation);
    }

    protected  int getDisabledMenuItem() {
        return R.id.action_show_list;
    }
}
