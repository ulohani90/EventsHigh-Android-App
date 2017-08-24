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
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.network.FetchUserFavouritesRequest;
import com.eventshigh.nearme.app.network.FetchUserReviewsRequest;
import com.eventshigh.nearme.app.network.MultiEventsRequest;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 23/08/17.
 */

public class MyReviewsActivity extends BaseContextActivity {

    View progressBar;
    View noEventLayout;
    AutofitRecyclerView recyclerView;
    EventsAdapter mAdapter;
    String profileId;
    View retryView;
    TextView noMyEventHeading;
    TextView exploreEvents;
    TextView noMyReviewsText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recyclerview_with_progress_layout);
        eventsContext = getIntent().getParcelableExtra("events_context");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Reviews");
        profileId = getIntent().getStringExtra("profile_id");
        progressBar = findViewById(R.id.top_progress_bar);
        noEventLayout = findViewById(R.id.view_no_my_event);
        retryView = findViewById(R.id.view_retry);
        noMyReviewsText = (TextView) findViewById(R.id.no_tickets_view);
        noMyReviewsText.setText(getString(R.string.no_review_text));
        retryView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUserReviews(true);
            }
        });
        noMyEventHeading = (TextView) findViewById(R.id.no_my_event_heading);
        noMyEventHeading.setText(getResources().getString(R.string.ui_no_my_event));
        recyclerView = (AutofitRecyclerView) findViewById(R.id.event_grid);
        exploreEvents = (TextView) findViewById(R.id.explore_events);
        exploreEvents.setText("Explore Events");
        mAdapter = new EventsAdapter(this);
        recyclerView.setAdapter(mAdapter);
        loadUserReviews(true);

    }

    public void onRetry(View view) {
        loadUserReviews(true);
    }

    private void loadUserReviews(final boolean shouldByPassChange) {
        progressBar.setVisibility(View.VISIBLE);
        retryView.setVisibility(View.GONE);
        FetchUserReviewsRequest.submit(this, profileId, Request.Priority.HIGH,
                new Response.Listener<List<MovieUserReviewObject>>() {
                    @Override
                    public void onResponse(List<MovieUserReviewObject> reviews, boolean b) {

                        if (reviews != null && reviews.size() > 0) {
                            //mAdapter.setTopicEvents(topicEvents, eventsContext, recyclerView.getSpanCount() * 2);
                            fetchDetailedInfo(shouldByPassChange, reviews);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            noEventLayout.setVisibility(View.GONE);
                            noMyReviewsText.setVisibility(View.VISIBLE);
                            retryView.setVisibility(View.GONE);
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        progressBar.setVisibility(View.GONE);
                        noEventLayout.setVisibility(View.GONE);
                        noMyReviewsText.setVisibility(View.GONE);
                        retryView.setVisibility(View.VISIBLE);
                    }
                }, shouldByPassChange);

    }

    private void fetchDetailedInfo(boolean shouldBypassCache, final List<MovieUserReviewObject> movieUserReviewObjectList) {
        MultiEventsRequest.submit(this, eventsContext, getEvents(movieUserReviewObjectList),
                Request.Priority.HIGH, null, shouldBypassCache, true, false, new Response.Listener<List<Event>>() {
                    @Override
                    public void onResponse(List<Event> events, boolean b) {
                        updateEvents(events, movieUserReviewObjectList);
                        progressBar.setVisibility(View.GONE);
                        noEventLayout.setVisibility(View.GONE);
                        noMyReviewsText.setVisibility(View.GONE);
                        retryView.setVisibility(View.GONE);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        progressBar.setVisibility(View.GONE);
                        noEventLayout.setVisibility(View.GONE);
                        noMyReviewsText.setVisibility(View.GONE);
                        retryView.setVisibility(View.VISIBLE);
                    }
                });
    }

    private List<String> getEvents(List<MovieUserReviewObject> movieUserReviewObjectList) {
        List<String> eventList = new ArrayList<>();
        for (MovieUserReviewObject movieUserReviewObject : movieUserReviewObjectList) {
            if (movieUserReviewObject.getReviewFor().equals("event"))
                eventList.add(movieUserReviewObject.getReviewedEntityId());
        }
        return eventList;
    }

    private void updateEvents(List<Event> events, List<MovieUserReviewObject> movieUserReviewObjects) {
        for (Event event : events) {
            for (MovieUserReviewObject movieUserReviewObject : movieUserReviewObjects) {
                if (movieUserReviewObject.getReviewedEntityId().equalsIgnoreCase(event.id)) {
                    movieUserReviewObject.setReviewedEntityImage(event.imgUrl);
                    movieUserReviewObject.setReviewedEntityLocation(event.locality);
                    movieUserReviewObject.setEvent(event);
                }
            }
        }
        mAdapter.setMyReviewsData(movieUserReviewObjects);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();

        }
        return super.onOptionsItemSelected(item);
    }
}
