package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.ProfileInfo;
import com.eventshigh.nearme.app.network.FetchProfileRequest;
import com.eventshigh.nearme.app.network.FetchUserInterestsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

/**
 * Created by umesh on 22/08/17.
 */

public class MyInterestsActivity extends BaseContextActivity {

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
        getSupportActionBar().setTitle("My Interests");
        profileId = getIntent().getStringExtra("profile_id");
        progressBar = findViewById(R.id.top_progress_bar);
        noEventLayout = findViewById(R.id.view_no_my_event);
        retryView = findViewById(R.id.view_retry);
        retryView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUserInterests(true);
            }
        });
        noMyEventHeading = (TextView) findViewById(R.id.no_my_event_heading);
        noMyEventHeading.setText(getResources().getString(R.string.ui_no_my_interest));
        recyclerView = (AutofitRecyclerView) findViewById(R.id.event_grid);
        exploreEvents = (TextView) findViewById(R.id.explore_events);
        exploreEvents.setText("Personalize");
        exploreEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MyInterestsActivity.this, SelectInterestsActivity.class);
                startActivity(intent);
            }
        });
        mAdapter = new EventsAdapter(this);
        recyclerView.setAdapter(mAdapter);
        loadUserInterests(true);

    }

    public void onRetry(View view) {
        loadUserInterests(true);
    }

    private void loadUserInterests(boolean shouldByPassChange) {
        progressBar.setVisibility(View.VISIBLE);
        retryView.setVisibility(View.GONE);
        FetchUserInterestsRequest.submit(this, profileId, Request.Priority.HIGH,
                new Response.Listener<List<MyEventsRequest.TopicEvents>>() {
                    @Override
                    public void onResponse(List<MyEventsRequest.TopicEvents> topicEvents, boolean b) {

                        if (topicEvents != null && topicEvents.size() > 0) {
                            mAdapter.setTopicEvents(topicEvents, eventsContext, recyclerView.getSpanCount() * 2);
                            recyclerView.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                            noEventLayout.setVisibility(View.GONE);
                            retryView.setVisibility(View.GONE);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.GONE);
                            noEventLayout.setVisibility(View.VISIBLE);
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
