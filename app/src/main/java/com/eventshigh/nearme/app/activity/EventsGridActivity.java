package com.eventshigh.nearme.app.activity;

import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.task.ShowLocalityTask;
import com.google.android.gms.maps.model.LatLng;

/**
 * An {@link com.eventshigh.nearme.app.activity.BaseEventsActivity} which shows the events in Grid.
 * On Phone, we have one column in portrait mode and two columns in landscape mode. On Tablet,
 * we try to put more columns as per the width offered.
 */
public class EventsGridActivity extends BaseEventsActivity {

    // ***********************
    // Helper Methods
    // ***********************

    @Override
    protected boolean showLocationInActionBar() {
        return true;
    }

    @Override
    protected boolean showExploreTab() {
        return true;
    }

    @Override
    protected boolean isDefaultView() {
        return !pref.isMapsViewDefault();
    }

    @Override
    protected int getDisabledMenuId() {
        return R.id.action_list;
    }

    @Override
    protected Fragment getNewFragment() {
        return new EventGridFragment();
    }

    @Override
    protected void updateUserLocation(LatLng userLocation) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null &&
                (actionBar.getSubtitle() == null || actionBar.getSubtitle().length() == 0)) {
            new ShowLocalityTask(this, actionBar).execute(userLocation);
        }

        super.updateUserLocation(userLocation);
    }
}
