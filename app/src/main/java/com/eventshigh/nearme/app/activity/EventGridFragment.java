package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.ui.EventsAdapter;

import java.util.ArrayList;

public class EventGridFragment extends Fragment {

    private EventsGridActivity activity;
    private EventsAdapter eventsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_event_grid, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GridView eventGridView = (GridView) view.findViewById(R.id.event_grid);
        eventsAdapter = new EventsAdapter(activity);
        eventGridView.setAdapter(eventsAdapter);
        eventGridView.setOnItemClickListener(mOnItemClickListener);

        if (getArguments() != null) {
            ArrayList<Event> events = getArguments().getParcelableArrayList("events");
            eventsAdapter.clear();
            eventsAdapter.addAll(events);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (EventsGridActivity) activity;
    }


    // ***********************
    // Callbacks
    // ***********************

    private final OnItemClickListener mOnItemClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            activity.showEventDetails(eventsAdapter.getItem(position));
        }
    };
}
