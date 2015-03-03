package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;

import org.apmem.tools.layouts.FlowLayout;

/**
 * Shows the filters screen showing various categories and let user select few
 * and go back to event list where filter is applied.
 */
public class ShowFiltersActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_filters);

        FlowLayout categoryContainer = (FlowLayout) findViewById(R.id.category_container);
        for (String tag : LaunchActivity.EXPLORE_TAGS) {
            getLayoutInflater().inflate(R.layout.event_tag, categoryContainer);
            ((TextView)categoryContainer.getChildAt(categoryContainer.getChildCount() - 1)).setText(tag);
        }
    }
}
