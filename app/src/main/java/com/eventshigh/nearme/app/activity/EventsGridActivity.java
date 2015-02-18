package com.eventshigh.nearme.app.activity;

import android.support.v4.app.Fragment;
import android.view.View;

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
    protected boolean showLocationInActionBar() {
        return true;
    }

    @Override
    protected boolean isDefaultView() {
        return !pref.isMapsViewDefault();
    }

    @Override
    protected Fragment getNewFragment() {
        return new EventGridFragment();
    }
}
