package com.eventshigh.nearme.app.activity;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Locality;
import com.eventshigh.nearme.app.data.stream.PointsObject;
import com.eventshigh.nearme.app.network.OffersRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.Signer;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;

/**
 * Created by umesh on 15/04/16.
 */
public class MyOffersListFragment extends Fragment {

    public static MyOffersListFragment newInstance(Bundle args) {
        MyOffersListFragment fragment = new MyOffersListFragment();
        fragment.setArguments(args);
        return fragment;
    }


    private EventsAdapter eventsAdapter;
    private View topProgressBar;

    Account account;

    long walletPoints;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);
        return view;
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        eventsAdapter = new EventsAdapter((LaunchActivity) getActivity());
        AutofitRecyclerView exploreGridView = (AutofitRecyclerView) view.findViewById(R.id.event_grid);
        exploreGridView.setAdapter(eventsAdapter);
        exploreGridView.addOnScrollListener(new HideActionBarOnScroll((LaunchActivity) getActivity()));

        topProgressBar = view.findViewById(R.id.top_progress_bar);
        topProgressBar.setVisibility(View.VISIBLE);

        // Setup the refresh on swipe down.
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //activity.reportActionToAnalytics("swipeRefresh", eventsContext.toString());
                topProgressBar.setVisibility(View.VISIBLE);
                swipeRefreshLayout.setRefreshing(false);
                getUserPoints(true);

            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);
        account = new Account(getActivity());
    }

    @Override
    public void onStart() {
        super.onStart();
        //makeServerRequest(false);
        if (account.getUserInfo().isVerified) {
            getUserPoints(false);
        } else {
            makeServerRequest(false);
        }


    }


    public void getUserPoints(final boolean shouldByPassCache) {
        Uri requestUrl = UpdateAccountInfoService.getBaseUri(getActivity(), "getWalletPoints")
                .build();
        try {
            VolleyHelper.addToRequestQueue(getActivity(),
                    new JsonObjectRequest(Request.Method.GET, Signer.sign(requestUrl).toString(), null,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject s, boolean isIntermediate) {
                                    if (getActivity() != null) {
                                        try {
                                            walletPoints = s.getLong("points");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        makeServerRequest(shouldByPassCache);
                                    }
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    if (getActivity() != null)
                                        getUserPoints(shouldByPassCache);
                                }
                            }
                    )
            );
        } catch (IOException | GeneralSecurityException e) {
        }
    }


    public void makeServerRequest(boolean shouldByPassCache) {
        City city = new Account(getActivity()).getLastCity();
        OffersRequest.submit(getActivity(), city, Request.Priority.IMMEDIATE, this, shouldByPassCache, mListener, mErrorListener);

    }

    private Response.Listener<OffersRequest.OffersPointsObject> mListener = new Response.Listener<OffersRequest.OffersPointsObject>() {
        @Override
        public void onResponse(OffersRequest.OffersPointsObject offersPointsObject, boolean isIntermediate) {
            topProgressBar.setVisibility(View.GONE);
            eventsAdapter.setOffers(offersPointsObject.offers, walletPoints);
        }
    };

    private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);


        }
    };
}
