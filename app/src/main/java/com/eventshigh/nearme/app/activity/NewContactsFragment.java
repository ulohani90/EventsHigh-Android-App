package com.eventshigh.nearme.app.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.ProfileInfo;
import com.eventshigh.nearme.app.ui.adapter.NewContactsAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;


/**
 * Created by umesh on 21/07/16.
 */
public class NewContactsFragment extends Fragment {


    AutofitRecyclerView gridView;
    private View topProgressBar;
    private View retryView;
    private View noFriendsOnEhView;
    private ProfileInfo profileInfo;


    public static NewContactsFragment newInstance(Bundle args) {
        NewContactsFragment fragment = new NewContactsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    BaseActivity activity;


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.activity = (BaseActivity) context;
        if (getArguments() != null && getArguments().getParcelable("profile_info") != null)
            profileInfo = getArguments().getParcelable("profile_info");
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        (view.findViewById(R.id.top_progress_bar)).setVisibility(View.GONE);
        gridView = (AutofitRecyclerView) view.findViewById(R.id.grid);
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setEnabled(false);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NewContactsAdapter adapter = new NewContactsAdapter((BaseActivity) getActivity());
        adapter.setFriendList(profileInfo.getUserContactList());
        adapter.setOnInviteBtnClick(new NewContactsAdapter.OnInviteBtnClick() {
            @Override
            public void onInviteBtnClick() {
                showInviteDialog();
            }
        });
        gridView.setAdapter(adapter);
        // More views.
        topProgressBar = view.findViewById(R.id.top_progress_bar);
        noFriendsOnEhView = view.findViewById(R.id.view_no_friends_on_eh);
        retryView = view.findViewById(R.id.view_retry);
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    public void showInviteDialog() {
        activity.reportActionToAnalytics("inviteFbFriends");

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
                    if (isAdded() && getActivity() != null)
                        Toast.makeText(getActivity(), "Invitation Send Successfully", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancel() {
                }

                @Override
                public void onError(FacebookException e) {
                    if (isAdded() && getActivity() != null)
                        Toast.makeText(getActivity(), "Could not send Invite. Try Again.", Toast.LENGTH_SHORT).show();
                }
            });

            dialog.show(this, content);
        }
    }

}
