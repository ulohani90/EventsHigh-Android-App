package com.eventshigh.nearme.app.activity;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.task.FetchLocalityTask;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * An {@link com.eventshigh.nearme.app.activity.BaseEventsActivity} which shows the events in Grid.
 * On Phone, we have one column in portrait mode and two columns in landscape mode. On Tablet,
 * we try to put more columns as per the width offered.
 */
public class EventsGridActivity extends BaseEventsActivity {

    private boolean showFollowCard;
    private boolean searchViewExpanded;

    // ***********************
    // Delegated Methods from {@link BaseEventsActivity}
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    protected  int getDisabledMenuItem() {
        return R.id.action_show_list;
    }

    protected void showEvents() {
        Fragment eventFragment;
        if (!eventsContext.query.isEmpty()) {
            EventsFragment eventFragment1 = EventsFragment.getInstance(
                    eventsContext, showFollowCard, true);
            if (showFollowCard) {
                eventFragment1.setOnScrollListener(followCardScrollListener);
            }
            eventFragment = eventFragment1;
        } else {
            eventFragment = ThisWeekFragment.getInstance(eventsContext, true, 14);
        }

        FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
        tr.replace(R.id.event_container, eventFragment);
        tr.commit();

        new FetchLocalityTask(this).execute(eventsContext.location);
    }

    private OnScrollListener followCardScrollListener = new OnScrollListener() {
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
        toolbar.setBackgroundColor(Color.argb(toolbarAlpha, 0xEA, 0x5D, 0x4B));
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
