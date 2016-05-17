package com.eventshigh.nearme.app.activity;

import android.content.Context;
import android.graphics.Movie;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.ArrayList;

/**
 * Created by umesh on 08/05/16.
 */
public class MovieListingFragment extends Fragment {


    AutofitRecyclerView moviesList;

    ArrayList<MovieDetailObject> movies;
    ArrayList<MovieDetailObject> upcomingMovies;
    TextView upcoming, showing;

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
        View view = inflater.inflate(R.layout.fragment_movies_list, container, false);
        moviesList = (AutofitRecyclerView) view.findViewById(R.id.movies_grid);
        upcoming = (TextView) view.findViewById(R.id.upcoming);
        showing = (TextView) view.findViewById(R.id.showing);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().containsKey("movies")) {
                movies = getArguments().getParcelableArrayList("movies");
            }
            if (getArguments().containsKey("upcoming_movies")) {
                upcomingMovies = getArguments().getParcelableArrayList("upcoming_movies");
            }


            if (movies == null || (movies != null && movies.size() == 0)) {
                showing.setVisibility(View.GONE);
            } else {
                showing.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currentState != 0) {
                            showing.setSelected(true);
                            if (upcoming.isShown())
                                upcoming.setSelected(false);
                            setAdapterData(movies);
                            currentState = 0;
                        }
                    }
                });
            }
            if (upcomingMovies == null || (upcomingMovies != null && upcomingMovies.size() == 0)) {
                upcoming.setVisibility(View.GONE);
            } else {
                upcoming.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (currentState != 1) {
                            showing.setSelected(false);
                            upcoming.setSelected(true);
                            setAdapterData(upcomingMovies);
                            currentState = 1;
                        }
                    }
                });
            }
            showing.setSelected(true);
            currentState = 0;
            setAdapterData(movies);
        }

    }

    public void setAdapterData(ArrayList<MovieDetailObject> objs) {
        EventsAdapter adapter = new EventsAdapter((MovieBrowseActivity) getActivity());
        moviesList.setAdapter(adapter);
        adapter.setMoviesListData(objs, null, false, true);

    }
}
