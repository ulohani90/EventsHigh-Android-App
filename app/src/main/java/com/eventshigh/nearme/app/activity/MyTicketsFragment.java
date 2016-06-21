package com.eventshigh.nearme.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MyTicketObject;
import com.eventshigh.nearme.app.network.MyTicketsRequest;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shubham
 * @since 15/6/16.
 */

public class MyTicketsFragment extends Fragment {


    AutofitRecyclerView myTicketsList;


    int currentState = 0;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    public static MovieListingFragment newInstance(Bundle bundle) {
        MovieListingFragment fragment = new MovieListingFragment();
        fragment.setArguments(bundle);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_tickets, container, false);
        myTicketsList = (AutofitRecyclerView) view.findViewById(R.id.my_tickets_grid);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MyTicketsRequest.submit(getActivity(), Request.Priority.IMMEDIATE, this, true, mTicketsListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(getActivity(), R.string.failed_load,
                        Toast.LENGTH_SHORT).show();
            }
        },false);
    }

    private Response.Listener<List<MyTicketObject>> mTicketsListener = new Response.Listener<List<MyTicketObject>>() {
        @Override
        public void onResponse(List<MyTicketObject> tickets, boolean isIntermediate) {
            if(tickets != null)
            setAdapterData(tickets);
        }
    };

    EventsAdapter adapter;
    public void setAdapterData(List<MyTicketObject> objs) {
        adapter = new EventsAdapter((LaunchActivity)getActivity());
        adapter.setOnMyTicketClickListener(myTicketItemClickedListener);
        myTicketsList.setAdapter(adapter);
        adapter.setMyTicketsData(objs,this);
    }

    public static int clickPosition = -1;
    EventsAdapter.OnMyTicketItemClickedListener myTicketItemClickedListener = new EventsAdapter.OnMyTicketItemClickedListener() {
        @Override
        public void onItemClicked(int pos) {
            if(clickPosition == -1){
                clickPosition = pos;
            }else if(clickPosition == pos){
                clickPosition = -1;
            }else{
                clickPosition = pos;
            }
            adapter.notifyDataSetChanged();
        }
    };
}
