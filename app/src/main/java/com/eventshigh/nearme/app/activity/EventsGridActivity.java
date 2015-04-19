package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toolbar;

import com.android.volley.Request.Priority;
import com.android.volley.Response.Listener;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.network.OffersRequest;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * An {@link com.eventshigh.nearme.app.activity.BaseEventsActivity} which shows the events in Grid.
 * On Phone, we have one column in portrait mode and two columns in landscape mode. On Tablet,
 * we try to put more columns as per the width offered.
 */
public class EventsGridActivity extends BaseEventsActivity {

    private SwipeRefreshLayout swipeRefreshLayout;
    private AutofitRecyclerView eventGridView;
    private EventsAdapter eventsAdapter;

    // ***********************
    // Delegated Methods from {@link BaseEventsActivity}
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the UI.
        View view = getLayoutInflater().inflate(R.layout.activity_event_grid, eventContainer, false);
        eventContainer.addView(view, 0);

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

        setupScrollListener();
    }

    private void setupScrollListener() {
        final View followWidget = findViewById(R.id.follow_widget);
        final TextView followWidgetTitle = (TextView) findViewById(R.id.follow_title);
        eventGridView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // Move big toolbar up
                final int titleOffsetY = (toolbar.getBottom() - followWidgetTitle.getBottom()
                    - Utils.dpToPx(EventsGridActivity.this, 16));
                float newY = followWidget.getY() - dy;
                followWidget.setY(newY);

                // Move title to the left.
                ActionBar actionBar = getSupportActionBar();
                if (newY > titleOffsetY) {
                    float toolbarTitleX = Utils.dpToPx(EventsGridActivity.this, 60);
                    float bigToolbarTitleX = followWidgetTitle.getLeft();
                    float titleOffsetX = bigToolbarTitleX - toolbarTitleX;
                    toolbarTitleX = toolbarTitleX + titleOffsetX * (titleOffsetY - newY) / titleOffsetY;
                    followWidgetTitle.setX(toolbarTitleX);
                    actionBar.setTitle("");
                    followWidgetTitle.setVisibility(View.VISIBLE);
                } else {
                    actionBar.setTitle(DateTimeUtils.queryToTitle(eventsContext.query));
                    followWidgetTitle.setVisibility(View.INVISIBLE);
                }

                System.out.println("----------> " + titleOffsetY + "   " + newY + "   ");
            }
        });
    }

    @Override
    protected void updateContentViewLayout(int top) {
        eventGridView.setPadding(eventGridView.getPaddingLeft(), top,
            eventGridView.getPaddingRight(), eventGridView.getPaddingBottom());
        swipeRefreshLayout.setProgressViewOffset(false, top - Utils.dpToPx(this, 30),
            top + Utils.dpToPx(this, 30));
        eventGridView.smoothScrollToPosition(0);
    }

    @Override
    protected boolean shouldIncludeWithoutLocation() {
        return true;
    }

    @Override
    protected void updateEventsCollection(List<Event> events) {
        super.updateEventsCollection(events);
        eventsAdapter.setEvents(events);
        eventGridView.smoothScrollToPosition(0);
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
            if (actionBar.getSubtitle() == null || actionBar.getSubtitle().length() == 0) {
                //new ShowLocalityTask(this, actionBar).execute(userLocation);
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
