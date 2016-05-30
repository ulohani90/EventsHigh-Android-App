package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.ArrayList;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * Created by umesh on 24/05/16.
 */
public class EventAllReviewsActivity extends BaseContextActivity {

    EventsAdapter eventsAdapter;

    ArrayList<MovieUserReviewObject> objs;

    String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_points_breakdown);

        eventsAdapter = new EventsAdapter(this);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (getIntent().hasExtra(EventDetailActivity.EVENT_REVIEWS)) {
            objs = getIntent().getParcelableArrayListExtra(EventDetailActivity.EVENT_REVIEWS);
            eventId = getIntent().getStringExtra(EventDetailActivity.EVENT_ID);
        }
        AutofitRecyclerView exploreGridView = (AutofitRecyclerView) findViewById(R.id.event_grid);
        exploreGridView.setAdapter(eventsAdapter);
        exploreGridView.addOnScrollListener(new HideActionBarOnScroll(this));

        findViewById(R.id.top_progress_bar).setVisibility(View.GONE);
        ((SwipeRefreshLayout) findViewById(R.id.swipe_refresh)).setEnabled(false);
        if (objs != null) {
            eventsAdapter.setUserMovieReviews(objs, eventId);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (toolbar != null) {
            setLightToolbarIcons();
        }
    }

    private void setLightToolbarIcons() {
        toolbar.post(new Runnable() {
            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.white), EventAllReviewsActivity.this);
            }
        });
    }

}
