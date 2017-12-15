package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Request.Priority;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.DealsObject;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.Locality;
import com.eventshigh.nearme.app.data.SponsoredEventObj;
import com.eventshigh.nearme.app.network.EventInvitationsRequest;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest;
import com.eventshigh.nearme.app.network.FeaturedEventsRequest.EventCollection;
import com.eventshigh.nearme.app.network.GetHotDealsRequest;
import com.eventshigh.nearme.app.network.GetSponsoredEventsRequest;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.adapter.DataType;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.ui.adapter.LocalitiesAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

/**
 * Fragment to show explore by categories.
 */
public class ExploreFragment extends BaseEventsFragment {
    public static final String[] EXPLORE_TAGS = {
            EventCategory.TODAY.categoryName,
            //EventCategory.MOVIES.categoryName,
            EventCategory.NIGHTLIFE.categoryName,
            EventCategory.LIVE_PERFORMANCES.categoryName,
            EventCategory.OUTDOORS.categoryName,
            EventCategory.HEALTH_WELLNESS.categoryName,
            EventCategory.KIDS_ENTERTAINMENT.categoryName,
            EventCategory.SPORTS.categoryName,
            EventCategory.WORKSHOPS.categoryName,
            EventCategory.TECH.categoryName,
            EventCategory.ART.categoryName,
            //    EventCategory.FOOD.categoryName

    };

    public static ExploreFragment getInstance(EventsContext eventsContext) {
        ExploreFragment fragment = new ExploreFragment();
        fragment.setArguments(getArgs(eventsContext, false, false));
        return fragment;
    }

    public static final String nyeEndDate = "2018-01-01 00:00:00.0";

    public static final String christmasEndDate = "2017-12-26 00:00:00.0";

    private EventsAdapter eventsAdapter;
    private View topProgressBar;

    private LinearLayout chooseLocalityLayout;
    private ListView localityListView;
    private ImageView accept, close;

    private List<Locality> selectedLocalities;
    private Account account;

    TextView verifyMobileBtn;


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
        //exploreGridView.setHorizontalSpacing((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        exploreGridView.addItemDecoration(new SpaceItemDecorator((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics())));
        topProgressBar = view.findViewById(R.id.top_progress_bar);
        topProgressBar.setVisibility(View.VISIBLE);
        chooseLocalityLayout = (LinearLayout) view.findViewById(R.id.choose_city_view);
        localityListView = (ListView) view.findViewById(R.id.locality_list);
        accept = (ImageView) view.findViewById(R.id.accept_tick);
        close = (ImageView) view.findViewById(R.id.close_view);

        verifyMobileBtn = (TextView) view.findViewById(R.id.verify_mobile_btn);


        eventsAdapter.setOnEditClickListener(new EventsAdapter.OnEditClickListener() {
            @Override
            public void onEditcliked() {
                activity.reportActionToAnalytics("editlocalitiesclick");
                showCitySelectionView();
            }
        });

        makeServerRequest();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (verifyMobileBtn != null) {
            if (account.getUserInfo().isVerified) {
                verifyMobileBtn.setVisibility(View.GONE);
            } else {
                if (account.getUserInfo().phoneNo != null) {
                    verifyMobileBtn.setVisibility(View.VISIBLE);
                    verifyMobileBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(activity, PhoneLoginActivity.class);
                            startActivity(intent);
                        }
                    });
                } else {
                    verifyMobileBtn.setVisibility(View.GONE);
                }
            }
        }
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

    public String getLocalitiesCommaSeparated() {
        StringBuilder builder = new StringBuilder();
        for (Locality locality : selectedLocalities) {
            builder.append(locality.name + ",");
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

        // makeServerRequest();
    }

    public void makeServerRequest() {
        FeaturedEventsRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                false, mFetcherCallBack, mErrorListener);
    }

    private Listener<EventCollection> mFetcherCallBack = new Listener<EventCollection>() {
        @Override
        public void onResponse(EventCollection eventCollection, boolean isIntermediate) {

            /*eventsAdapter.setExploreCategories(eventCollection, selectedLocalities,
                    eventsContext.city == City.BANGALORE ? EXPLORE_TAGS_BANGALORE :
                            (eventsContext.city == City.CHENNAI ? EXPLORE_TAGS_CHENNAI : EXPLORE_TAGS), "movies");*/

            int width = (activity.getResources().getDisplayMetrics().widthPixels - (2 * (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, activity.getResources().getDisplayMetrics())));
            eventsAdapter.setNewExploreCategories(eventCollection, EXPLORE_TAGS, (System.currentTimeMillis() < DateTimeUtils.parseOfferTime(nyeEndDate)), (System.currentTimeMillis() < DateTimeUtils.parseOfferTime(christmasEndDate)), width);

            makeHotDealsRequest();


            if (!isIntermediate) {
               /* EventInvitationsRequest.submit(activity, eventsContext, Priority.IMMEDIATE, this,
                        false, mEventInvitationsCallback, mErrorListener);*/
            }
        }
    };

    public void makeSponsoredEventsRequest() {

        final int width = (3 * (getResources().getDisplayMetrics().widthPixels -
                3 * ((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics())))) / 7;

        GetSponsoredEventsRequest.submit(activity, eventsContext.city.name().toLowerCase(), Priority.HIGH, this, false, new Listener<List<SponsoredEventObj>>() {
            @Override
            public void onResponse(List<SponsoredEventObj> sponsoredEventObjs, boolean isIntermediate) {
                if (!isIntermediate) {
                    if (sponsoredEventObjs.size() > 0)
                        eventsAdapter.addSponsoredEvents(sponsoredEventObjs, width);
                    topProgressBar.setVisibility(View.GONE);
                }
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                topProgressBar.setVisibility(View.GONE);
            }
        });
    }


    public void makeHotDealsRequest() {

        GetHotDealsRequest.submit(activity, eventsContext.city.name(), Request.Priority.HIGH, null, true, new Response.Listener<DealsObject>() {
            @Override
            public void onResponse(DealsObject dealsObject, boolean b) {
                if (dealsObject != null) {
                    if (dealsObject.getHelloBarDeals() != null && dealsObject.getHelloBarDeals().size() > 0)
                        eventsAdapter.addDealsData(dealsObject.getHelloBarDeals().get(0));
                }
                makeSponsoredEventsRequest();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                //topProgressBar.setVisibility(View.GONE);
                makeSponsoredEventsRequest();
            }
        });

    }

    private Listener<EventInvitationsRequest.InvitaionData> mEventInvitationsCallback = new Listener<EventInvitationsRequest.InvitaionData>() {
        @Override
        public void onResponse(EventInvitationsRequest.InvitaionData eventInvitations, boolean isIntermediate) {
            if (isAdded()) {
                topProgressBar.setVisibility(View.GONE);
                activity.reportActionToAnalytics("showSocialInfo", "eventInvitations",
                        eventInvitations.invitations.size());
                eventsAdapter.addEventInvitations(eventInvitations.invitations, eventInvitations.specials);
            }
        }
    };

    private ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);

            if (eventsAdapter.getItemCount() == 0) {
                int width = (activity.getResources().getDisplayMetrics().widthPixels - (2 * (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, activity.getResources().getDisplayMetrics())));
                eventsAdapter.setNewExploreCategories(null, EXPLORE_TAGS, (System.currentTimeMillis() < DateTimeUtils.parseOfferTime(nyeEndDate)),(System.currentTimeMillis() < DateTimeUtils.parseOfferTime(christmasEndDate)), width);
               /* eventsAdapter.setExploreCategories(null,
                        Locality.getLocalities(eventsContext.city, false),
                        eventsContext.city == City.BANGALORE ? EXPLORE_TAGS_BANGALORE : EXPLORE_TAGS, "movies");*/
            }
            makeHotDealsRequest();
        }
    };

    public class SpaceItemDecorator extends RecyclerView.ItemDecoration {
        int space;

        public SpaceItemDecorator(int space) {
            this.space = space;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {


            if (parent.getAdapter().getItemViewType(parent.getChildAdapterPosition(view)) == DataType.EVENT_PAGER.typeId
                    || parent.getAdapter().getItemViewType(parent.getChildAdapterPosition(view)) == DataType.EXPLORE_CATEGORY_HEADER.typeId) {
                outRect.top = 0;
                outRect.bottom = 0;
                outRect.left = 0;
                outRect.right = 0;
            } else if (parent.getAdapter().getItemViewType(parent.getChildAdapterPosition(view)) == DataType.BROWSE_SPONSORED_EVENTS.typeId) {
                outRect.top = 0;
                outRect.bottom = space;
                outRect.left = 0;
                outRect.right = 0;
            } else if (parent.getAdapter().getItemViewType(parent.getChildAdapterPosition(view)) == DataType.HELLOBAR_DEAL_CARD.typeId) {
                outRect.top = space;
                outRect.bottom = 0;
                outRect.left = space;
                outRect.right = space;
            } else {
                outRect.top = space;
                if (parent.getChildAdapterPosition(view) % 2 == 0) {
                    outRect.right = space;
                    outRect.left = space / 2;
                } else {
                    outRect.left = space;
                    outRect.right = space / 2;
                }
            }

        }


    }
}
