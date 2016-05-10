package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MovieInfoObject;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.ArrayList;

/**
 * Created by umesh on 06/05/16.
 */
public class MovieInfoLayoutFragment extends Fragment{


    private EventsAdapter eventsAdapter;

    MovieInfoObject infoObject;

    AutofitRecyclerView reviewList;

    public static MovieInfoLayoutFragment newInstance(Bundle bundle){
        MovieInfoLayoutFragment fragment = new MovieInfoLayoutFragment();
        fragment.setArguments(bundle);
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view  = inflater.inflate(R.layout.fragment_movies,container,false);
        view.findViewById(R.id.top_progress_bar).setVisibility(View.GONE);
        reviewList = (AutofitRecyclerView)view.findViewById(R.id.event_grid);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(getArguments()!=null && getArguments().getParcelable(MovieDetailActivity.MOVIE_INFO)!=null){
            infoObject = getArguments().getParcelable(MovieDetailActivity.MOVIE_INFO);
        }
        eventsAdapter = new EventsAdapter((MovieDetailActivity) getActivity());
        reviewList.setVerticalSpacing(0);
        reviewList.setAdapter(eventsAdapter);
        //reviewList.addOnScrollListener(new HideActionBarOnScroll((LaunchActivity) getActivity()));
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setEnabled(false);

        eventsAdapter.setInfoObject(infoObject);
    }
}
