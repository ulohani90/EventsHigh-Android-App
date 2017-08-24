package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.network.FetchUserFavouritesRequest;
import com.eventshigh.nearme.app.network.FetchUserInterestsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

/**
 * Created by umesh on 23/08/17.
 */

public class MyFavouritesActivity extends BaseContextActivity {

    View progressBar;
    View noEventLayout;
    AutofitRecyclerView recyclerView;
    EventsAdapter mAdapter;
    String profileId;
    View retryView;
    TextView noMyEventHeading;
    TextView exploreEvents;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recyclerview_with_progress_layout);
        eventsContext = getIntent().getParcelableExtra("events_context");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Favourites");
        profileId = getIntent().getStringExtra("profile_id");
        progressBar = findViewById(R.id.top_progress_bar);
        noEventLayout = findViewById(R.id.view_no_my_event);
        retryView = findViewById(R.id.view_retry);
        retryView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUserFavourites(true);
            }
        });
        noMyEventHeading = (TextView) findViewById(R.id.no_my_event_heading);
        noMyEventHeading.setText(getResources().getString(R.string.ui_no_my_event));
        recyclerView = (AutofitRecyclerView) findViewById(R.id.event_grid);
        exploreEvents = (TextView) findViewById(R.id.explore_events);
        exploreEvents.setText("Explore Events");
        exploreEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchView("today");
            }
        });
        mAdapter = new EventsAdapter(this);
        recyclerView.setAdapter(mAdapter);
        loadUserFavourites(true);

    }
    public void onRetry(View view) {
        loadUserFavourites(true);
    }

    private void loadUserFavourites(boolean shouldByPassChange) {
        progressBar.setVisibility(View.VISIBLE);
        retryView.setVisibility(View.GONE);
        FetchUserFavouritesRequest.submit(this, profileId, Request.Priority.HIGH,
                new Response.Listener<List<Event>>() {
                    @Override
                    public void onResponse(List<Event> topicEvents, boolean b) {

                        if (topicEvents != null && topicEvents.size() > 0) {
                            //mAdapter.setTopicEvents(topicEvents, eventsContext, recyclerView.getSpanCount() * 2);
                            mAdapter.setEvents(topicEvents, null, false);
                            recyclerView.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                            noEventLayout.setVisibility(View.GONE);
                            retryView.setVisibility(View.GONE);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            noEventLayout.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            retryView.setVisibility(View.GONE);
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        progressBar.setVisibility(View.GONE);
                        noEventLayout.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.GONE);
                        retryView.setVisibility(View.VISIBLE);
                    }
                }, shouldByPassChange);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();

        }
        return super.onOptionsItemSelected(item);
    }
}
