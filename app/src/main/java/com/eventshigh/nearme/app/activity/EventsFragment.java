package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.MyEvents;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

/**
 * Fragment to show events.
 */
public class EventsFragment extends Fragment {
    private static final String LOG_TAG = EventsFragment.class.getSimpleName();
    private static final String EVENT_CONTEXT_PARAM = EventsFragment.class.getName() + "_event_context";

    private BaseContextActivity activity;
    private EventsAdapter eventsAdapter;

    private AutofitRecyclerView eventGridView;
    private View topProgressBar;
    private View noMyEventsView;
    private View retryView;

    public static EventsFragment getInstance(EventsContext eventsContext) {
        EventsFragment fragment = new EventsFragment();

        Bundle args = new Bundle();
        args.putParcelable(EVENT_CONTEXT_PARAM, eventsContext);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseContextActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup the events adapter to show data.
        eventsAdapter = new EventsAdapter(activity);
        eventGridView = (AutofitRecyclerView) view.findViewById(R.id.event_grid);
        eventGridView.setEventsAdapter(eventsAdapter);

        // Setup the refresh on swipe down.
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                activity.reportActionToAnalytics("swipeRefresh");
                swipeRefreshLayout.setRefreshing(false);
                fetchNewListing(true /* bypass cache*/);
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);

        view.findViewById(R.id.retry).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("retry");
                fetchNewListing(false /* bypass cache*/);
            }
        });

        // Setup the actionbar hide/show on scroll.
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setShowHideAnimationEnabled(true);
            eventGridView.setOnScrollListener(new OnScrollListener() {
                int currentY;

                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);

                    currentY += dy;
                    if (currentY > 150 || currentY < -150) {
                        boolean isDown = dy > 0;
                        if (isDown && actionBar.isShowing()) {
                            actionBar.hide();
                        }

                        if (!isDown && !actionBar.isShowing()) {
                            actionBar.show();
                        }

                        currentY = 0;
                    }
                }
            });
        }

        topProgressBar = view.findViewById(R.id.top_progress_bar);
        noMyEventsView = view.findViewById(R.id.view_no_my_event);
        retryView = view.findViewById(R.id.view_retry);

        fetchNewListing(false);
    }

    private void fetchNewListing(boolean shouldBypassCache) {
        topProgressBar.setVisibility(View.VISIBLE);
        noMyEventsView.setVisibility(View.GONE);
        retryView.setVisibility(View.GONE);

        // Stop all requests associated with this activity and then submit new request.
        EventsContext eventsContext = getArguments().getParcelable(EVENT_CONTEXT_PARAM);
        VolleyHelper.getRequestQueue(activity).cancelAll(this);
        if (EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
            new MyEventsRequest(activity, eventsContext, Priority.IMMEDIATE,
                    shouldBypassCache, true, mMyEventsFetcherCallBack, mErrorListener).execute();
        } else {
            EventCollectionRequest.submit(activity, eventsContext, Priority.IMMEDIATE,
                    shouldBypassCache, true, mEventsFetcherCallBack, mErrorListener);
        }
    }

    private Listener<MyEvents> mMyEventsFetcherCallBack = new Listener<MyEvents>() {
        @Override
        public void onResponse(MyEvents myEvents, boolean isIntermediate) {
            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (myEvents.isEmpty()) {
                    noMyEventsView.setVisibility(View.VISIBLE);
                }
            }

            if (!isIntermediate || !myEvents.isEmpty()) {
                eventsAdapter.setMyEvents(myEvents, eventGridView.getSpanCount() * 2);
            }
        }
    };

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private Listener<List<Event>> mEventsFetcherCallBack = new Listener<List<Event>>() {
        @Override
        public void onResponse(List<Event> events, boolean isIntermediate) {
            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (events.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    Toast.makeText(activity, R.string.no_events, Toast.LENGTH_SHORT).show();
                }
            }

            if (!isIntermediate || !events.isEmpty()) {
                eventsAdapter.setEvents(events);
            }
        }
    };

    protected ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);
            if (eventsAdapter.getItemCount() > 0) {
                Toast.makeText(activity, R.string.failed_refresh, Toast.LENGTH_SHORT).show();
            } else {
                retryView.setVisibility(View.VISIBLE);
            }

            Throwable cause = volleyError.getCause();
            if (cause != null) {
                Log.w(LOG_TAG, "Volley Error: " + volleyError.getMessage(), cause);
                activity.reportActionToAnalytics("failedRequest", cause.getClass().getSimpleName());
            } else {
                Log.w(LOG_TAG, "Volley Error: " + volleyError.getMessage());
                activity.reportActionToAnalytics("failedRequest");
            }
        }
    };
}
