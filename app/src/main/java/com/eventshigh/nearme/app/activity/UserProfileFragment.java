package com.eventshigh.nearme.app.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.ProfileInfo;
import com.eventshigh.nearme.app.network.FetchProfileRequest;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.CircularImageView;
import com.facebook.CallbackManager;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by umesh on 01/08/16.
 */
public class UserProfileFragment extends Fragment implements View.OnClickListener, ViewPager.OnPageChangeListener {


    CallbackManager callbackManager;


    public static final String PROFILE_ID = "profile_id";

    public static final String FROM_NOTIFICATION_PARAM = "is_from_notification";

    public ArrayList<String> TABS;
    CircularImageView userImage;
    ImageView shareButton;
    TextView userName, userCity;
    TextView btnFollow;
    TextView userInterestCount, userFollowerCount, userFavouriteCount;

    TabLayout tabsView;
    ViewPager pager;

    UserProfilePagerAdapter userProfilePagerAdapter;

    private View topProgressBar;
    private View retryView;
    private View profileView;

    TextView tvFacebookInfo;
    LinearLayout llFacebookInfoMask, llAboutUserMask;
    JSONObject facebookJsonObject;


    private String emailProfileUser;


    private LinearLayout loginViaFBLayout;
    private Account account;
    private FriendsStore friendsStore;

    //TABS
    private final String FAVOURITES_TAB = "Favourites";
    private final String INTERESTS_TAB = "Interests";
    private final String REVIEWS_TAB = "Reviews";
    private final String TICKETS_TAB = "Tickets";
    private final String FRIENDS_TAB = "Friends";

    boolean isFromNotification;

    BaseActivity activity;

    EventsContext eventsContext;

    public static UserProfileFragment newInstance(EventsContext eventsContext) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("events_context", eventsContext);
        UserProfileFragment fragment = new UserProfileFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (BaseActivity) context;
        eventsContext = getArguments().getParcelable("events_context");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    Toolbar toolbar;

    View view;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_user_profile, container, false);
        toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        topProgressBar = view.findViewById(R.id.top_progress_bar_activity);
        //phone verify
        account = new Account(getActivity());
        loginViaFBLayout = (LinearLayout) view.findViewById(R.id.verify_phn_layout);
        (view.findViewById(R.id.verify_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("loginClicked");
                showFbLoginActivity();
            }

        });

        retryView = view.findViewById(R.id.view_retry);
        view.findViewById(R.id.retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchProfileInfo(true);
            }
        });


        pager = (ViewPager) view.findViewById(R.id.view_pager);
        tabsView = (TabLayout) view.findViewById(R.id.tabs);
        tabsView.setVisibility(View.GONE);

        //follow functionality
        friendsStore = new FriendsStore(getActivity());

        //write review

        pager.addOnPageChangeListener(this);
        if (getArguments().getBoolean(FROM_NOTIFICATION_PARAM, false)) {
            isFromNotification = true;
        }

        addToolbarView();
        return view;
    }

    public void showFbLoginActivity() {
        Intent intent = new Intent(activity, FBLoginActivity.class);
        intent.putExtra("show_special_text", true);
        intent.putExtra("hide_skip", true);
        activity.startActivity(intent);
    }

    @Override
    public void onStart() {
        super.onStart();

        if (account.getUserInfo().isSignedIn) {
            emailProfileUser = account.getUserInfo().email;
            loginViaFBLayout.setVisibility(View.GONE);
            fetchProfileInfo(true);
        } else {
            loginViaFBLayout.setVisibility(View.VISIBLE);

        }


    }

    private void fetchProfileInfo(boolean shouldByPassChange) {
        topProgressBar.setVisibility(View.VISIBLE);
        retryView.setVisibility(View.GONE);
        FetchProfileRequest.submit(activity, emailProfileUser, Request.Priority.HIGH,
                new Response.Listener<ProfileInfo>() {
                    @Override
                    public void onResponse(ProfileInfo profileInfo, boolean b) {
                        if (isAdded()) {
                            if (profileInfo != null) {
                                setViews(profileInfo);
                            } else {
                                topProgressBar.setVisibility(View.GONE);
                                retryView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        topProgressBar.setVisibility(View.GONE);
                        retryView.setVisibility(View.VISIBLE);
                    }
                }, shouldByPassChange);

    }

    private void setViews(ProfileInfo profileInfo) {


        int favCount = profileInfo.getMeEventFavouriteObject().topicEvents.get(0).events.size() +
                profileInfo.getMeEventFavouriteObject().movies.size();
        TABS = new ArrayList<>();
        TABS.add(INTERESTS_TAB);
        TABS.add(FAVOURITES_TAB);
        TABS.add(FRIENDS_TAB);
        TABS.add(TICKETS_TAB);
        TABS.add(REVIEWS_TAB);
        emailProfileUser = account.getUserInfo().email;


        userProfilePagerAdapter = new UserProfilePagerAdapter(getChildFragmentManager(), activity);
        userProfilePagerAdapter.setProfileInfo(profileInfo);
        pager.setAdapter(userProfilePagerAdapter);
        tabsView.setVisibility(View.VISIBLE);
        tabsView.setupWithViewPager(pager);
        tabsView.setScrollPosition(0, 0, true);
        if (account.getUserInfo().isSignedIn && ((LaunchActivity) getActivity()).getViewPager() != null && ((LaunchActivity) getActivity()).getViewPager().getCurrentItem() == 0)
            ((LaunchActivity) getActivity()).animateFabIn();

        topProgressBar.setVisibility(View.GONE);

        if (profileInfo.getName() != null)
            userName.setText(profileInfo.getName());

        if (profileInfo.getLastCity() != null)
            userCity.setText(profileInfo.getLastCity());

        //attech user data
        if (!Utils.checkIfStringEmpty(profileInfo.getProfilePic())) {
            Glide.with(activity).load(profileInfo.getProfilePic())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.com_facebook_profile_picture_blank_portrait).crossFade().centerCrop()
                    .into(userImage);
            llAboutUserMask.setVisibility(View.VISIBLE);
        }
        //attech counts
        userFollowerCount.setText(profileInfo.getUserContactList().size() + "");
        userInterestCount.setText(profileInfo.getMyInterestEvents().size() + "");
        userFavouriteCount.setText(favCount + "");
        pager.setCurrentItem(0);
        TabLayout.Tab tab = tabsView.getTabAt(0);
        tab.select();
        profileView.setVisibility(View.VISIBLE);
        shareButton.setVisibility(View.VISIBLE);
        tabsView.setVisibility(View.VISIBLE);
        pager.setVisibility(View.VISIBLE);
    }


    public void addToolbarView() {
        View view = LayoutInflater.from(activity).inflate(R.layout.card_user_profile, toolbar, false);
        view.findViewById(R.id.profile_toolbar).setVisibility(View.GONE);
        view.findViewById(R.id.top_progress_bar).setVisibility(View.GONE);
        userImage = (CircularImageView) view.findViewById(R.id.profile_image);
        shareButton = (ImageView) view.findViewById(R.id.profile_share);
        userName = (TextView) view.findViewById(R.id.profile_user_name);
        userCity = (TextView) view.findViewById(R.id.profile_user_city);
        btnFollow = (TextView) view.findViewById(R.id.btn_follow);
        btnFollow.setOnClickListener(this);
        userInterestCount = (TextView) view.findViewById(R.id.user_interest_count);
        userFavouriteCount = (TextView) view.findViewById(R.id.user_favourite_count);
        userFollowerCount = (TextView) view.findViewById(R.id.user_follower_count);
        tvFacebookInfo = (TextView) view.findViewById(R.id.facebook_fetch_info);
        llFacebookInfoMask = (LinearLayout) view.findViewById(R.id.ll_facebook_info_mask);
        llAboutUserMask = (LinearLayout) view.findViewById(R.id.ll_about_user_mask);
        tvFacebookInfo.setOnClickListener(this);
        shareButton.setOnClickListener(this);
        toolbar.addView(view);
        toolbar.setBackgroundColor(Color.TRANSPARENT);
        profileView = view.findViewById(R.id.profile_view);
        UserProfileFragment.this.view.findViewById(R.id.btn_user_favourite_count).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TABS.contains(FAVOURITES_TAB)) {
                    pager.setCurrentItem(TABS.indexOf(FAVOURITES_TAB));
                }
            }
        });
        UserProfileFragment.this.view.findViewById(R.id.btn_user_follower_count).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TABS.contains(FRIENDS_TAB)) {
                    pager.setCurrentItem(TABS.indexOf(FRIENDS_TAB));
                }
            }
        });
        UserProfileFragment.this.view.findViewById(R.id.btn_user_interest_count).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TABS.contains(INTERESTS_TAB)) {
                    pager.setCurrentItem(TABS.indexOf(INTERESTS_TAB));
                }
            }
        });
    }

    public void setFriendsCount(int count) {
        userFollowerCount.setText(count + "");
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.facebook_fetch_info:
                activity.reportActionToAnalytics("fetchFBInfo");
                break;
            case R.id.profile_share:
                activity.shareProfileWithBranch(userProfilePagerAdapter.getProfileInfo(), emailProfileUser, null, "Profile");
                break;
            case R.id.btn_follow:
                if (btnFollow.isSelected()) {
                    activity.reportActionToAnalytics("userFollowBtnClick");
                    friendsStore.setFollowing(emailProfileUser, null, false);
                    btnFollow.setText(R.string.ui_follow);
                    btnFollow.setSelected(false);
                } else {
                    activity.reportActionToAnalytics("userUnFollowBtnClick");
                    friendsStore.setFollowing(emailProfileUser, null, true);
                    btnFollow.setText(R.string.ui_following);
                    btnFollow.setSelected(true);
                    btnFollow.setSelected(true);
                }

                //friendsStore.setFollowing(mobileNoProfileUser,mobileNoProfileUser,true);
                break;

        }
    }


    //Viewpager Fragments
    EventsFragment myFavouritesFragment;
    EventsFragment myInterestEventsFragment;

    public class UserProfilePagerAdapter extends FragmentStatePagerAdapter {
        private Context context;
        private ProfileInfo profileInfo;

        public ProfileInfo getProfileInfo() {
            return profileInfo;
        }

        public void setProfileInfo(ProfileInfo profileInfo) {
            this.profileInfo = profileInfo;
        }


        public UserProfilePagerAdapter(FragmentManager fragmentManager, Context context) {
            super(fragmentManager);
            this.context = context;
        }


        @Override
        public Fragment getItem(int position) {
            if (TABS.get(position).equalsIgnoreCase(FAVOURITES_TAB)) {
                LatLng latLng = (new Account(context)).getLastCity().cityBounds.getCenter();
                EventsContext myEventsContext = new EventsContext(latLng, EventsHighEndpoints.QUERY_MY_EVENT);
                myFavouritesFragment = EventsFragment.getInstance(myEventsContext, false, false, false, null, false, profileInfo, false);
                return myFavouritesFragment;
            } else if (TABS.get(position).equalsIgnoreCase(INTERESTS_TAB)) {
                EventsContext myEventsContext = new EventsContext(eventsContext.location,
                        EventsHighEndpoints.QUERY_MY_INTEREST_EVENTS);
                myInterestEventsFragment = EventsFragment.getInstance(myEventsContext, false, true, false, null, false, profileInfo, false);
                return myInterestEventsFragment;
            } else if (TABS.get(position).equalsIgnoreCase(REVIEWS_TAB)) {
                MyReviewsFragment myReviewsFragment = MyReviewsFragment.newInstance(eventsContext, profileInfo.getMovieUserReviewObjectArrayList(), emailProfileUser);
                return myReviewsFragment;
            } else if (TABS.get(position).equalsIgnoreCase(TICKETS_TAB)) {
                MyTicketsFragment myTicketsFragment = new MyTicketsFragment();
                return myTicketsFragment;
            } else if (TABS.get(position).equalsIgnoreCase(FRIENDS_TAB)) {
                Bundle bundle = new Bundle();
                bundle.putParcelable("profile_info", profileInfo);
                NewContactsFragment fragment = NewContactsFragment.newInstance(bundle);
                return fragment;
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
    }

    //OnPageListerner Methods
    @Override
    public void onPageSelected(int position) {
        activity.reportActionToAnalytics("profile_tab_change", TABS.get(position));
        if (position == 0 && ((LaunchActivity) getActivity()).getViewPager() != null && ((LaunchActivity) getActivity()).getViewPager().getCurrentItem() == 0) {
            ((LaunchActivity) getActivity()).animateFabIn();
        } else {
            ((LaunchActivity) getActivity()).animateFabOut();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {
        Log.e("", state + " state changed");
    }


    public ViewPager getPager() {
        return pager;
    }
}

