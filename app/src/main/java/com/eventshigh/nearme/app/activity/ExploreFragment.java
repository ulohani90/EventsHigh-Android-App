package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.Locality;
import com.eventshigh.nearme.app.network.EventInvitationsRequest;
import com.eventshigh.nearme.app.network.EventInvitationsRequest.EventInvitation;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest.EventCollection;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.ui.adapter.LocalitiesAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

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

    public static final String[] EXPLORE_TAGS_CHENNAI = {
            EventCategory.PARTIES.categoryName,
            EventCategory.THEATRE.categoryName,
            EventCategory.MUSIC.categoryName,
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
            EventCategory.DANCE.categoryName,
            EventCategory.ART.categoryName,
            EventCategory.FOOD.categoryName,
            EventCategory.LITERATURE.categoryName,
    };

    public static ExploreFragment getInstance(EventsContext eventsContext) {
        ExploreFragment fragment = new ExploreFragment();
        fragment.setArguments(getArgs(eventsContext, false, false));
        return fragment;
    }

    private EventsAdapter eventsAdapter;
    private View topProgressBar;

    private LinearLayout chooseLocalityLayout;
    private ListView localityListView;
    private ImageView accept, close;

    private List<Locality> selectedLocalities;
    private Account account;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        account = new Account(getActivity());
        eventsAdapter = new EventsAdapter(activity);
        AutofitRecyclerView exploreGridView = (AutofitRecyclerView) view.findViewById(R.id.explore_grid);
        exploreGridView.setAdapter(eventsAdapter);
        exploreGridView.addOnScrollListener(new HideActionBarOnScroll(activity));

        topProgressBar = view.findViewById(R.id.top_progress_bar);
        topProgressBar.setVisibility(View.VISIBLE);
        chooseLocalityLayout = (LinearLayout) view.findViewById(R.id.choose_city_view);
        localityListView = (ListView) view.findViewById(R.id.locality_list);
        accept = (ImageView) view.findViewById(R.id.accept_tick);
        close = (ImageView) view.findViewById(R.id.close_view);

        eventsAdapter.setOnEditClickListener(new EventsAdapter.OnEditClickListener() {
            @Override
            public void onEditcliked() {
                activity.reportActionToAnalytics("editlocalitiesclick");
                Toast.makeText(getActivity(), "Header Clicked Edited", Toast.LENGTH_SHORT).show();
                showCitySelectionView();
            }
        });
    }

    public void animateLocalityViewOut() {
        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.animate_up_bottom);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ((LaunchActivity) getActivity()).setisPagerSwipeBlocked(false);
                chooseLocalityLayout.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        chooseLocalityLayout.startAnimation(anim);
    }

    public void animateLocalityViewIn() {
        Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.animate_bottom_up);
        chooseLocalityLayout.startAnimation(anim);
        chooseLocalityLayout.setVisibility(View.VISIBLE);
        ((LaunchActivity) getActivity()).setisPagerSwipeBlocked(true);
    }

    public void showCitySelectionView() {
        LocalitiesAdapter adapter = new LocalitiesAdapter(getActivity(),
                Locality.getLocalities(eventsContext.city, true), selectedLocalities);
        adapter.setOnLocalitySelectedListener(new LocalitiesAdapter.OnLocalitySelectedListener() {
            @Override
            public boolean onLocalitySelected(Locality locality, boolean isSelected) {

                if (isSelected) {
                    selectedLocalities.remove(locality);
                    return false;
                } else {
                    if (selectedLocalities.size() == 6) {
                        Toast.makeText(getActivity(), "You cannot select more than 6 localities.",
                                Toast.LENGTH_LONG).show();
                        return false;
                    } else {
                        selectedLocalities.add(locality);
                        return true;
                    }
                }
            }
        });

        localityListView.setAdapter(adapter);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Animate View Down;
                animateLocalityViewOut();
            }
        });

        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedLocalities.size() >= 2) {
                    activity.reportActionToAnalytics("selectedlocalities", getLocalitiesCommaSeparated());
                    account.setSavedLocalities(selectedLocalities);
                    makeServerRequest();
                    animateLocalityViewOut();
                } else {
                    Toast.makeText(getActivity(), "Please select atleast " + (2 - selectedLocalities.size()) + " more", Toast.LENGTH_SHORT).show();
                }
            }
        });
        animateLocalityViewIn();
    }

    public String getLocalitiesCommaSeparated(){
        StringBuilder builder  =new StringBuilder();
        for (Locality locality : selectedLocalities){
            builder.append(locality.name+",");
        }
        return builder.toString();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (account.getSavedLocalities().size() > 0) {
            selectedLocalities = account.getSavedLocalities();
        } else {
            selectedLocalities = Locality.getLocalities(eventsContext.city, false);
        }

        makeServerRequest();
    }

    public void makeServerRequest() {
        FeaturedEventsRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                false, mFetcherCallBack, mErrorListener);
    }

    private Listener<EventCollection> mFetcherCallBack = new Listener<EventCollection>() {
        @Override
        public void onResponse(EventCollection eventCollection, boolean isIntermediate) {

            eventsAdapter.setExploreCategories(eventCollection, selectedLocalities,
                    eventsContext.city == City.BANGALORE ? EXPLORE_TAGS_BANGALORE :
                            (eventsContext.city == City.CHENNAI ? EXPLORE_TAGS_CHENNAI : EXPLORE_TAGS));

            if (!isIntermediate) {
                EventInvitationsRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                        false, mEventInvitationsCallback, mErrorListener);
            }
        }
    };

    private Listener<List<EventInvitation>> mEventInvitationsCallback = new Listener<List<EventInvitation>>() {
        @Override
        public void onResponse(List<EventInvitation> eventInvitations, boolean isIntermediate) {
            if (isAdded()) {
                topProgressBar.setVisibility(View.GONE);
                activity.reportActionToAnalytics("showSocialInfo", "eventInvitations",
                        eventInvitations.size());
                eventsAdapter.addEventInvitations(eventInvitations);
            }
        }
    };

    private ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);

            if (eventsAdapter.getItemCount() == 0) {
                eventsAdapter.setExploreCategories(null,
                    Locality.getLocalities(eventsContext.city, false),
                    eventsContext.city == City.BANGALORE ? EXPLORE_TAGS_BANGALORE : EXPLORE_TAGS);
            }
        }
    };
}
