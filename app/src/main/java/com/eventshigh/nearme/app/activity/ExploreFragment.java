package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.ui.EventsAdapter;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

/**
 * Fragment to show explore by categories.
 */
public class ExploreFragment extends Fragment {
    private BaseContextActivity activity;

    public static final String[] EXPLORE_TAGS = {
            IntentUtils.QUERY_ALL,
            EventCategory.MUSIC.categoryName,
            EventCategory.PARTIES.categoryName,
            EventCategory.THEATRE.categoryName,
            EventCategory.KIDS_ENTERTAINMENT.categoryName,
            EventCategory.TECH.categoryName,
            EventCategory.SPORTS.categoryName,
            EventCategory.HEALTH_WELLNESS.categoryName,
            EventCategory.DANCE.categoryName,
            EventCategory.ART.categoryName,
            EventCategory.FOOD.categoryName,
            EventCategory.LITERATURE.categoryName
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseContextActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        EventsAdapter eventsAdapter = new EventsAdapter(activity);
        AutofitRecyclerView exploreGridView = (AutofitRecyclerView) view.findViewById(R.id.explore_grid);
        exploreGridView.setEventsAdapter(eventsAdapter);
        eventsAdapter.setExploreCategories(EXPLORE_TAGS);
    }
}
