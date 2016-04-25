package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.stream.StreamItem;
import com.eventshigh.nearme.app.network.AlertsRequest;
import com.eventshigh.nearme.app.task.StreamItemLoaderTask;
import com.eventshigh.nearme.app.task.StreamItemLoaderTask.StreamItemsCallback;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.adapter.StreamAdapter;
import com.eventshigh.nearme.app.user.Account;

import java.util.List;

public class StreamFragment extends Fragment {
    private BaseContextActivity activity;
    private RecyclerView gridView;
    private StreamAdapter streamAdapter;

    ProgressBar topProgressBar;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseContextActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        gridView = (RecyclerView) view.findViewById(R.id.grid);
        topProgressBar = (ProgressBar)view.findViewById(R.id.top_progress_bar);
        topProgressBar.setVisibility(View.VISIBLE);
        streamAdapter = new StreamAdapter(activity);
        gridView.setAdapter(streamAdapter);

        // Setup the refresh on swipe down.
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                activity.reportActionToAnalytics("swipeRefresh", "stream");
                swipeRefreshLayout.setRefreshing(false);
                refresh();
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        gridView.addOnScrollListener(new HideActionBarOnScroll(activity));
        refresh();
    }

    private void refresh() {
        new StreamItemLoaderTask(activity, new StreamItemsCallback() {
            @Override
            public void onContactLoad(List<StreamItem> streamItems) {

                if (isAdded()) {
                    if(streamItems.size() == 0){
                            makeServerRequest();
                    }else {
                        topProgressBar.setVisibility(View.GONE);
                        streamAdapter.setStreamItems(streamItems);
                    }
                }
            }
        }).execute();
    }

    public void makeServerRequest(){
        AlertsRequest.submit(getActivity(),new Account(getActivity()).getLastCity(), Request.Priority.IMMEDIATE,this,true,mListener,mErrorListener);
    }

    Response.Listener<Boolean> mListener = new Response.Listener<Boolean>() {
        @Override
        public void onResponse(Boolean aBoolean, boolean b) {
            if(aBoolean){
                refresh();
            }
        }
    };
    private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {

        }
    };
}
