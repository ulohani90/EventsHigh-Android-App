package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView.OnScrollListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.DateCategoryRequest;
import com.eventshigh.nearme.app.network.EventCollectionRequest;
import com.eventshigh.nearme.app.network.EventCollectionRequest.EventsCollection;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest.TopicEvents;
import com.eventshigh.nearme.app.network.SocialActionsRequest;
import com.eventshigh.nearme.app.network.SocialActionsRequest.SocialActions;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Fragment to show events.
 */
public class EventsFragment extends BaseEventsFragment {
    private static final long REFRESH_MY_EVENTS_INTERVAL = TimeUnit.SECONDS.toMillis(2);

    private AutofitRecyclerView eventGridView;
    private View topProgressBar;
    private View noMyEventsView;
    private View retryView;

    private EventsAdapter eventsAdapter;
    private OnScrollListener onScrollListener;

    public static EventsFragment getInstance(EventsContext eventsContext, boolean showFollowCard,
                                             boolean showCategories) {
        EventsFragment fragment = new EventsFragment();
        fragment.setArguments(getArgs(eventsContext, showFollowCard, showCategories));
        return fragment;
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (onScrollListener == null) {
            onScrollListener = new HideActionBarOnScroll(this.activity);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_events, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // Setup the events adapter to show data.
        eventsAdapter = new EventsAdapter(this);
        eventGridView = (AutofitRecyclerView) view.findViewById(R.id.event_grid);
        eventGridView.setEventsAdapter(eventsAdapter);

        // Setup the actionbar hide/show on scroll.
        eventGridView.addOnScrollListener(onScrollListener);

        // Setup the refresh on swipe down.
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                activity.reportActionToAnalytics("swipeRefresh", eventsContext.toString());
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
                refresh();
            }
        });

        // More views.
        topProgressBar = view.findViewById(R.id.top_progress_bar);
        noMyEventsView = view.findViewById(R.id.view_no_my_event);
        retryView = view.findViewById(R.id.view_retry);
    }

    @Override
    protected void refresh() {
        fetchNewListing(false);
    }

    @Override
    protected long refreshInterval() {
        return EventsHighEndpoints.isMyEventQuery(eventsContext.query) ?
                REFRESH_MY_EVENTS_INTERVAL : super.refreshInterval();
    }

    public void setOnScrollListener (OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    private void fetchNewListing(boolean shouldBypassCache) {
        topProgressBar.setVisibility(View.VISIBLE);
        noMyEventsView.setVisibility(View.GONE);
        retryView.setVisibility(View.GONE);

        // Stop all requests associated with this fragment and then submit new request.
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

        // Load social actions.
        SocialActionsRequest.submit(activity, Priority.LOW, this, shouldBypassCache,
            new Listener<SocialActions>() {
                @Override
                public void onResponse(SocialActions socialActions, boolean isIntermediate) {
                    eventsAdapter.setSocialActions(socialActions);
                }
            },
            new ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    VolleyHelper.log(activity, volleyError);
                }
            }
        );
    }

    private Listener<List<TopicEvents>> mMyEventsFetcherCallBack = new Listener<List<TopicEvents>>() {
        @Override
        public void onResponse(List<TopicEvents> myEvents, boolean isIntermediate) {
            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (myEvents.isEmpty()) {
                    if (EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
                        noMyEventsView.setVisibility(View.VISIBLE);
                    } else {
                        retryView.setVisibility(View.VISIBLE);
                    }
                }
            }

            if (!isIntermediate || !myEvents.isEmpty()) {
                eventsAdapter.setTopicEvents(myEvents, eventGridView.getSpanCount() * 2);
            }
        }
    };

    // This callback is called by EventsFetcher when new set of events are available. We build the
    // markers for all events and then call method to show selected markers.
    private Listener<EventsCollection> mEventsFetcherCallBack = new Listener<EventsCollection>() {
        @Override
        public void onResponse(EventsCollection eventsCollection, boolean isIntermediate) {
            if (isDetached()) {
                return;
            }

            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (eventsCollection.events.isEmpty()) {
                    // Failed. Show toast and return empty list.
                    Snackbar.make(topProgressBar, R.string.no_events, Snackbar.LENGTH_SHORT).show();
                }
            }

            if (!isIntermediate || !eventsCollection.events.isEmpty()) {
                eventsAdapter.setEvents(eventsCollection.events,
                        (eventsContext.query.isEmpty() || eventsContext.dateFilter.isEmpty() ? null : eventsContext.query));
                if (showFollowCard) {
                    eventsAdapter.addFollowCard(eventsContext.query, eventsCollection.events.size(),
                            eventsCollection.numFollowers);
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
                activity.showMessage(R.string.failed_refresh);
            } else {
                retryView.setVisibility(View.VISIBLE);
            }

            VolleyHelper.log(activity, volleyError);
        }
    };
}
