package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.graphics.Movie;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MovieReviewObject;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.ArrayList;

public class UserReviewsFragment extends android.support.v4.app.Fragment {

    private EventsAdapter eventsAdapter;

    private String movieId;

    AutofitRecyclerView reviewList;

    TextView tvFirstReviewText;


    public static UserReviewsFragment newInstance(Bundle bundle) {
        UserReviewsFragment fragment = new UserReviewsFragment();
        fragment.setArguments(bundle);
        return fragment;

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie_user_review, container, false);
        tvFirstReviewText = (TextView) view.findViewById(R.id.tv_first_movie_review_text);
        reviewList = (AutofitRecyclerView) view.findViewById(R.id.event_grid);
        return view;
    }

    ArrayList<MovieUserReviewObject> reviews;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reviews = new ArrayList<>();

        if (getArguments() != null && getArguments().getParcelableArrayList(MovieDetailActivity.USER_REVIEWS) != null) {
            reviews = getArguments().getParcelableArrayList(MovieDetailActivity.USER_REVIEWS);
            movieId = getArguments().getString(MovieDetailActivity.MOVIE_ID);

        }
        eventsAdapter = new EventsAdapter((MovieDetailActivity) getActivity());
        reviewList.setAdapter(eventsAdapter);
        //reviewList.addOnScrollListener(new HideActionBarOnScroll((LaunchActivity) getActivity()));
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setEnabled(false);
        if (reviews.size() == 0) {
            tvFirstReviewText.setVisibility(View.VISIBLE);
        } else {
            tvFirstReviewText.setVisibility(View.GONE);
        }
        eventsAdapter.setUserMovieReviews(reviews, movieId);
    }


    @Override
    public void onResume() {
        super.onResume();
    }
}
