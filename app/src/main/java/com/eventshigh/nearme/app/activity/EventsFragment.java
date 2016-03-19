package com.eventshigh.nearme.app.activity;

import android.content.Context;
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
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

/**
 * Fragment to show events.
 */
public class EventsFragment extends BaseEventsFragment {
    private AutofitRecyclerView eventGridView;
    private View topProgressBar;
    private View noMyEventsView;
    private View retryView;

    private EventsAdapter eventsAdapter;
    private OnScrollListener onScrollListener;

    int scrollPosition = 0;


    public static EventsFragment getInstance(EventsContext eventsContext, boolean showFollowCard,
                                             boolean showCategories) {
        EventsFragment fragment = new EventsFragment();
        Bundle args = getArgs(eventsContext, showFollowCard, showCategories);
        fragment.setArguments(args);
        return fragment;
    }

    public void onAttach(Context context) {
        super.onAttach(context);

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
        eventsAdapter = new EventsAdapter(activity);
        eventGridView = (AutofitRecyclerView) view.findViewById(R.id.event_grid);
        eventGridView.setAdapter(eventsAdapter);
        eventsAdapter.setOnItemClickedListener(new EventsAdapter.OnItemClickedListener() {
            @Override
            public void onItemClicked(int pos) {
                scrollPosition = pos;
            }
        });

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
                activity.showSearchView("today");
            }
        });

        view.findViewById(R.id.retry).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("retry");
                fetchNewListing(false);
            }
        });

        // More views.
        topProgressBar = view.findViewById(R.id.top_progress_bar);
        noMyEventsView = view.findViewById(R.id.view_no_my_event);
        retryView = view.findViewById(R.id.view_retry);
    }

    @Override
    public void onStart() {
        super.onStart();
        fetchNewListing(false);
    }

    @Override
    public void onStop() {

        if(asyncRequest!=null)
        asyncRequest.cancel(true);


        if(eventsAdapter!=null){
            eventsAdapter.clear();
        }
        super.onStop();
    }

    public void setOnScrollListener (OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    MyEventsRequest asyncRequest;
    private void fetchNewListing(boolean shouldBypassCache) {
        topProgressBar.setVisibility(View.VISIBLE);
        noMyEventsView.setVisibility(View.GONE);
        retryView.setVisibility(View.GONE);

        // Stop all requests associated with this fragment and then submit new request.
        if (EventsHighEndpoints.isMyEventQuery(eventsContext.query)) {
            asyncRequest =  new MyEventsRequest(activity, eventsContext, Priority.IMMEDIATE, this,
                    shouldBypassCache, mMyEventsFetcherCallBack, mErrorListener);
            asyncRequest.execute();
        } else if (eventsContext.query.isEmpty() && !eventsContext.dateFilter.isEmpty() && showCategories) {
            DateCategoryRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                    shouldBypassCache, mMyEventsFetcherCallBack, mErrorListener);
        } else {
            EventCollectionRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                    shouldBypassCache, mEventsFetcherCallBack, mErrorListener);
        }
    }

    boolean isFragmentDestroyed=false;
    @Override
    public void onDestroy() {
        isFragmentDestroyed = true;
        super.onDestroy();
    }

    private Listener<List<TopicEvents>> mMyEventsFetcherCallBack = new Listener<List<TopicEvents>>() {
        @Override
        public void onResponse(List<TopicEvents> myEvents, boolean isIntermediate) {
            if (isFragmentDestroyed) {
                return;
            }

            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (myEvents.isEmpty()) {
                    if (EventsHighEndpoints.isMyEventQuery(eventsContext.query) && retryView.getVisibility() == View.GONE) {
                        noMyEventsView.setVisibility(View.VISIBLE);
                    } else {
                        noMyEventsView.setVisibility(View.GONE);
                        retryView.setVisibility(View.VISIBLE);
                    }
                }
            }

            if (!isIntermediate || !myEvents.isEmpty()) {
                if(getActivity()!=null && (getActivity()) instanceof EventsGridActivity){
                    ((EventsGridActivity)getActivity()).setShareImageUrl(myEvents.get(0).events.get(0).imgUrl);
                }
                eventsAdapter.setTopicEvents(myEvents, eventsContext, eventGridView.getSpanCount() * 2);

                eventGridView.scrollToPosition(scrollPosition);
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
                String seeAllQuery = eventsContext.query.isEmpty() ||
                        eventsContext.dateFilter.isEmpty() ? null : eventsContext.query;
                if(getActivity()!=null && (getActivity()) instanceof EventsGridActivity){
                    if(eventsCollection.events.size()>0)
                    ((EventsGridActivity)getActivity()).setShareImageUrl(eventsCollection.events.get(0).imgUrl);
                }
                eventsAdapter.setEvents(eventsCollection.events, seeAllQuery);
                if (showFollowCard) {
                    eventsAdapter.addFollowCard(eventsContext.query, eventsCollection.events.size(),
                            eventsCollection.numFollowers);
                }
                eventGridView.scrollToPosition(scrollPosition);
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
