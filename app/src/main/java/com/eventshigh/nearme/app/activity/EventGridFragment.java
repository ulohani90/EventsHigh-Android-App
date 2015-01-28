package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.eventshigh.nearme.app.user.Personalization;
import com.eventshigh.nearme.app.user.Personalization.OnEventStateListener;
import com.eventshigh.nearme.app.user.Personalization.UserEventPref;

import java.util.ArrayList;

public class EventGridFragment extends Fragment {
    public static final String EVENTS_LIST_PARAMETER = "events";

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
            ArrayList<Event> events = getArguments().getParcelableArrayList(EVENTS_LIST_PARAMETER);
            eventsAdapter.clear();
            eventsAdapter.addAll(events);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (EventsGridActivity) activity;

        Personalization.getInstance(activity).addOnEventStateChangeListener(mOnEventStateListener);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Personalization.getInstance(activity).removeOnEventStateChangeListener(mOnEventStateListener);
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

    private final OnEventStateListener mOnEventStateListener = new OnEventStateListener() {
        @Override
        public void onEventStateChange(String eventId, @Nullable UserEventPref pref) {
            boolean isDismissed = (pref != null && pref == UserEventPref.DISMISSED);
            if (isDismissed) {
                for(int i = 0; i < eventsAdapter.getCount(); i++) {
                    Event event = eventsAdapter.getItem(i);
                    if (event.id.equals(eventId)) {
                        eventsAdapter.remove(event);
                        break;
                    }
                }
            } else {
                eventsAdapter.notifyDataSetChanged();
            }
        }
    };
}
