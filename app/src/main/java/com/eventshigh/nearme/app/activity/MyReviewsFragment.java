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
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.network.MultiEventsRequest;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shubham
 * @since 27/6/16.
 */
public class MyReviewsFragment extends Fragment {

    private View topProgressBar;
    private View retryView;
    private TextView noMyEventsView;
    private AutofitRecyclerView myReviewsList;
    Context context;
    private EventsContext eventsContext;

    Account account;
    LinearLayout verifyPhnLayout;

    String userEmail;
    public static final String EVENT_CONTEXT = "event_context";
    public static final String USER_MOBILE_NO = "mobile_no";
    public static final String USER_EMAIL = "email";

    public static MyReviewsFragment newInstance(EventsContext eventsContext, ArrayList<MovieUserReviewObject> movieUserReviewObjectList, String userMobileNo) {
        Bundle args = new Bundle();
        args.putParcelable(EVENT_CONTEXT, eventsContext);
        args.putString(USER_EMAIL, userMobileNo);
        args.putParcelableArrayList("reviews", movieUserReviewObjectList);
        MyReviewsFragment fragment = new MyReviewsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        this.userEmail = getArguments().getString(USER_EMAIL);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_reviews, container, false);
        myReviewsList = (AutofitRecyclerView) view.findViewById(R.id.my_reviews_grid);
        // More views.
        noMyEventsView = (TextView) view.findViewById(R.id.view_no_my_event);
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

        eventsContext = getArguments().getParcelable(EVENT_CONTEXT);
        movieUserReviewObjects = getArguments().getParcelableArrayList("reviews");
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchData(false);
    }


    //util methods to fetch details of reviewed entity

    private boolean isEventRespoRecieved = false;

    List<MovieUserReviewObject> movieUserReviewObjects;

    private void fetchData(final boolean shouldByPassCache) {

        retryView.setVisibility(View.GONE);

        isEventRespoRecieved = false;
        if (movieUserReviewObjects != null && movieUserReviewObjects.size() != 0) {
            fetchDetailedInfo(shouldByPassCache, movieUserReviewObjects);
        }

        if (movieUserReviewObjects == null || movieUserReviewObjects.size() == 0) {
            noMyEventsView.setVisibility(View.VISIBLE);
            if (userEmail.equalsIgnoreCase(new Account(context).getUserInfo().email)) {
                noMyEventsView.setText(getString(R.string.no_review_text));
            } else {
                noMyEventsView.setText("No Reviews");
            }

            topProgressBar.setVisibility(View.GONE);
        }

    }

    private void fetchDetailedInfo(boolean shouldBypassCache, List<MovieUserReviewObject> movieUserReviewObjectList) {
        movieUserReviewObjects = movieUserReviewObjectList;

        isEventRespoRecieved = false;
        topProgressBar.setVisibility(View.VISIBLE);

        MultiEventsRequest.submit(context, eventsContext, getEvents(movieUserReviewObjectList),
                Request.Priority.HIGH, null, shouldBypassCache, true, false, new Response.Listener<List<Event>>() {
                    @Override
                    public void onResponse(List<Event> events, boolean b) {
                        updateEvents(events);
                        isEventRespoRecieved = true;

                        setAdapterData(movieUserReviewObjects);

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        isEventRespoRecieved = true;
                        topProgressBar.setVisibility(View.GONE);
                        retryView.setVisibility(View.VISIBLE);
                    }
                });
    }

    private List<String> getEvents(List<MovieUserReviewObject> movieUserReviewObjectList) {
        List<String> eventList = new ArrayList<>();
        for (MovieUserReviewObject movieUserReviewObject : movieUserReviewObjectList) {
            if (movieUserReviewObject.getReviewFor().equals("event"))
                eventList.add(movieUserReviewObject.getReviewedEntityId());
        }
        return eventList;
    }

    private void updateEvents(List<Event> events) {
        for (Event event : events) {
            for (MovieUserReviewObject movieUserReviewObject : movieUserReviewObjects) {
                if (movieUserReviewObject.getReviewedEntityId().equalsIgnoreCase(event.id)) {
                    movieUserReviewObject.setReviewedEntityImage(event.imgUrl);
                    movieUserReviewObject.setReviewedEntityLocation(event.locality);
                    movieUserReviewObject.setEvent(event);
                }
            }
        }
    }

    EventsAdapter adapter;

    public void setAdapterData(List<MovieUserReviewObject> objs) {
        topProgressBar.setVisibility(View.GONE);
        adapter = new EventsAdapter((BaseContextActivity) getActivity());
        myReviewsList.setAdapter(adapter);
        adapter.setMyReviewsData(objs);
    }

}
