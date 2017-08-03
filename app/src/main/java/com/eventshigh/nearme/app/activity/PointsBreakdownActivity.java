package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.network.MyPointsBreakdownRequest;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

/**
 * Created by umesh on 22/04/16.
 */
public class PointsBreakdownActivity extends BaseContextActivity {

    public static final String FROM_NOTIFICATION_PARAM = "is_from_notification";

    public static final String TOTAL_POINTS_PARAM = "total_points_param";

    EventsAdapter eventsAdapter;

    View topProgressBar;

    boolean isFromNotification;

    long totalPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_points_breakdown);
        eventsAdapter = new EventsAdapter(this);
        if (getIntent() != null) {
            totalPoints = getIntent().getLongExtra(TOTAL_POINTS_PARAM,0);
            isFromNotification = getIntent().getBooleanExtra(FROM_NOTIFICATION_PARAM, false);

        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AutofitRecyclerView exploreGridView = (AutofitRecyclerView) findViewById(R.id.event_grid);
        exploreGridView.setAdapter(eventsAdapter);
        exploreGridView.addOnScrollListener(new HideActionBarOnScroll(this));

        topProgressBar = findViewById(R.id.top_progress_bar);
        topProgressBar.setVisibility(View.VISIBLE);

        // Setup the refresh on swipe down.
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //activity.reportActionToAnalytics("swipeRefresh", eventsContext.toString());
                topProgressBar.setVisibility(View.VISIBLE);
                swipeRefreshLayout.setRefreshing(false);
                makeServerRequest();


            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        Account account = new Account(this);
    }


    @Override
    protected void onStart() {
        super.onStart();

        makeServerRequest();
    }

    public void makeServerRequest() {

        MyPointsBreakdownRequest.submit(this, Request.Priority.IMMEDIATE, this, true, mListener, mErrorListener);
    }

    Response.Listener<MyPointsBreakdownRequest.PointBreakdownBaseObj> mListener = new Response.Listener<MyPointsBreakdownRequest.PointBreakdownBaseObj>() {
        @Override
        public void onResponse(MyPointsBreakdownRequest.PointBreakdownBaseObj pointBreakDowns, boolean b) {
            topProgressBar.setVisibility(View.GONE);
            eventsAdapter.addPointsBreakDown(pointBreakDowns);
        }
    };

    private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {

        if (isFromNotification){
            Intent intent = new Intent(this,LaunchActivity.class);
            startActivity(intent);
            finish();
        }else {
            super.onBackPressed();
        }
    }
}
