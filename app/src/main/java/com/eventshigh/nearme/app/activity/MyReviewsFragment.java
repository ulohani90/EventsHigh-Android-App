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
import com.android.volley.toolbox.RequestFuture;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.network.MultiEventsRequest;
import com.eventshigh.nearme.app.network.MultiMovieRequest;
import com.eventshigh.nearme.app.network.MyReviewsRequest;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author shubham
 * @since 27/6/16.
 */
public class MyReviewsFragment extends Fragment{

    private View topProgressBar;
    private View retryView;
    private View noMyEventsView;
    private AutofitRecyclerView myReviewsList;
    Context context;
    private EventsContext eventsContext;

    Account account;
    LinearLayout verifyPhnLayout;
    public static final String EVENT_CONTEXT = "event_context";

    public static MyReviewsFragment newInstance(EventsContext eventsContext, ArrayList<MovieUserReviewObject> movieUserReviewObjectList) {
        Bundle args = new Bundle();
        args.putParcelable(EVENT_CONTEXT, eventsContext);
        args.putParcelableArrayList("reviews", movieUserReviewObjectList);
        MyReviewsFragment fragment = new MyReviewsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_reviews, container, false);
        myReviewsList = (AutofitRecyclerView) view.findViewById(R.id.my_reviews_grid);
        // More views.
        noMyEventsView = view.findViewById(R.id.view_no_my_event);
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
        verifyPhnLayout = (LinearLayout)view.findViewById(R.id.verify_phn_layout);
        (view.findViewById(R.id.verify_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                verifyClicked();
            }
        });
        return view;
    }

    public void verifyClicked(){
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
    private boolean isRespoRecieved = false;
    List<MovieUserReviewObject> movieUserReviewObjects;

    private void fetchData(final boolean shouldByPassCache) {
        /*if(Utils.checkIfStringEmpty(account.getUserInfo().phoneNo)){
            verifyPhnLayout.setVisibility(View.VISIBLE);
        }else{
            verifyPhnLayout.setVisibility(View.GONE);
            topProgressBar.setVisibility(View.VISIBLE);
            retryView.setVisibility(View.GONE);
            noMyEventsView.setVisibility(View.GONE);
            MyReviewsRequest.submit(context,account.getUserInfo().phoneNo, Request.Priority.IMMEDIATE, this, shouldByPassCache,
                    new Response.Listener<List<MovieUserReviewObject>>(){
                        @Override
                        public void onResponse(List<MovieUserReviewObject> movieUserReviewObjects, boolean b) {

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    Toast.makeText(getActivity(), R.string.failed_load,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
*/
        if (movieUserReviewObjects != null){
            //setAdapterData(movieUserReviewObjects);
            fetchDetailedInfo(shouldByPassCache, movieUserReviewObjects);
        }
        if(movieUserReviewObjects == null || movieUserReviewObjects.size() == 0)noMyEventsView.setVisibility(View.VISIBLE);
        topProgressBar.setVisibility(View.GONE);


    }

    private void fetchDetailedInfo(boolean shouldBypassCache, List<MovieUserReviewObject> movieUserReviewObjectList){
        movieUserReviewObjects = movieUserReviewObjectList;
        isRespoRecieved = false;
        RequestFuture<List<MovieDetailObject>> moviesList = RequestFuture.newFuture();
        MultiMovieRequest.submit(context, eventsContext, getMovies(movieUserReviewObjectList),
                Request.Priority.HIGH, null, shouldBypassCache, true, new Response.Listener<List<MovieDetailObject>>() {
                    @Override
                    public void onResponse(List<MovieDetailObject> movieDetailObjects, boolean b) {
                        updateMovies(movieDetailObjects);
                        if(isRespoRecieved){
                            setAdapterData(movieUserReviewObjects);
                        }else{
                            isRespoRecieved = true;
                        }
                    }
                }, moviesList);

        RequestFuture<List<Event>> favEvents = RequestFuture.newFuture();
        MultiEventsRequest.submit(context, eventsContext, getEvents(movieUserReviewObjectList),
                Request.Priority.HIGH, null, shouldBypassCache,true,false,new Response.Listener<List<Event>>() {
                    @Override
                    public void onResponse(List<Event> events, boolean b) {
                        updateEvents(events);
                        if(isRespoRecieved){
                            setAdapterData(movieUserReviewObjects);
                        }else{
                            isRespoRecieved = true;
                        }
                    }
                }, favEvents);
    }

    private List<String> getMovies(List<MovieUserReviewObject> movieUserReviewObjectList){
        List<String> movieList = new ArrayList<>();
        for(MovieUserReviewObject movieUserReviewObject:movieUserReviewObjectList){
            if(movieUserReviewObject.getReviewFor().equals("movie")){
                movieList.add(movieUserReviewObject.getReviewedEntityId());
            }
        }
        return movieList;
    }

    private List<String> getEvents(List<MovieUserReviewObject> movieUserReviewObjectList){
        List<String> eventList = new ArrayList<>();
        for(MovieUserReviewObject movieUserReviewObject: movieUserReviewObjectList){
            if(movieUserReviewObject.getReviewFor().equals("event"))
                eventList.add(movieUserReviewObject.getReviewedEntityId());
        }
        return eventList;
    }

    private void updateMovies(List<MovieDetailObject> movieDetailObjects){
        for(MovieDetailObject movieDetailObject: movieDetailObjects){
            for(MovieUserReviewObject movieUserReviewObject: movieUserReviewObjects){
                if(movieUserReviewObject.getReviewedEntityId().equalsIgnoreCase(movieDetailObject.getMovieInfo().getId()+"")){
                    movieUserReviewObject.setReviewedEntityImage(movieDetailObject.getMovieInfo().getImg_url());
                    movieUserReviewObject.setMovieDetailObject(movieDetailObject);
                }
            }
        }
    }

    private void updateEvents(List<Event> events){
        for(Event event: events){
            for(MovieUserReviewObject movieUserReviewObject: movieUserReviewObjects){
                if(movieUserReviewObject.getReviewedEntityId().equalsIgnoreCase(event.id)){
                    movieUserReviewObject.setReviewedEntityImage(event.imgUrl);
                    movieUserReviewObject.setReviewedEntityLocation(event.locality);
                    movieUserReviewObject.setEvent(event);
                }
            }
        }
    }

    EventsAdapter adapter;
    public void setAdapterData(List<MovieUserReviewObject> objs) {
        adapter = new EventsAdapter((BaseContextActivity) getActivity());
        myReviewsList.setAdapter(adapter);
        adapter.setMyReviewsData(objs);
    }

}
