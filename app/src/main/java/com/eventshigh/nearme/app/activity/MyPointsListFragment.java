package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.network.OffersRequest;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

/**
 * Created by umesh on 15/04/16.
 */
public class MyPointsListFragment  extends Fragment {

    public static MyPointsListFragment newInstance(Bundle args) {
        MyPointsListFragment fragment = new MyPointsListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private EventsAdapter eventsAdapter;
    private View topProgressBar;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events,container,false);
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        eventsAdapter = new EventsAdapter((LaunchActivity)getActivity());
        AutofitRecyclerView exploreGridView = (AutofitRecyclerView) view.findViewById(R.id.event_grid);
        exploreGridView.setAdapter(eventsAdapter);
        exploreGridView.addOnScrollListener(new HideActionBarOnScroll((LaunchActivity) getActivity()));
        int paddingTop = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
        int paddingBottom = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        exploreGridView.setPadding(0, paddingTop, 0, paddingBottom);
        exploreGridView.setVerticalSpacing(0);
        topProgressBar = view.findViewById(R.id.top_progress_bar);
        topProgressBar.setVisibility(View.VISIBLE);
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //activity.reportActionToAnalytics("swipeRefresh", eventsContext.toString());
                swipeRefreshLayout.setRefreshing(false);
                makeServerRequest(true);
            }
        });

        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
    }

    @Override
    public void onStart() {

        super.onStart();

        makeServerRequest(false);
    }

    public void makeServerRequest(boolean shouldByPassCache){
        City city = new Account(getActivity()).getLastCity();
        OffersRequest.submit(getActivity(), city, Request.Priority.IMMEDIATE, this, shouldByPassCache, mListener, mErrorListener);

    }

    private Response.Listener<OffersRequest.OffersPointsObject> mListener = new Response.Listener<OffersRequest.OffersPointsObject>() {
        @Override
        public void onResponse(OffersRequest.OffersPointsObject offersPointsObject, boolean isIntermediate) {
            topProgressBar.setVisibility(View.GONE);
            eventsAdapter.setPoints(offersPointsObject.points);
        }
    };

    private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);


        }
    };
}
