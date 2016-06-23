package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.MovieReviewSubmitRequest;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

public class UserProfileActivity extends BaseContextActivity implements View.OnClickListener{
    CallbackManager callbackManager;

    public ArrayList<String> TABS;


    ImageView userImage;
    TextView userName;
    TextView userCity;
    TextView userInterestCount, userFollowerCount, userFavouriteCount;
    TabLayout tabsView;
    ViewPager pager;
    UserProfilePagerAdapter userProfilePagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setupLayout(R.layout.activity_user_profile);
        TABS = new ArrayList<>();
        TABS.add("Favourites");
        TABS.add("Events");
        TABS.add("Friends");
        TABS.add("My Tickets");

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        addToolbarView();

        pager = (ViewPager) findViewById(R.id.view_pager);
        userProfilePagerAdapter = new UserProfilePagerAdapter(getSupportFragmentManager(),this);
        pager.setAdapter(userProfilePagerAdapter);

        tabsView = (TabLayout) findViewById(R.id.tabs);
        tabsView.setupWithViewPager(pager);
        tabsView.setVisibility(View.VISIBLE);
        pager.setVisibility(View.VISIBLE);
        //tabsView.setScrollPosition(0, 0, true);


        FacebookSdk.sdkInitialize(getApplicationContext());
        fbLoginButtonPressed();
    }

    public void addToolbarView() {
        View view = LayoutInflater.from(this).inflate(R.layout.card_user_profile, toolbar, false);
        userImage = (ImageView) view.findViewById(R.id.profile_image);
        userName = (TextView) view.findViewById(R.id.profile_user_name);
        userCity = (TextView) view.findViewById(R.id.profile_user_city);
        userInterestCount = (TextView)view.findViewById(R.id.user_interest_count);
        userFavouriteCount = (TextView)view.findViewById(R.id.user_favourite_count);
        userFollowerCount = (TextView)view.findViewById(R.id.user_follower_count);
        toolbar.addView(view);
        toolbar.setBackgroundColor(Color.TRANSPARENT);
    }


    @Override
    public void onClick(View v){
        switch (v.getId()) {
        }
    }


    void fbLoginButtonPressed(){
        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("public_profile", "email", "user_friends"));
        callbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                requestUserProfile(loginResult);
            }
            @Override
            public void onCancel() {
                Toast.makeText(getBaseContext(), "Login Cancelled", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(FacebookException e){
                Log.e("Problem conn fb",e.toString());
                Toast.makeText(getBaseContext(), "Problem connecting to Facebook" , Toast.LENGTH_SHORT).show();
            }
        });

    }

    void requestUserProfile(LoginResult loginResult){
                System.out.println("onSuccess");
                String accessToken = loginResult.getAccessToken().getToken();
                Log.i("accessToken", accessToken);

                GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {

                    @Override
                    public void onCompleted(JSONObject object, GraphResponse response) {
                        Log.i("LoginActivity", response.toString());
                        // Get facebook data from login
                        try {
                            String userID = object.getString("id");
                            object.remove("id");
                            object.put("profile_pic","https://graph.facebook.com/" + userID + "/picture?type=large");
                        }catch (JSONException e){
                            Log.e("User Profile","JSON Exception");
                        }
                        //tv1.setText(object.toString());
                        Log.e("obj ",object.toString());
                     }
            });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "name, email"); // Parámetros que pedimos a facebook
            request.setParameters(parameters);
            request.executeAsync();

    }

    public void placeReviewAction(final JSONObject data){
        MovieReviewSubmitRequest.submit(this,
                data, Request.Priority.HIGH, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject, boolean b) {
                        Log.i("Message Success", "true");
                        Toast.makeText(getApplicationContext(), "Your profile created successfully", Toast.LENGTH_SHORT);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.i("Message failure", "true" + data.toString());
                    }
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    EventsFragment myFavouritesFragment;
    EventsFragment myInterestEventsFragment;

    public class UserProfilePagerAdapter extends FragmentStatePagerAdapter{
        private Context context;
        public UserProfilePagerAdapter(FragmentManager fragmentManager, Context context){
            super(fragmentManager);
            this.context = context;
        }

        @Override
        public Fragment getItem(int position){
            if(position == 0){
                LatLng latLng = (new Account(context)).getLastCity().cityBounds.getCenter();
                EventsContext myEventsContext = new EventsContext(latLng,EventsHighEndpoints.QUERY_MY_EVENT);
                myFavouritesFragment = EventsFragment.getInstance(myEventsContext, false, false, false, null,false);
                return myFavouritesFragment;
            }else  if(position == 1){
                EventsContext myEventsContext = new EventsContext(eventsContext.location,
                        EventsHighEndpoints.QUERY_MY_INTEREST_EVENTS);
                myInterestEventsFragment = EventsFragment.getInstance(myEventsContext, false, true, false, null,false);
                return myInterestEventsFragment;
            }else if(position == 2){
                ContactsFragment fragment = new ContactsFragment();
                return fragment;
            }else{
                MyTicketsFragment myTicketsFragment = new MyTicketsFragment();
                return myTicketsFragment;
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


}