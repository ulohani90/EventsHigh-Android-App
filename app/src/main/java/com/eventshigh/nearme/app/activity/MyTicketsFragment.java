package com.eventshigh.nearme.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MyTicketObject;
import com.eventshigh.nearme.app.network.MyTicketsRequest;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

/**
 * @author shubham
 * @since 15/6/16.
 */

public class MyTicketsFragment extends Fragment {


    private View topProgressBar;
    private View retryView;
    private View noMyEventsView;
    private AutofitRecyclerView myTicketsList;
    Context context;

    Account account;
    LinearLayout verifyPhnLayout;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_tickets, container, false);
        myTicketsList = (AutofitRecyclerView) view.findViewById(R.id.my_tickets_grid);
        // More views.
        noMyEventsView = view.findViewById(R.id.no_tickets_view);
        topProgressBar = view.findViewById(R.id.top_progress_bar);
        retryView = view.findViewById(R.id.view_retry);
        view.findViewById(R.id.retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchData(false);
            }
        });
        //phone verify
        account = new Account(context);
        verifyPhnLayout = (LinearLayout) view.findViewById(R.id.verify_phn_layout);
        (view.findViewById(R.id.verify_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyClicked();
            }
        });

        return view;
    }

    public void verifyClicked() {
        startActivity(new Intent(context, PhoneLoginActivity.class));
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Setup the refresh on swipe down.
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(false);
                fetchData(true);// bypass cache
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        swipeRefreshLayout.setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchData(false);
    }

    private void fetchData(boolean shouldCache) {
        if (Utils.checkIfStringEmpty(account.getUserInfo().email)) {
            verifyPhnLayout.setVisibility(View.VISIBLE);
        } else {
            verifyPhnLayout.setVisibility(View.GONE);
            topProgressBar.setVisibility(View.VISIBLE);
            retryView.setVisibility(View.GONE);
            noMyEventsView.setVisibility(View.GONE);
            MyTicketsRequest.submit(getActivity(), Request.Priority.IMMEDIATE, this, shouldCache, mTicketsListener, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    Toast.makeText(context, R.string.failed_load, Toast.LENGTH_SHORT).show();
                    topProgressBar.setVisibility(View.GONE);
                    retryView.setVisibility(View.VISIBLE);
                }
            }, false);
        }
    }

    private Response.Listener<List<MyTicketObject>> mTicketsListener = new Response.Listener<List<MyTicketObject>>() {
        @Override
        public void onResponse(List<MyTicketObject> tickets, boolean isIntermediate) {
            if (tickets != null) setAdapterData(tickets);
            if (tickets == null || tickets.size() == 0) noMyEventsView.setVisibility(View.VISIBLE);
            topProgressBar.setVisibility(View.GONE);


        }
    };

    EventsAdapter adapter;

    public void setAdapterData(List<MyTicketObject> objs) {
        adapter = new EventsAdapter((BaseContextActivity) getActivity());
        adapter.setOnMyTicketClickListener(myTicketItemClickedListener);
        myTicketsList.setAdapter(adapter);
        // adapter.setMyTicketsData(objs, this);
    }

    public static int clickPosition = -1;
    EventsAdapter.OnMyTicketItemClickedListener myTicketItemClickedListener = new EventsAdapter.OnMyTicketItemClickedListener() {
        @Override
        public void onItemClicked(int pos) {
            if (clickPosition == -1) {
                clickPosition = pos;
            } else if (clickPosition == pos) {
                clickPosition = -1;
            } else {
                clickPosition = pos;
            }
            adapter.notifyDataSetChanged();
        }
    };
}
