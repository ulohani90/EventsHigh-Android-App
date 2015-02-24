package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.Editor;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.data.EventsMarkerManager.OnEventMarkChangeListener;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.eventshigh.nearme.app.view.GridViewAnimator;

import java.util.ArrayList;
import java.util.List;

public class EventGridFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    public static final String EVENTS_LIST_PARAMETER = "events";

    private EventsGridActivity activity;
    private EventsAdapter eventsAdapter;
    private Editor eventsMarkerEditor;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.fragment_event_grid, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView eventGridView = (RecyclerView) view.findViewById(R.id.event_grid);
        eventGridView.setAdapter(eventsAdapter);
        eventGridView.setItemAnimator(new GridViewAnimator(eventsAdapter));

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.app_color);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        this.activity = (EventsGridActivity) activity;
        eventsMarkerEditor = EventsMarkerManager.getInstance(activity).getEditor();

        List<Event> events = new ArrayList<>();
        if (getArguments() != null) {
            events = getArguments().getParcelableArrayList(EVENTS_LIST_PARAMETER);
            eventsMarkerEditor.getEventsMarkerManager().removeDismissed(events);
        }

        eventsAdapter = new EventsAdapter(this.activity, events, eventsMarkerEditor);

        // Add listener to remove dismissed events.
        eventsMarkerEditor.getEventsMarkerManager()
                .addOnEventMarkChangeListener(mOnEventMarkChangeListener);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        eventsMarkerEditor.close();
        eventsMarkerEditor.getEventsMarkerManager()
                .removeOnEventMarkChangeListener(mOnEventMarkChangeListener);
    }


    // ***********************
    // Callbacks
    // ***********************

    private final OnEventMarkChangeListener mOnEventMarkChangeListener = new OnEventMarkChangeListener() {
        @Override
        public void onEventStateChange(String eventId, @Nullable EventMark eventMark) {
            if (EventMark.isDismissed(eventMark)) {
                for(int i = 0; i < eventsAdapter.getItemCount(); i++) {
                    Event event = eventsAdapter.getItem(i);
                    if (event.id.equals(eventId)) {
                        eventsAdapter.remove(event);
                        break;
                    }
                }
                return;
            }

            // An event can be present in multiple tabs and we need to redraw the event cards
            // so that favourite status is shown correctly in all tabs.
            eventsAdapter.notifyDataSetChanged(eventId);
        }
    };

    @Override
    public void onRefresh() {
        activity.reportActionToAnalytics("swipeRefresh");
        swipeRefreshLayout.setRefreshing(false);
        activity.fetchNewListing(true /* bypass cache*/);
    }
}
