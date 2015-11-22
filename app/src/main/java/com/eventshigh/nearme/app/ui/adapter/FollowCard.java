package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.user.Account;

import java.text.MessageFormat;

public class FollowCard extends ViewHolder {
    private TextView titleView;
    private TextView subtitleView;
    private View followButton;
    private View followingButton;

    public static FollowCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_follow, parent, false);
        return new FollowCard(view);
    }

    public FollowCard(View itemView) {
        super(itemView);

        titleView = (TextView) itemView.findViewById(R.id.cat_title);
        subtitleView = (TextView) itemView.findViewById(R.id.subtitle);
        followButton = itemView.findViewById(R.id.follow_button);
        followingButton = itemView.findViewById(R.id.following_button);
    }

    public void populate(final FollowData data) {
        titleView.setText(data.title);
        subtitleView.setText(MessageFormat.format(
                data.activity.getString(R.string.num_events), data.numFollowers, data.numEvents));

        final Account account = new Account(data.activity);
        setFollowButtons(account.isFollowing(data.title));
        followButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                data.activity.reportActionToAnalytics("addFollowing", data.title);
                if (!account.getUserInfo().isVerified) {
                    PhoneVerificationDialog.show(data.activity,
                            R.string.ui_verify_phone, R.string.ui_phone_verify_pa);
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
