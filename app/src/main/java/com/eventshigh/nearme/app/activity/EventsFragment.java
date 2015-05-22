package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
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
import com.eventshigh.nearme.app.network.DateCategoryRequest;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

/**
 * Fragment to show events.
 */
public class EventsFragment extends BaseEventsFragment {
    private static final String LOG_TAG = EventsFragment.class.getSimpleName();

    private EventsAdapter eventsAdapter;

    private AutofitRecyclerView eventGridView;
    private View topProgressBar;
    private View noMyEventsView;
    private View retryView;

    private OnScrollListener onScrollListener = new HideActionBarOnScroll(activity);

    public static EventsFragment getInstance(EventsContext eventsContext, boolean showFollowCard,
                                             boolean showCategories) {
        EventsFragment fragment = new EventsFragment();
        fragment.setArguments(getArgs(eventsContext, showFollowCard, showCategories));
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
            eventGridView.post(new Runnable() {
                @Override
                public void run() {
                    fetchNewListing(false);
                }
            });
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup the events adapter to show data.
        eventsAdapter = new EventsAdapter(this);
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

        // Actions Buttons.
        view.findViewById(R.id.explore_events).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchView("today");
            }
        });

        view.findViewById(R.id.retry).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("retry");
                fetchNewListing(false /* bypass cache*/);
            }
        });

        // Setup the actionbar hide/show on scroll.
        eventGridView.setOnScrollListener(onScrollListener);

        topProgressBar = view.findViewById(R.id.top_progress_bar);
        noMyEventsView = view.findViewById(R.id.view_no_my_event);
        retryView = view.findViewById(R.id.view_retry);

        fetchNewListing(false);
    }

    public void setOnScrollListener (OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    private void fetchNewListing(boolean shouldBypassCache) {
        topProgressBar.setVisibility(View.VISIBLE);
        noMyEventsView.setVisibility(View.GONE);
        retryView.setVisibility(View.GONE);

        // Stop all requests associated with this activity and then submit new request.
        VolleyHelper.getRequestQueue(activity).cancelAll(this);
        if (EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
            new MyEventsRequest(activity, eventsContext, Priority.IMMEDIATE, this,
                    shouldBypassCache, true, mMyEventsFetcherCallBack, mErrorListener).execute();
        } else if (eventsContext.query.isEmpty() && !eventsContext.dateFilter.isEmpty() && showCategories) {
            DateCategoryRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                    shouldBypassCache, mMyEventsFetcherCallBack, mErrorListener);
        } else {
            EventCollectionRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                    shouldBypassCache, true, mEventsFetcherCallBack, mErrorListener);
        }
    }

    private Listener<List<TopicEvents>> mMyEventsFetcherCallBack = new Listener<List<TopicEvents>>() {
        @Override
        public void onResponse(List<TopicEvents> myEvents, boolean isIntermediate) {
            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (myEvents.isEmpty()) {
                    noMyEventsView.setVisibility(View.VISIBLE);
                }
            }

            if (!isIntermediate || !myEvents.isEmpty()) {
                eventsAdapter.setTopicEvents(myEvents, eventGridView.getSpanCount() * 2);
            }
        }
    };

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private Listener<List<Event>> mEventsFetcherCallBack = new Listener<List<Event>>() {
        @Override
        public void onResponse(List<Event> events, boolean isIntermediate) {
            if (isDetached()) {
                return;
            }

            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (events.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    Toast.makeText(activity, R.string.no_events, Toast.LENGTH_SHORT).show();
                }
            }

            if (!isIntermediate || !events.isEmpty()) {
                eventsAdapter.setEvents(events,
                    (eventsContext.query.isEmpty() || eventsContext.dateFilter.isEmpty() ? null : eventsContext.query));
                if (showFollowCard) {
                    eventsAdapter.addFollowCard(eventsContext.query, events.size());
                }
            }
        }
    };

    private ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            if (isDetached()) {
                return;
            }

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
