package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsContext;

/**
 * Created by umesh on 22/08/17.
 */

public class FragmentContainerActivity extends BaseContextActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_container_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        String title = getIntent().getStringExtra("title");
        getSupportActionBar().setTitle(title);
        EventsContext eventsContext = getIntent().getParcelableExtra("event_context");
        EventsFragment fragment = EventsFragment.getInstance(eventsContext, false, false, false, null, false, null, false);
        getSupportFragmentManager().beginTransaction().add(R.id.container, fragment).commitAllowingStateLoss();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
