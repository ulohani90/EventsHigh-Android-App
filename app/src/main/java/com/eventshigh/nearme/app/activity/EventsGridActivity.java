package com.eventshigh.nearme.app.activity;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.View;

import com.eventshigh.nearme.app.task.ShowLocalityTask;
import com.google.android.gms.maps.model.LatLng;

/**
 * An {@link com.eventshigh.nearme.app.activity.BaseEventsActivity} which shows the events in Grid.
 * On Phone, we have one column in portrait mode and two columns in landscape mode. On Tablet,
 * we try to put more columns as per the width offered.
 */
public class EventsGridActivity extends BaseEventsActivity {

    // Called when fab icon is pressed
    public void onSwitchView(View view) {
        switchTo(EventsMapsActivity.class);
    }

    // ***********************
    // Helper Methods
    // ***********************

    @Override
    protected boolean shouldIncludeWithoutLocation() {
        return true;
    }

    @Override
    protected Fragment getNewFragment() {
        return new EventGridFragment();
    }

    protected void updateUserLocation(@Nullable LatLng userLocation) {
        if (userLocation != null) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar.getSubtitle() == null || actionBar.getSubtitle().length() == 0) {
                new ShowLocalityTask(this, actionBar).execute(userLocation);
            }
        }

        super.updateUserLocation(userLocation);
    }
}
