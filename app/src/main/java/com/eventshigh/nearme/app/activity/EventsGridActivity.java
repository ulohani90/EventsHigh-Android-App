package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * Shows the events in Grid layout.
 */
public class EventsGridActivity extends BaseContextActivity {

    private boolean showFollowCard;
    private View fabShare;

    String shareImageUrl;

    private boolean isFromNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_events_grid);

        if (eventsContext.city == null) {
            City lastCity = new Account(this).getLastCity();
            if (lastCity != null) {
                reportActionToAnalytics("usedLastCity");
                eventsContext.changeLocation(lastCity);
            }
        }

        // Show query as title.
        if (!eventsContext.query.isEmpty()) {
            setTitle();
        }

        // Fab Share.
        fabShare = findViewById(R.id.fab_share);
        fabShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //shareEvents(eventsContext);
                shareEventsWithBranch(eventsContext,shareImageUrl);
            }
        });

        // Should we show follow widget?
        showFollowCard = !eventsContext.query.isEmpty() &&
                eventsContext.dateFilter.isEmpty() &&
                !EventsHighEndpoints.isDateQuery(eventsContext.query) &&
                !EventsHighEndpoints.isMyEventQuery(eventsContext.query) &&
                !EventsHighEndpoints.isFeaturedEventQuery(eventsContext.query);
        if (showFollowCard) {
            // Hide the regular toolbar and show the follow toolbar
            toolbar.setVisibility(View.GONE);
            toolbar = (Toolbar) findViewById(R.id.follow_toolbar);
            toolbar.setVisibility(View.VISIBLE);
            setSupportActionBar(toolbar);
            updateToolbar(0);
            setTitle();
        }
        if(getIntent() != null &&
                getIntent().getAction() != null && getIntent().getAction().startsWith(NOTIFICATION_ACTION)){
            isFromNotification = true;
        }


        // Add Events Fragment.
        Fragment eventFragment;
        if (!eventsContext.query.isEmpty()) {
            EventsFragment eventFragment1 = EventsFragment.getInstance(
                    eventsContext, showFollowCard, false,(SocialInvitationsRequest.SpecialCoupons)getIntent().getParcelableExtra("special_obj"));

            eventFragment1.setOnScrollListener(
                    showFollowCard ? followCardScrollListener : doNothingScrollListener);
            eventFragment = eventFragment1;
        } else {
            eventFragment = ThisWeekFragment.getInstance(eventsContext, false, 14);
        }

        FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
        tr.replace(R.id.event_container, eventFragment);
        tr.commit();
    }

    /*@Override
    protected void onStart() {
        super.onStart();
        String action = getIntent().getAction();
        if (BaseActivity.NOTIFICATION_ACTION.equals(action)) {
            reportActionToAnalytics("openNotification",eventsContext.query);
        }
    }*/

    public void setShareImageUrl(String shareImageUrl) {
        this.shareImageUrl = shareImageUrl;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (showFollowCard || EventsHighEndpoints.isDateQuery(eventsContext.query)) {
            fabShare.setVisibility(View.VISIBLE);
        }

        return true;
    }

    private OnScrollListener doNothingScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
        }
    };

    private OnScrollListener followCardScrollListener = new OnScrollListener() {
        private int y;

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            // do nothings.
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            y += dy;
            updateToolbar(Math.min(y, 255));
        }
    };

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
        toolbar.setBackgroundColor(Color.argb(toolbarAlpha, 0xCE, 0x4A, 0x46));
        toolbar.setTitleTextColor(Color.argb(toolbarAlpha, 255, 255, 255));
        toolbar.setSubtitleTextColor(Color.argb(toolbarAlpha, 255, 255, 255));
    }

    @Override
    public View getViewForSnackbar() {
        return toolbar;
    }

    private void setDarkToolbarIcons() {
        toolbar.post(new Runnable() {
            @SuppressWarnings("deprecation")
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
            @SuppressWarnings("deprecation")
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.white), EventsGridActivity.this);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(isFromNotification){
            Intent intent  =new Intent(this,LaunchActivity.class);
            startActivity(intent);
        }
        super.onBackPressed();
    }
}
