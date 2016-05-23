package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MovieUserReviewObject;
import com.eventshigh.nearme.app.network.MovieDetailRequest;
import com.eventshigh.nearme.app.network.MyReviewsRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.user.Account;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 29/04/16.
 */
public class MovieDetailActivity extends BaseContextActivity implements ViewPager.OnPageChangeListener, View.OnClickListener {

    Toolbar toolbar;

    ViewPager pager;

    ImageView backArrow;


    public static final String FROM_NOTIFICATION_PARAM = "from notification";
    public static final String MOVIE_PARAM = "movie";

    public static final String MOVIE_ID = "movie_id";
    public static final String CRITICS_REVIEWS = "critic reviews";
    public static final String SHOWTIMES = "showtimes";
    public static final String MOVIE_INFO = "movie_info";
    public static final String USER_REVIEWS = "user reviews";
    public static final String MY_REVIEW = "my_review";
    public static final String MOVIE_DETAIL_OBJECT = "movie_detail_object";

    private final String CRITICS = "reviews";
    private final String SHOWTIME = "showtime";
    private final String INFO = "info";
    public ArrayList<String> TABS;

    int movieId = 1798;
    ProgressBar topProgressBar;
    FloatingActionButton fabWriteReviews;

    private View retryView;
    private MovieDetailObject movieDetailOject;
    Account account;

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

        //write review
        fabWriteReviews = (FloatingActionButton) findViewById(R.id.fab_write_review);
        pager.addOnPageChangeListener(this);
        fabWriteReviews.setOnClickListener(this);
        fabWriteReviews.setVisibility(View.GONE);

        account = new Account(this);

        if (getIntent().hasExtra(MOVIE_PARAM)) {
            MovieDetailObject movie = getIntent().getParcelableExtra(MOVIE_PARAM);
            movieDetailOject = movie;
            populateView(movie);
        } else {
            movieId = getIntent().getIntExtra(MOVIE_ID, -1);
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
        topProgressBar.setVisibility(View.VISIBLE);

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
            movieDetailOject = movie;
            makeMyReviewsServerRequest();
            populateView(movie);
        }
    };

    private Response.Listener<List<MovieUserReviewObject>> mReviewListener = new Response.Listener<List<MovieUserReviewObject>>() {
        @Override
        public void onResponse(List<MovieUserReviewObject> reviews, boolean isIntermediate) {
            findReviewsByUserForMovie(reviews);
            populateView(movieDetailOject);
        }
    };


    MovieUserReviewObject myReview;

    public void findReviewsByUserForMovie(List<MovieUserReviewObject> reviews) {
        for (MovieUserReviewObject obj : reviews) {
            if (obj.getReviewerId().equalsIgnoreCase(account.getUserInfo().phoneNo) && obj.getReviewedEntityId().equalsIgnoreCase(movieDetailOject.getMovieInfo().getId() + "")) {
                myReview = obj;
                break;
            }
        }
    }

    public void makeMyReviewsServerRequest() {
        MyReviewsRequest.submit(this, account.getUserInfo().phoneNo, Request.Priority.IMMEDIATE, this, false, mReviewListener, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(MovieDetailActivity.this, R.string.failed_load,
                        Toast.LENGTH_SHORT).show();
                VolleyHelper.log(MovieDetailActivity.this, volleyError);
                finish();
            }
        });
    }

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
        TABS.add(USER_REVIEWS);

        movieBg.setVisibility(View.VISIBLE);
        playVideo.setVisibility(View.VISIBLE);
        Glide.with(this).load(movie.getMovieInfo().getImg_url())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                .into(movieBg);
        if (movie.getMovieInfo().getYoutubeVideoId() != null && movie.getMovieInfo().getYoutubeVideoId().length() > 0) {
            playVideo.setVisibility(View.VISIBLE);
        } else {
            playVideo.setVisibility(View.GONE);
        }
        movieBg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (movie.getMovieInfo().getYoutubeVideoId() != null && movie.getMovieInfo().getYoutubeVideoId().length() > 0) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(movie.getMovieInfo().getYoutubeVideoId())));
                }
            }
        });
        scrimView.setVisibility(View.VISIBLE);
        pager.setVisibility(View.VISIBLE);
        MovieDetailPagerAdapter adapter
                = new MovieDetailPagerAdapter(getSupportFragmentManager(), movie);
        pager.setAdapter(adapter);
        TabLayout tabsView = (TabLayout) findViewById(R.id.tabs);
        tabsView.setVisibility(View.VISIBLE);
        tabsView.setTabGravity(TabLayout.GRAVITY_FILL);
        tabsView.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabsView.setupWithViewPager(pager);
        tabsView.setScrollPosition(0, 0, true);
        (findViewById(R.id.share)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareMovie(movie);
            }
        });
    }


    public class MovieDetailPagerAdapter extends FragmentPagerAdapter implements ViewPager.OnPageChangeListener {

        MovieDetailObject movieObject;

        public MovieDetailPagerAdapter(FragmentManager fm, MovieDetailObject movie) {
            super(fm);
            this.movieObject = movie;
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            if (TABS.get(position).equalsIgnoreCase(INFO)) {

                bundle.putParcelable(MOVIE_INFO, movieObject.getMovieInfo());
                return MovieInfoLayoutFragment.newInstance(bundle);
            }
            if (TABS.get(position).equalsIgnoreCase(CRITICS_REVIEWS)) {
                bundle.putParcelableArrayList(CRITICS_REVIEWS, movieObject.getReviews());
                return CriticsReviewsFragment.newInstance(bundle);
            } else if (TABS.get(position).equalsIgnoreCase(SHOWTIMES)) {
                bundle.putParcelableArrayList(SHOWTIMES, movieObject.getShowtimes());
                return ShowtimeFragment.newInstance(bundle);
            } else if (TABS.get(position).equalsIgnoreCase(USER_REVIEWS)) {
                if (myReview != null)
                    bundle.putParcelable(MY_REVIEW, myReview);
                bundle.putParcelableArrayList(USER_REVIEWS, movieObject.getUserReviews());
                return UserReviewsFragment.newInstance(bundle);
            } else {
                return null;
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

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            reportActionToAnalytics("movie_detail_tab_change", TABS.get(position));
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getBooleanExtra(FROM_NOTIFICATION_PARAM, false)) {
            Intent intent = new Intent(this, LaunchActivity.class);
            startActivity(intent);
        }
        super.onBackPressed();

    }


    //OnPageListerner Methods
    @Override
    public void onPageSelected(int position) {
        if (myReview == null)
            animateFab(position);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        Log.e("", state + " state changed");
    }


    protected void animateFab(int position) {
        if (!TABS.get(position).equalsIgnoreCase(USER_REVIEWS)) {
            if (fabWriteReviews.getVisibility() == View.VISIBLE) {
                fabWriteReviews.setVisibility(View.GONE);
                TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0, 250);
                translateAnimation.setDuration(300);
                fabWriteReviews.startAnimation(translateAnimation);
            }
        } else {
            //Set First Reviewer Text Visible
            fabWriteReviews.clearAnimation();
            fabWriteReviews.setVisibility(View.VISIBLE);
            TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 250, 0);
            translateAnimation.setDuration(300);
            fabWriteReviews.startAnimation(translateAnimation);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_write_review:
                if (account.getUserInfo().phoneNo == null || account.getUserInfo().name == null) {
                    PhoneVerificationDialog.show(this, R.string.ui_verify_phone, R.string.ui_phone_verify_book);
                    return;
                }
                Intent i = new Intent(this, WriteReviewActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable(MOVIE_DETAIL_OBJECT, movieDetailOject);
                i.putExtras(bundle);
                startActivity(i);
                overridePendingTransition(R.anim.animate_slide_up, R.anim.stay);
                break;
        }
    }


}
