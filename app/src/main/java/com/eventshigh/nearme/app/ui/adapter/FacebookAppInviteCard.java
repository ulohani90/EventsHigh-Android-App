package com.eventshigh.nearme.app.ui.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;

public class FacebookAppInviteCard extends ViewHolder {
    public static FacebookAppInviteCard newInstance(Activity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_invite_facebook, parent, false);
        return new FacebookAppInviteCard(view);
    }

    public FacebookAppInviteCard(View cardView) {
        super(cardView);
    }
}
