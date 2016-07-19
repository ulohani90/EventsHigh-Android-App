package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.FriendsStore;
import com.eventshigh.nearme.app.data.ProfileInfo;
import com.eventshigh.nearme.app.network.AddFacebookUserInfoRequest;
import com.eventshigh.nearme.app.network.FetchProfileRequest;
import com.eventshigh.nearme.app.network.MovieReviewSubmitRequest;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.view.CircularImageView;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class UserProfileActivity extends BaseContextActivity implements View.OnClickListener, ViewPager.OnPageChangeListener {
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
    private FloatingActionButton fabWriteReviews;


    TextView tvFacebookInfo;
    LinearLayout llFacebookInfoMask, llAboutUserMask;
    JSONObject facebookJsonObject;

    private String mobileNoProfileUser;
    private boolean isUserSelf;

    private LinearLayout verifyPhnLayout;
    private Account account;
    private FriendsStore friendsStore;

    //TABS
    private final String FAVOURITES_TAB = "Favourites";
    private final String INTERESTS_TAB = "Interests";
    private final String REVIEWS_TAB = "Reviews";
    private final String TICKETS_TAB = "Tickets";
    private final String FRIENDS_TAB = "Friends";

    boolean isFromNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_user_profile);

        //phone verify
        account = new Account(this);
        verifyPhnLayout = (LinearLayout) findViewById(R.id.verify_phn_layout);
        (findViewById(R.id.verify_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyClicked();
            }

        });

        mobileNoProfileUser = getIntent().getStringExtra(PROFILE_ID);
        //check if user's self profile
        if (mobileNoProfileUser != null) {
            if (mobileNoProfileUser.equalsIgnoreCase(account.getUserInfo().phoneNo)) {
                isUserSelf = true;
            } else {
                isUserSelf = false;
            }
        } else {
            isUserSelf = true;
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        retryView = findViewById(R.id.view_retry);
        findViewById(R.id.retry).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchProfileInfo(true);
            }
        });
        addToolbarView();
        pager = (ViewPager) findViewById(R.id.view_pager);
        tabsView = (TabLayout) findViewById(R.id.tabs);


        topProgressBar = findViewById(R.id.top_progress_bar);

        FacebookSdk.sdkInitialize(getApplicationContext());

        //follow functionality
        friendsStore = new FriendsStore(this);

        //write review
        fabWriteReviews = (FloatingActionButton) findViewById(R.id.fab_write_review);
        pager.addOnPageChangeListener(this);
        fabWriteReviews.setOnClickListener(this);
        fabWriteReviews.setVisibility(View.GONE);

        if (getIntent().getBooleanExtra(FROM_NOTIFICATION_PARAM, false)) {
            isFromNotification = true;
        }
    }


    @Override
    public View getViewForSnackbar() {
        return toolbar;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isFromNotification) {
            Intent intent = new Intent(this, LaunchActivity.class);
            startActivity(intent);
            finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        verifyPhnLayout.setVisibility(View.GONE);
        if (Utils.checkIfStringEmpty(mobileNoProfileUser) && account.getUserInfo().isVerified) {
            mobileNoProfileUser = account.getUserInfo().phoneNo;
        }
        fetchProfileInfo(true);
    }

    private void setViews(ProfileInfo profileInfo) {
        int favCount = profileInfo.getMeEventFavouriteObject().topicEvents.get(0).events.size() +
                profileInfo.getMeEventFavouriteObject().movies.size();
        TABS = new ArrayList<>();
        if (isUserSelf) {
            TABS.add(INTERESTS_TAB);
            TABS.add(FAVOURITES_TAB);
            TABS.add(FRIENDS_TAB);
            TABS.add(TICKETS_TAB);
            TABS.add(REVIEWS_TAB);
            mobileNoProfileUser = account.getUserInfo().phoneNo;
        } else {
            if (profileInfo.getMyInterestEvents().size() != 0)
                TABS.add(INTERESTS_TAB);
            if (favCount != 0)
                TABS.add(FAVOURITES_TAB);
            TABS.add(REVIEWS_TAB);
            btnFollow.setVisibility(View.VISIBLE);

            if (friendsStore.isFollowing(mobileNoProfileUser)) {
                btnFollow.setSelected(true);
                btnFollow.setText(R.string.ui_following);

            } else {
                btnFollow.setSelected(false);
                btnFollow.setText(R.string.ui_follow);
            }
        }
        userProfilePagerAdapter = new UserProfilePagerAdapter(getSupportFragmentManager(), this);
        userProfilePagerAdapter.setProfileInfo(profileInfo);
        pager.setAdapter(userProfilePagerAdapter);
        tabsView.setupWithViewPager(pager);
        tabsView.setScrollPosition(0, 0, true);
        animateFab(0);

        topProgressBar.setVisibility(View.GONE);

        if (profileInfo.getName() != null)
            userName.setText(profileInfo.getName());

        if (profileInfo.getLastCity() != null)
            userCity.setText(profileInfo.getLastCity());

        //attech user data
        if (!Utils.checkIfStringEmpty(profileInfo.getProfilePic())) {
            Glide.with(UserProfileActivity.this).load(profileInfo.getProfilePic())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.com_facebook_profile_picture_blank_portrait).crossFade().centerCrop()
                    .into(userImage);
            llAboutUserMask.setVisibility(View.VISIBLE);
        } else if (isUserSelf) {
            llFacebookInfoMask.setVisibility(View.VISIBLE);
            userName.setText(new Account(UserProfileActivity.this).getUserInfo().name);
            userCity.setText(new Account(UserProfileActivity.this).getLastCity().name());
        } else {
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

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void fetchProfileInfo(boolean shouldByPassChange) {
        if (Utils.checkIfStringEmpty(mobileNoProfileUser)) {
            verifyPhnLayout.setVisibility(View.VISIBLE);
        } else {

            topProgressBar.setVisibility(View.VISIBLE);
            retryView.setVisibility(View.GONE);
            FetchProfileRequest.submit(this, mobileNoProfileUser, Request.Priority.HIGH,
                    new Response.Listener<ProfileInfo>() {
                        @Override
                        public void onResponse(ProfileInfo profileInfo, boolean b) {
                            if (isRunning()) {
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
    }

    public void addToolbarView() {
        View view = LayoutInflater.from(this).inflate(R.layout.card_user_profile, toolbar, false);
        (view.findViewById(R.id.back_arrow)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
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
        findViewById(R.id.btn_user_favourite_count).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TABS.contains(FAVOURITES_TAB)) {
                    pager.setCurrentItem(TABS.indexOf(FAVOURITES_TAB));
                }
            }
        });
        findViewById(R.id.btn_user_follower_count).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TABS.contains(FRIENDS_TAB)) {
                    pager.setCurrentItem(TABS.indexOf(FRIENDS_TAB));
                }
            }
        });
        findViewById(R.id.btn_user_interest_count).setOnClickListener(new View.OnClickListener() {
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


    public void verifyClicked() {
        startActivity(new Intent(this, PhoneLoginActivity.class));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.facebook_fetch_info:
                reportActionToAnalytics("fetchFBInfo");
                fbLoginButtonPressed();
                break;
            case R.id.profile_share:
                shareProfileWithBranch(userProfilePagerAdapter.getProfileInfo(), mobileNoProfileUser, null, "Profile");
                break;
            case R.id.btn_follow:
                if (btnFollow.isSelected()) {

                    reportActionToAnalytics("userFollowBtnClick");
                    friendsStore.setFollowing(mobileNoProfileUser, mobileNoProfileUser, false);
                    btnFollow.setText(R.string.ui_follow);
                    btnFollow.setSelected(false);
                } else {
                    reportActionToAnalytics("userUnFollowBtnClick");
                    friendsStore.setFollowing(mobileNoProfileUser, mobileNoProfileUser, true);
                    btnFollow.setText(R.string.ui_following);
                    btnFollow.setSelected(true);
                    btnFollow.setSelected(true);
                }

                //friendsStore.setFollowing(mobileNoProfileUser,mobileNoProfileUser,true);
                break;
            case R.id.fab_write_review:
                reportActionToAnalytics("profileInterestFabClick");
                Intent i = new Intent(this, SelectInterestsActivity.class);
                startActivity(i);
                break;
        }
    }

    LoginResult loginResult;

    void fbLoginButtonPressed() {
        LoginManager.getInstance().logOut();
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email", "user_friends", "user_interests"));
        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginRes) {
                loginResult = loginRes;
                requestUserProfile();
            }

            @Override
            public void onCancel() {
                Toast.makeText(getBaseContext(), "Login Cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException e) {
                Log.e("Problem conn fb", e.toString());
                Toast.makeText(getBaseContext(), "Problem connecting to Facebook", Toast.LENGTH_SHORT).show();
            }
        });
    }

    void requestUserProfile() {
        System.out.println("onSuccess");
        final String accessToken = loginResult.getAccessToken().getToken();
        Log.i("accessToken", accessToken);

        GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                Log.i("LoginActivity", response.toString());
                // Get facebook data from login
                if (object != null) {
                    try {
                        String userId = object.getString("id");
                        object.remove("id");
                        object.put("fb_id", userId);
                        object.put("fb_profile_pic", "https://graph.facebook.com/" + userId + "/picture?type=large");
                        String temp = object.getString("email");
                        object.remove("email");
                        object.put("fb_email", temp);
                        temp = object.getString("name");
                        object.remove("name");
                        object.put("fb_name", temp);
                        object.put("fb_token", accessToken);
                        object.put("android_id", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
                        addFacebookUserInfo(object);
                        facebookJsonObject = object;
                        llAboutUserMask.setVisibility(View.VISIBLE);
                        llFacebookInfoMask.setVisibility(View.GONE);
                    } catch (JSONException e) {
                        Log.i("User Profile", "JSON Exception");
                    }
                    Log.e("obj ", object.toString());
                }
            }
        });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "name, email");//add fields to fetch in graph response
        request.setParameters(parameters);
        request.executeAsync();
    }


    private void addFacebookUserInfo(JSONObject object) {
        AddFacebookUserInfoRequest.submit(this, object, Request.Priority.HIGH,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject, boolean b) {
                        updateProfile();
                        Toast.makeText(UserProfileActivity.this, "Your profile has been updated successfully.", Toast.LENGTH_SHORT).show();
                        ;
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {

                    }
                });
    }

    private void updateProfile() {
        llAboutUserMask.setVisibility(View.VISIBLE);
        try {
            if (facebookJsonObject.has("fb_profile_pic")) {
                Glide.with(this).load(facebookJsonObject.getString("fb_profile_pic"))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                        .into(userImage);
                userProfilePagerAdapter.profileInfo.setProfilePic(facebookJsonObject.getString("fb_profile_pic"));
            }
            if (facebookJsonObject.has("fb_name")) {
                userName.setText(facebookJsonObject.getString("fb_name"));
                userProfilePagerAdapter.profileInfo.setName(facebookJsonObject.getString("fb_name"));
            }
            if (facebookJsonObject.has("fb_email")) {
                userProfilePagerAdapter.profileInfo.setEmail(facebookJsonObject.getString("fb_email"));
            }

        } catch (JSONException jse) {
            Log.e("User Profile", jse.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            callbackManager.onActivityResult(requestCode, resultCode, data);
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
                myFavouritesFragment = EventsFragment.getInstance(myEventsContext, false, false, false, null, false, profileInfo);
                return myFavouritesFragment;
            } else if (TABS.get(position).equalsIgnoreCase(INTERESTS_TAB)) {
                EventsContext myEventsContext = new EventsContext(eventsContext.location,
                        EventsHighEndpoints.QUERY_MY_INTEREST_EVENTS);
                myInterestEventsFragment = EventsFragment.getInstance(myEventsContext, false, true, false, null, false, profileInfo);
                return myInterestEventsFragment;
            } else if (TABS.get(position).equalsIgnoreCase(REVIEWS_TAB)) {
                MyReviewsFragment myReviewsFragment = MyReviewsFragment.newInstance(eventsContext, profileInfo.getMovieUserReviewObjectArrayList(), mobileNoProfileUser);
                return myReviewsFragment;
            } else if (TABS.get(position).equalsIgnoreCase(TICKETS_TAB)) {
                MyTicketsFragment myTicketsFragment = new MyTicketsFragment();
                return myTicketsFragment;
            } else if (TABS.get(position).equalsIgnoreCase(FRIENDS_TAB)) {
                ContactsFragment fragment = ContactsFragment.newInstance(profileInfo);
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
        reportActionToAnalytics("profile_tab_change", TABS.get(position));
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
        if (!TABS.get(position).equalsIgnoreCase(INTERESTS_TAB)) {
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


}