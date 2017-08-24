package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.NewSocialFriend;
import com.eventshigh.nearme.app.network.FetchUserFriendsRequest;
import com.eventshigh.nearme.app.network.FetchUserInterestsRequest;
import com.eventshigh.nearme.app.network.MyEventsRequest;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.ui.adapter.NewContactsAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;

import java.util.List;

/**
 * Created by umesh on 23/08/17.
 */

public class MyFriendsActivity extends BaseActivity {

    View progressBar;
    View noEventLayout;
    AutofitRecyclerView recyclerView;
    NewContactsAdapter mAdapter;
    String profileId;
    View retryView;
    TextView noMyEventHeading;
    TextView exploreEvents;
    TextView noFriendsText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recyclerview_with_progress_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("My Friends");
        profileId = getIntent().getStringExtra("profile_id");
        progressBar = findViewById(R.id.top_progress_bar);
        noEventLayout = findViewById(R.id.view_no_my_event);
        retryView = findViewById(R.id.view_retry);
        noFriendsText = (TextView) findViewById(R.id.no_tickets_view);
        noFriendsText.setText("No Friends");
        retryView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadUserFriends(true);
            }
        });
        noMyEventHeading = (TextView) findViewById(R.id.no_my_event_heading);
        noMyEventHeading.setText(getResources().getString(R.string.ui_no_my_interest));
        recyclerView = (AutofitRecyclerView) findViewById(R.id.event_grid);
        exploreEvents = (TextView) findViewById(R.id.explore_events);
        exploreEvents.setText("Personalize");
        mAdapter = new NewContactsAdapter(this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setVisibility(View.GONE);
        loadUserFriends(true);

    }

    public void onRetry(View view) {
        loadUserFriends(true);
    }

    private void loadUserFriends(boolean shouldByPassChange) {
        progressBar.setVisibility(View.VISIBLE);
        retryView.setVisibility(View.GONE);
        FetchUserFriendsRequest.submit(this, profileId, Request.Priority.HIGH,
                new Response.Listener<List<NewSocialFriend>>() {
                    @Override
                    public void onResponse(List<NewSocialFriend> friendsList, boolean b) {

                        mAdapter.setFriendList(friendsList);
                        mAdapter.setOnInviteBtnClick(new NewContactsAdapter.OnInviteBtnClick() {
                            @Override
                            public void onInviteBtnClick() {
                                showInviteDialog();
                            }
                        });
                        recyclerView.setVisibility(View.VISIBLE);
                        if (friendsList != null && friendsList.size() > 0) {
                            noFriendsText.setVisibility(View.GONE);
                            progressBar.setVisibility(View.GONE);
                            noEventLayout.setVisibility(View.GONE);
                            retryView.setVisibility(View.GONE);
                        } else {
                            noFriendsText.setVisibility(View.VISIBLE);
                            progressBar.setVisibility(View.GONE);
                            noEventLayout.setVisibility(View.GONE);
                            retryView.setVisibility(View.GONE);
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        progressBar.setVisibility(View.GONE);
                        noFriendsText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.GONE);
                        noEventLayout.setVisibility(View.GONE);
                        retryView.setVisibility(View.VISIBLE);
                    }
                }, shouldByPassChange);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();

        }
        return super.onOptionsItemSelected(item);
    }

    public void showInviteDialog() {
        reportActionToAnalytics("inviteFbFriends");

        String appLinkUrl, previewImageUrl;

        appLinkUrl = "https://fb.me/528284797367992";
        previewImageUrl = "https://s3-us-west-2.amazonaws.com/ehasset/eh_tag_logo.jpg";

        if (AppInviteDialog.canShow()) {
            AppInviteContent content = new AppInviteContent.Builder()
                    .setApplinkUrl(appLinkUrl)
                    .setPreviewImageUrl(previewImageUrl)
                    .build();
            AppInviteDialog dialog = new AppInviteDialog(this);
            CallbackManager sCallbackManager = CallbackManager.Factory.create();
            dialog.registerCallback(sCallbackManager, new FacebookCallback<AppInviteDialog.Result>() {
                @Override
                public void onSuccess(AppInviteDialog.Result result) {
                    if (!isDestroyed)
                        Toast.makeText(MyFriendsActivity.this, "Invitation Send Successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancel() {
                }

                @Override
                public void onError(FacebookException e) {
                    if (!isDestroyed)
                        Toast.makeText(MyFriendsActivity.this, "Could not send Invite. Try Again.", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.show(this, content);
        }
    }

    boolean isDestroyed;

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        super.onDestroy();

    }
}
