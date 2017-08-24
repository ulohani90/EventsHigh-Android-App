package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MyTicketObject;
import com.eventshigh.nearme.app.data.NewSocialFriend;
import com.eventshigh.nearme.app.network.FetchUserFriendsRequest;
import com.eventshigh.nearme.app.network.MyTicketsRequest;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.ui.adapter.NewContactsAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;

import java.util.List;

/**
 * Created by umesh on 23/08/17.
 */

public class MyTicketsActivity extends BaseContextActivity {

    View progressBar;
    View noEventLayout;
    AutofitRecyclerView recyclerView;
    EventsAdapter mAdapter;
    String profileId;
    View retryView;
    TextView noMyEventHeading;
    TextView exploreEvents;
    TextView noTicketsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recyclerview_with_progress_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Tickets");
        profileId = getIntent().getStringExtra("profile_id");
        progressBar = findViewById(R.id.top_progress_bar);
        noEventLayout = findViewById(R.id.view_no_my_event);
        retryView = findViewById(R.id.view_retry);
        noTicketsView = (TextView) findViewById(R.id.no_tickets_view);
        retryView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUserTickets(true);
            }
        });
        noMyEventHeading = (TextView) findViewById(R.id.no_my_event_heading);
        noMyEventHeading.setText(getResources().getString(R.string.ui_no_my_interest));
        recyclerView = (AutofitRecyclerView) findViewById(R.id.event_grid);
        exploreEvents = (TextView) findViewById(R.id.explore_events);
        exploreEvents.setText("Personalize");
        mAdapter = new EventsAdapter(this);
        recyclerView.setAdapter(mAdapter);
        loadUserTickets(true);

    }

    public void onRetry(View view) {
        loadUserTickets(true);
    }


    private void loadUserTickets(boolean shouldByPassChange) {
        progressBar.setVisibility(View.VISIBLE);
        retryView.setVisibility(View.GONE);
        MyTicketsRequest.submit(this, Request.Priority.IMMEDIATE, this, shouldByPassChange, mTicketsListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(MyTicketsActivity.this, R.string.failed_load, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
                retryView.setVisibility(View.VISIBLE);
                noEventLayout.setVisibility(View.GONE);
            }
        }, false);

    }

    private Response.Listener<List<MyTicketObject>> mTicketsListener = new Response.Listener<List<MyTicketObject>>() {
        @Override
        public void onResponse(List<MyTicketObject> tickets, boolean isIntermediate) {
            if (tickets != null && tickets.size() > 0) {
                setAdapterData(tickets);
            } else {
                noTicketsView.setVisibility(View.VISIBLE);
                retryView.setVisibility(View.GONE);
                noEventLayout.setVisibility(View.GONE);
            }
            progressBar.setVisibility(View.GONE);
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();

        }
        return super.onOptionsItemSelected(item);
    }


    boolean isDestroyed;

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        super.onDestroy();

    }


    public void setAdapterData(List<MyTicketObject> objs) {
        mAdapter.setOnMyTicketClickListener(myTicketItemClickedListener);
        mAdapter.setMyTicketsData(objs, this);
    }

    public int clickPosition = -1;
    EventsAdapter.OnMyTicketItemClickedListener myTicketItemClickedListener = new EventsAdapter.OnMyTicketItemClickedListener() {
        @Override
        public void onItemClicked(int pos) {
            if (clickPosition == -1) {
                clickPosition = pos;
            } else if (clickPosition == pos) {
                clickPosition = -1;
            } else {
                clickPosition = pos;
            }
            mAdapter.notifyDataSetChanged();
        }
    };
}
