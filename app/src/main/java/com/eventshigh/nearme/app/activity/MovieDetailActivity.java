package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.network.MovieDetailRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.view.ZCustomFlowLayout;

import java.util.ArrayList;

/**
 * Created by umesh on 29/04/16.
 */
public class MovieDetailActivity extends BaseContextActivity {

    Toolbar toolbar;

    ViewPager pager;

    ImageView backArrow;

    public static final String MOVIE_ID = "movie_id";
    public static final String CRITICS_REVIEWS = "reviews";
    public static final String SHOWTIMES = "showtimes";
    public static final String MOVIE_INFO = "movie_info";

    private final String CRITICS = "reviews";
    private final String SHOWTIME = "showtime";
    private final String INFO = "info";
    public ArrayList<String> TABS;

    int movieId = 1798;
    ProgressBar topProgressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_movie_detail_layout);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        addToolbarView();
        toolbar.setBackgroundColor(Color.TRANSPARENT);

        topProgressBar = (ProgressBar) findViewById(R.id.top_progress_bar);
        //setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        pager = (ViewPager) findViewById(R.id.view_pager);
        backArrow = (ImageView) findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        topProgressBar.setVisibility(View.VISIBLE);
        if(getIntent().hasExtra("movie")) {
            MovieDetailObject movie = getIntent().getParcelableExtra("movie");
            populateView(movie);
        }else{
            movieId = getIntent().getIntExtra("movie_id",-1);
            if (movieId != -1) {
                makeServerRequest();
            } else {
                Toast.makeText(MovieDetailActivity.this, R.string.failed_load,
                        Toast.LENGTH_SHORT).show();

                finish();
            }
        }
    }


    ImageView movieBg, playVideo;
    LinearLayout headerParent;
    View scrimView;

    /**
     * Add layout to the toolbar
     */
    public void addToolbarView() {
        View view = LayoutInflater.from(this).inflate(R.layout.activity_movie_detail, toolbar, false);
        movieBg = (ImageView) view.findViewById(R.id.movie_bg);
        headerParent = (LinearLayout) view.findViewById(R.id.header_parent);
        playVideo = (ImageView) view.findViewById(R.id.play_video);
        scrimView = view.findViewById(R.id.scrim_view);
        toolbar.addView(view);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    public void makeServerRequest() {
        MovieDetailRequest.submit(this, movieId, Request.Priority.IMMEDIATE, mEventListener,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Toast.makeText(MovieDetailActivity.this, R.string.failed_load,
                                Toast.LENGTH_SHORT).show();
                        VolleyHelper.log(MovieDetailActivity.this, volleyError);
                        finish();
                    }
                });
    }

    private Response.Listener<MovieDetailObject> mEventListener = new Response.Listener<MovieDetailObject>() {
        @Override
        public void onResponse(final MovieDetailObject movie, boolean isIntermediate) {
            populateView(movie);
        }
    };

    public void populateView(final MovieDetailObject movie) {
        topProgressBar.setVisibility(View.GONE);
        headerParent.setVisibility(View.VISIBLE);
        TABS = new ArrayList<>();

        TABS.add(INFO);
        if (movie.getReviews() != null && movie.getReviews().size() > 0) {
            TABS.add(CRITICS_REVIEWS);
        }
        if (movie.getShowtimes() != null && movie.getShowtimes().size() > 0) {
            TABS.add(SHOWTIMES);
        }
        movieBg.setVisibility(View.VISIBLE);
        playVideo.setVisibility(View.VISIBLE);
        Glide.with(this).load(movie.getMovieInfo().getImg_url())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                .into(movieBg);
        movieBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(movie.getMovieInfo().getYoutubeVideoId())));
            }
        });
        scrimView.setVisibility(View.VISIBLE);
        pager.setVisibility(View.VISIBLE);
        MovieDetailPagerAdapter adapter = new MovieDetailPagerAdapter(getSupportFragmentManager(), movie);
        pager.setAdapter(adapter);
        TabLayout tabsView = (TabLayout) findViewById(R.id.tabs);
        tabsView.setVisibility(View.VISIBLE);
        tabsView.setupWithViewPager(pager);
        tabsView.setTabMode(TabLayout.MODE_FIXED);
        tabsView.setTabGravity(TabLayout.GRAVITY_FILL);
        tabsView.setupWithViewPager(pager);
        tabsView.setScrollPosition(0, 0, true);
        (findViewById(R.id.share)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareMovie(movie);
            }
        });
    }


    public class MovieDetailPagerAdapter extends FragmentPagerAdapter {

        MovieDetailObject movieObject;

        public MovieDetailPagerAdapter(FragmentManager fm, MovieDetailObject movie) {
            super(fm);
            this.movieObject = movie;
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            if (position == 0) {

                bundle.putParcelable(MOVIE_INFO, movieObject.getMovieInfo());
                return MovieInfoLayoutFragment.newInstance(bundle);
            } else if (position == 1) {
                bundle.putParcelableArrayList(CRITICS_REVIEWS, movieObject.getReviews());
                return CriticsReviewsFragment.newInstance(bundle);
            } else {
                bundle.putParcelableArrayList(SHOWTIMES, movieObject.getShowtimes());
                return ShowtimeFragment.newInstance(bundle);
            }

        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TABS.get(position);
        }

        @Override
        public int getCount() {
            return TABS.size();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
