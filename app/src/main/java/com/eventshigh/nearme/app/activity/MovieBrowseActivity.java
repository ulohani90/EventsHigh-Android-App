package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ProgressBar;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.network.BrowseMoviesRequest;
import com.eventshigh.nearme.app.network.OffersRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;

import java.lang.reflect.Array;
import java.util.ArrayList;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * Created by umesh on 08/05/16.
 */
public class MovieBrowseActivity extends BaseContextActivity {


    public static final String FROM_NOTIFICATION_PARAM = "from_notification";

    public static String TAB_NAME = "tab_name";
    ViewPager pager;
    ProgressBar topProgressBar;

    BrowseMoviesRequest.MovieBrowseListobject moviesObject;
    Account account;
    TabLayout tabsView;

    private View retryView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_browse_movies);
        topProgressBar = (ProgressBar) findViewById(R.id.top_progress_bar);
        tabsView = (TabLayout) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.view_pager);
        retryView = findViewById(R.id.view_retry);
        findViewById(R.id.retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportActionToAnalytics("retry");
                makeServerRequest();
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Movies");
        account = new Account(this);
        tabsView.setVisibility(View.GONE);

        makeServerRequest();
        defalutTab = 0;
    }


    @Override
    public View getViewForSnackbar() {
        return null;
    }

    @Override
    protected void onResume() {
        setLightToolbarIcons();
        super.onResume();
    }

    private void setLightToolbarIcons() {
        toolbar.post(new Runnable() {
            @Override
            @SuppressWarnings("deprecation")
            public void run() {
                ToolbarColorizeHelper.colorizeToolbar(toolbar,
                        getResources().getColor(android.R.color.white), MovieBrowseActivity.this);
            }
        });
    }

    public void makeServerRequest() {
        topProgressBar.setVisibility(View.VISIBLE);
        retryView.setVisibility(View.GONE);
        BrowseMoviesRequest.submit(this, account.getLastCity(), Request.Priority.IMMEDIATE, this, false, mListener, mErrorListener);
    }

    private Response.Listener<BrowseMoviesRequest.MovieBrowseListobject> mListener = new Response.Listener<BrowseMoviesRequest.MovieBrowseListobject>() {
        @Override
        public void onResponse(BrowseMoviesRequest.MovieBrowseListobject listObject, boolean isIntermediate) {
            moviesObject = listObject;
            topProgressBar.setVisibility(View.GONE);
            retryView.setVisibility(View.GONE);
            setAdapter();
        }
    };


    private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);

            topProgressBar.setVisibility(View.GONE);
            retryView.setVisibility(View.VISIBLE);
            VolleyHelper.log(MovieBrowseActivity.this, volleyError);
        }
    };

    int defalutTab;

    private void setAdapter() {
        if (getIntent() != null & getIntent().hasExtra(TAB_NAME)) {
            String tabName = getIntent().getStringExtra(TAB_NAME);
            if (tabName.equalsIgnoreCase("All")) {
                defalutTab = 0;
            } else {
                for (int i = 0; i < moviesObject.languages.size(); i++) {
                    if (moviesObject.languages.get(i).equalsIgnoreCase(tabName)) {
                        defalutTab = i + 1;
                    }

                }
            }
        }
        MoviePagerAdapter adapter = new MoviePagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);
        pager.setCurrentItem(defalutTab, false);
        tabsView.setVisibility(View.VISIBLE);
        tabsView.setupWithViewPager(pager);
        tabsView.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabsView.setTabGravity(TabLayout.GRAVITY_FILL);
        tabsView.setupWithViewPager(pager);
        tabsView.setScrollPosition(defalutTab, 0, true);
        TabLayout.Tab tab = tabsView.getTabAt(defalutTab);
        if (tab != null) {
            tab.select();
        }

    }

    public class MoviePagerAdapter extends FragmentStatePagerAdapter implements ViewPager.OnPageChangeListener {

        public MoviePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            if (position == 0) {
                bundle.putParcelableArrayList("movies", moviesObject.movies);
                bundle.putParcelableArrayList("upcoming_movies", moviesObject.upcomingMovies);
            } else {
                bundle.putParcelableArrayList("movies", getLanguageBasedMovies(moviesObject.languages.get(position - 1), moviesObject.movies));
                bundle.putParcelableArrayList("upcoming_movies", getLanguageBasedMovies(moviesObject.languages.get(position - 1), moviesObject.upcomingMovies));
            }


            return MovieListingFragment.newInstance(bundle);
        }

        @Override
        public int getCount() {
            return moviesObject.languages.size() + 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return (position == 0) ? "All" : moviesObject.languages.get(position - 1);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            reportActionToAnalytics("movie_browse_tab_change", (position == 0) ? "All" : moviesObject.languages.get(position - 1));
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    public ArrayList<MovieDetailObject> getLanguageBasedMovies(String language, ArrayList<MovieDetailObject> movies) {
        ArrayList<MovieDetailObject> result = new ArrayList<>();
        for (MovieDetailObject obj : movies) {
            for (String launguageObj : obj.getMovieInfo().getLaunguages()) {
                if (launguageObj.equalsIgnoreCase(language)) {
                    result.add(obj);
                    break;
                }
            }

        }
        return result;
    }

    @Override
    public void onBackPressed() {
        if (getIntent().getBooleanExtra(FROM_NOTIFICATION_PARAM, false)) {
            Intent intent = new Intent(this, LaunchActivity.class);
            startActivity(intent);
        }
        super.onBackPressed();

    }

}
