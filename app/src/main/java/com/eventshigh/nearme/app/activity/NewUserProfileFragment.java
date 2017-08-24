package com.eventshigh.nearme.app.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.BasicProfileInfo;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.network.FetchBasicUserProfileRequest;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by umesh on 22/08/17.
 */

public class NewUserProfileFragment extends Fragment {


    public static NewUserProfileFragment newInstance(EventsContext eventsContext) {
        Bundle bundle = new Bundle();
        bundle.putParcelable("events_context", eventsContext);
        NewUserProfileFragment fragment = new NewUserProfileFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    BaseActivity activity;

    EventsContext eventsContext;

    LinearLayout profileLayout;

    TextView userName, userEmail, numInterests, numFavourites, numReviews, numTickets, numFriends;
    ImageView userImage;

    BasicProfileInfo basicProfileInfo;

    View progressBar;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (BaseActivity) context;
        eventsContext = getArguments().getParcelable("events_context");
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_user_profile, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.top_progress_bar);
        userImage = (ImageView) view.findViewById(R.id.profile_image);
        userName = (TextView) view.findViewById(R.id.profile_user_name);

        userEmail = (TextView) view.findViewById(R.id.profile_user_email);
        numInterests = (TextView) view.findViewById(R.id.num_interests);
        numFavourites = (TextView) view.findViewById(R.id.num_favourites);
        numReviews = (TextView) view.findViewById(R.id.num_reviews);
        numTickets = (TextView) view.findViewById(R.id.num_tickets);
        numFriends = (TextView) view.findViewById(R.id.num_friends);
        profileLayout = (LinearLayout) view.findViewById(R.id.profile_layout);


        LinearLayout myInterests = (LinearLayout) view.findViewById(R.id.my_interests);
        LinearLayout myFavourites = (LinearLayout) view.findViewById(R.id.my_favourites);
        LinearLayout myTickets = (LinearLayout) view.findViewById(R.id.my_tickets);
        LinearLayout myReviews = (LinearLayout) view.findViewById(R.id.my_reviews);
        LinearLayout myFriends = (LinearLayout) view.findViewById(R.id.my_friends);
        final Account.UserInfo info = new Account(getActivity()).getUserInfo();
        myInterests.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EventsContext myEventsContext = new EventsContext(eventsContext.location,
                        EventsHighEndpoints.QUERY_MY_INTEREST_EVENTS);
                Intent intent = new Intent(getActivity(), MyInterestsActivity.class);
                intent.putExtra("events_context", myEventsContext);
                if (basicProfileInfo != null) {
                    intent.putExtra("profile_id", basicProfileInfo.getEmail());
                } else {
                    intent.putExtra("profile_id", info.email);

                }
                startActivity(intent);

            }
        });

        myFavourites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                City lastCity = (new Account(getActivity())).getLastCity();
                LatLng latLng;
                if (lastCity != null) {
                    latLng = (new Account(getActivity())).getLastCity().cityBounds.getCenter();
                } else {
                    latLng = City.BANGALORE.cityBounds.getCenter();
                }
                EventsContext myEventsContext = new EventsContext(latLng, EventsHighEndpoints.QUERY_MY_EVENT);
                Intent intent = new Intent(getActivity(), MyFavouritesActivity.class);
                intent.putExtra("events_context", myEventsContext);
                if (basicProfileInfo != null) {
                    intent.putExtra("profile_id", basicProfileInfo.getEmail());
                } else {
                    intent.putExtra("profile_id", info.email);

                }
                startActivity(intent);
            }
        });
        myTickets.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MyTicketsActivity.class);
                if (basicProfileInfo != null) {
                    intent.putExtra("profile_id", basicProfileInfo.getEmail());
                } else {
                    intent.putExtra("profile_id", info.email);

                }
                startActivity(intent);
            }
        });
        myReviews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EventsContext myEventsContext = new EventsContext(eventsContext.location,
                        EventsHighEndpoints.QUERY_MY_INTEREST_EVENTS);
                Intent intent = new Intent(getActivity(), MyReviewsActivity.class);
                intent.putExtra("events_context", myEventsContext);
                if (basicProfileInfo != null) {
                    intent.putExtra("profile_id", basicProfileInfo.getEmail());
                } else {
                    intent.putExtra("profile_id", info.email);

                }
                startActivity(intent);
            }
        });
        myFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), MyFriendsActivity.class);
                if (basicProfileInfo != null) {
                    intent.putExtra("profile_id", basicProfileInfo.getEmail());
                } else {
                    intent.putExtra("profile_id", info.email);

                }
                startActivity(intent);
            }
        });
        loadBasicProfile(info.email);

    }

    public void loadBasicProfile(String profileId) {
        progressBar.setVisibility(View.VISIBLE);
        profileLayout.setVisibility(View.GONE);
        FetchBasicUserProfileRequest.submit(getActivity(), profileId, Request.Priority.HIGH,
                new Response.Listener<BasicProfileInfo>() {
                    @Override
                    public void onResponse(BasicProfileInfo basicProfileInfo, boolean b) {
                        if (basicProfileInfo != null) {
                            NewUserProfileFragment.this.basicProfileInfo = basicProfileInfo;
                        }
                        setData();
                        progressBar.setVisibility(View.GONE);
                        profileLayout.setVisibility(View.VISIBLE);
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        setData();
                        progressBar.setVisibility(View.GONE);
                        profileLayout.setVisibility(View.VISIBLE);
                    }
                }, true);
    }

    public void setData() {
        if (basicProfileInfo != null) {
            userName.setText(basicProfileInfo.getName());
            userEmail.setText(basicProfileInfo.getEmail());
            if (!Utils.checkIfStringEmpty(basicProfileInfo.getProfilePic())) {
                Glide.with(activity).load(basicProfileInfo.getProfilePic())
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.com_facebook_profile_picture_blank_portrait).crossFade().centerCrop()
                        .into(userImage);

            }
            if (basicProfileInfo.getNumInterests() > 0) {
                numInterests.setVisibility(View.VISIBLE);
                numInterests.setText(basicProfileInfo.getNumInterests() + "");
            } else {
                numInterests.setVisibility(View.GONE);
            }
            if (basicProfileInfo.getNumFavourites() > 0) {
                numFavourites.setVisibility(View.VISIBLE);
                numFavourites.setText(basicProfileInfo.getNumFavourites() + "");
            } else {
                numFavourites.setVisibility(View.GONE);
            }

            if (basicProfileInfo.getNumFriends() > 0) {
                numFriends.setVisibility(View.VISIBLE);
                numFriends.setText(basicProfileInfo.getNumFriends() + "");
            } else {
                numFriends.setVisibility(View.GONE);
            }

            if (basicProfileInfo.getNumReviews() > 0) {
                numReviews.setVisibility(View.VISIBLE);
                numReviews.setText(basicProfileInfo.getNumReviews() + "");
            } else {
                numReviews.setVisibility(View.GONE);
            }

            if (basicProfileInfo.getNumTickets() > 0) {
                numTickets.setVisibility(View.VISIBLE);
                numTickets.setText(basicProfileInfo.getNumTickets() + "");
            } else {
                numTickets.setVisibility(View.GONE);
            }

        } else {
            Account.UserInfo userInfo = new Account(getActivity()).getUserInfo();
            userName.setText(userInfo.name);
            userEmail.setText(userInfo.email);
            if (!Utils.checkIfStringEmpty(userInfo.profilePic)) {
                Glide.with(activity).load(userInfo.profilePic)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.com_facebook_profile_picture_blank_portrait).crossFade().centerCrop()
                        .into(userImage);
            }
            numInterests.setVisibility(View.GONE);

            numFavourites.setVisibility(View.GONE);

            numFriends.setVisibility(View.GONE);

            numReviews.setVisibility(View.GONE);

            numTickets.setVisibility(View.GONE);
        }

    }


}
