package com.eventshigh.nearme.app.activity;

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
import com.eventshigh.nearme.app.user.Account;

import java.lang.reflect.Array;
import java.util.ArrayList;

import pl.snowdog.material.ui.ToolbarColorizeHelper;

/**
 * Created by umesh on 08/05/16.
 */
public class MovieBrowseActivity extends BaseContextActivity {

    ViewPager pager;
    ProgressBar topProgressBar;

    BrowseMoviesRequest.MovieBrowseListobject moviesObject;
    Account account;
    TabLayout tabsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_browse_movies);
        topProgressBar = (ProgressBar) findViewById(R.id.top_progress_bar);
         tabsView = (TabLayout) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.view_pager);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Movies");
        account = new Account(this);
        tabsView.setVisibility(View.GONE);
        topProgressBar.setVisibility(View.VISIBLE);
        makeServerRequest();

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
        BrowseMoviesRequest.submit(this, account.getLastCity(), Request.Priority.IMMEDIATE, this, false, mListener, mErrorListener);
    }

    private Response.Listener<BrowseMoviesRequest.MovieBrowseListobject> mListener = new Response.Listener<BrowseMoviesRequest.MovieBrowseListobject>() {
        @Override
        public void onResponse(BrowseMoviesRequest.MovieBrowseListobject listObject, boolean isIntermediate) {
            moviesObject = listObject;
            topProgressBar.setVisibility(View.GONE);
            setAdapter();
        }
    };




    private Response.ErrorListener mErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);
        }
    };


    private void setAdapter() {
        MoviePagerAdapter adapter = new MoviePagerAdapter(getSupportFragmentManager());
        pager.setAdapter(adapter);

        tabsView.setVisibility(View.VISIBLE);
        tabsView.setupWithViewPager(pager);
        tabsView.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabsView.setTabGravity(TabLayout.GRAVITY_FILL);
        tabsView.setupWithViewPager(pager);
        tabsView.setScrollPosition(0, 0, true);

    }

    public class MoviePagerAdapter extends FragmentStatePagerAdapter {

        public MoviePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            if(position == 0){
                bundle.putParcelableArrayList("movies",  moviesObject.movies);
                bundle.putParcelableArrayList("upcoming_movies",  moviesObject.upcomingMovies);
            }else{
                bundle.putParcelableArrayList("movies", getLanguageBasedMovies(moviesObject.languages.get(position-1), moviesObject.movies));
                bundle.putParcelableArrayList("upcoming_movies", getLanguageBasedMovies(moviesObject.languages.get(position-1), moviesObject.upcomingMovies));
            }


            return MovieListingFragment.newInstance(bundle);
        }

        @Override
        public int getCount() {
            return moviesObject.languages.size()+1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return (position == 0)?"All":moviesObject.languages.get(position-1);
        }
    }

    public ArrayList<MovieDetailObject> getLanguageBasedMovies(String language, ArrayList<MovieDetailObject> movies) {
        ArrayList<MovieDetailObject> result = new ArrayList<>();
        for (MovieDetailObject obj : movies) {
            for(String launguageObj:obj.getMovieInfo().getLaunguages()){
                if (launguageObj.equalsIgnoreCase(language)) {
                    result.add(obj);
                    break;
                }
            }

        }
        return result;
    }


}
