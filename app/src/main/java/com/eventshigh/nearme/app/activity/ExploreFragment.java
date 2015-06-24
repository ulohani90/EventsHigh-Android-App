package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.Locality;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest.EventCollection;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

/**
 * Fragment to show explore by categories.
 */
public class ExploreFragment extends BaseEventsFragment {
    public static final String[] EXPLORE_TAGS = {
            EventsHighEndpoints.QUERY_FEATURED,
            EventCategory.PARTIES.categoryName,
            EventCategory.THEATRE.categoryName,
            EventCategory.MUSIC.categoryName,
            EventCategory.KIDS_ENTERTAINMENT.categoryName,
            EventCategory.TECH.categoryName,
            EventCategory.SPORTS.categoryName,
            EventCategory.HEALTH_WELLNESS.categoryName,
            EventCategory.DANCE.categoryName,
            EventCategory.ART.categoryName,
            EventCategory.FOOD.categoryName,
            EventCategory.LITERATURE.categoryName
    };

    public static final String[] EXPLORE_TAGS_BANGALORE = {
            EventsHighEndpoints.QUERY_FEATURED,
            EventCategory.PARTIES.categoryName,
            "Free",
            EventCategory.THEATRE.categoryName,
            EventCategory.MUSIC.categoryName,
            EventCategory.KIDS_ENTERTAINMENT.categoryName,
            EventCategory.TECH.categoryName,
            EventCategory.SPORTS.categoryName,
            EventCategory.HEALTH_WELLNESS.categoryName,
            EventCategory.DANCE.categoryName,
            EventCategory.ART.categoryName,
            EventCategory.FOOD.categoryName,
            EventCategory.LITERATURE.categoryName,
            "Workshops"
    };

    public static ExploreFragment getInstance(EventsContext eventsContext) {
        ExploreFragment fragment = new ExploreFragment();
        fragment.setArguments(getArgs(eventsContext, false, false));
        return fragment;
    }

    private EventsAdapter eventsAdapter;
    private View topProgressBar;
    private AutofitRecyclerView exploreGridView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        eventsAdapter = new EventsAdapter(this);
        exploreGridView = (AutofitRecyclerView) view.findViewById(R.id.explore_grid);
        exploreGridView.setEventsAdapter(eventsAdapter);
        exploreGridView.addOnScrollListener(new HideActionBarOnScroll(activity));

        topProgressBar = view.findViewById(R.id.top_progress_bar);
        topProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();

        ViewPager eventsPager = (ViewPager) exploreGridView.findViewById(R.id.events_pager);
        if (eventsPager != null) {
            eventsPager.getAdapter().notifyDataSetChanged();
        }
    }

    @Override
    protected void refresh() {
        FeaturedEventsRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                false, mFetcherCallBack, mErrorListener);
    }

    private Listener<EventCollection> mFetcherCallBack = new Listener<EventCollection>() {
        @Override
        public void onResponse(EventCollection eventCollection, boolean isIntermediate) {
            topProgressBar.setVisibility(isIntermediate ? View.VISIBLE : View.GONE);
            eventsAdapter.setExploreCategories(eventCollection,
                Locality.getLocalities(eventsContext.city),
                eventsContext.city == City.BANGALORE ? EXPLORE_TAGS_BANGALORE : EXPLORE_TAGS);
        }
    };

    private ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);

            if (eventsAdapter.getItemCount() == 0) {
                eventsAdapter.setExploreCategories(null,
                    Locality.getLocalities(eventsContext.city),
                    eventsContext.city == City.BANGALORE ? EXPLORE_TAGS_BANGALORE : EXPLORE_TAGS);
            }
        }
    };
}
