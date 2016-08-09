package com.eventshigh.nearme.app.ui.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.ReferralActivity;
import com.eventshigh.nearme.app.ui.FBSigninDialog;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.view.ContactListView;

import java.text.MessageFormat;

public class FollowCard extends ViewHolder {
    private TextView titleView;
    private View followButton;
    private View followingButton;
    private TextView followersCount;
    private TextView eventsCount;


    public static FollowCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_follow, parent, false);
        return new FollowCard(view);
    }

    public FollowCard(View itemView) {
        super(itemView);

        titleView = (TextView) itemView.findViewById(R.id.cat_title);

        followersCount = (TextView) itemView.findViewById(R.id.followers_count);
        eventsCount = (TextView) itemView.findViewById(R.id.events_count);
        followButton = itemView.findViewById(R.id.follow_button);
        followingButton = itemView.findViewById(R.id.following_button);
    }

    public void populate(final FollowData data) {
        titleView.setText(data.title);

        followersCount.setText(data.numFollowers + " Followers");
        eventsCount.setText(data.numEvents + " Events");

        final Account account = new Account(data.activity);
        setFollowButtons(account.isFollowing(data.title));
        followButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                data.activity.reportActionToAnalytics("addFollowing", data.title);
                if (!account.getUserInfo().isSignedIn) {
                    FBSigninDialog.show(data.activity, R.string.ui_signin_via_fb, R.string.ui_signin_fb_plan_more, 1);
                }
                account.setIsFollowing(data.title, true);
                setFollowButtons(true);
            }
        });
        followingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                data.activity.reportActionToAnalytics("removeFollowing", data.title);
                account.setIsFollowing(data.title, false);
                setFollowButtons(false);
            }
        });


    }

    public void setFollowButtons(boolean isFollowing) {
        followButton.setVisibility(isFollowing ? View.GONE : View.VISIBLE);
        followingButton.setVisibility(isFollowing ? View.VISIBLE : View.GONE);
        followingButton.setSelected(true);
    }


}
