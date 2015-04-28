package com.eventshigh.nearme.app.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Request.Priority;
import com.android.volley.Response.Listener;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.network.OffersRequest;
import com.eventshigh.nearme.app.task.FetchLocalityTask;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
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
    private boolean searchViewExpanded;

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
        eventGridView.setEventsAdapter(eventsAdapter);
        if (EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
            eventGridView.setSpacing((int) getResources().getDimension(R.dimen.small_card_spacing));
            eventGridView.setColumnWidth((int) getResources().getDimension(R.dimen.small_event_card_width));
        }

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
                !EventsHighEndpoints.isFeaturedEventQuery(eventsContext.query) &&
                eventGridView.getSpanCount() == 1;
        if (showFollowCard) {
            // Hide the regular toolbar and show the follow toolbar
            toolbar.setVisibility(View.GONE);
            toolbar = (Toolbar) findViewById(R.id.follow_toolbar);
            toolbar.setVisibility(View.VISIBLE);
            setSupportActionBar(toolbar);
            setTitle();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean returnValue = super.onCreateOptionsMenu(menu);
        MenuItem item = menu.findItem(R.id.action_search);
        MenuItemCompat.setOnActionExpandListener(item,
                new MenuItemCompat.OnActionExpandListener() {
                    int oldToolbarAlpha;

                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        searchViewExpanded = true;
                        oldToolbarAlpha = currentToolBarAlpha;
                        updateToolbar(255);
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        searchViewExpanded = false;
                        updateToolbar(oldToolbarAlpha);
                        return true;
                    }
                });

        return returnValue;
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
            updateToolbar(0);
            eventsAdapter.addFollowCard(eventsContext.query, events.size());
            eventGridView.setOnScrollListener(new OnScrollListener() {
                private int y;

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    // do nothings.
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    y += dy;
                    if (!searchViewExpanded) {
                        updateToolbar(Math.min(y, 255));
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
        eventsAdapter.setMyEvents(myEvents, eventGridView.getSpanCount() * 2);
    }

    @Override
    protected void updateUserLocation(@Nullable LatLng userLocation) {
        if (userLocation != null) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null &&
                (actionBar.getSubtitle() == null || actionBar.getSubtitle().length() == 0)) {
                new FetchLocalityTask(this, this).execute(userLocation);
            }
        }

        super.updateUserLocation(userLocation);
    }

    protected  int getDisabledMenuItem() {
        return R.id.action_show_list;
    }

    private int currentToolBarAlpha = 255;
    private void updateToolbar(int toolbarAlpha) {
        if (toolbarAlpha == currentToolBarAlpha) {
            // do nothing
            return;
        }

        // Change the color of toolbar icons and text if needed.
        if (toolbarAlpha < 100) {
            if (currentToolBarAlpha >= 100) {
                setDarkToolbarIcons();
            }
        } else {
            if (currentToolBarAlpha < 100) {
                setLightToolbarIcons();
            }
        }

        currentToolBarAlpha = toolbarAlpha;
        toolbar.setBackgroundColor(Color.argb(toolbarAlpha, 0x15, 0xA9, 0xD1));
        toolbar.setTitleTextColor(Color.argb(toolbarAlpha, 255, 255, 255));
        toolbar.setSubtitleTextColor(Color.argb(toolbarAlpha, 255, 255, 255));
    }

    private void setDarkToolbarIcons() {
        toolbar.post(new Runnable() {
            @Override
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.black), EventsGridActivity.this);
            }
        });
    }

    private void setLightToolbarIcons() {
        toolbar.post(new Runnable() {
            @Override
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.white), EventsGridActivity.this);
            }
        });
    }
}
