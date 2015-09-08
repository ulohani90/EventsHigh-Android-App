package com.eventshigh.nearme.app.ui;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.View.OnClickListener;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.EventDetailActivity;
import com.eventshigh.nearme.app.activity.PlanActivity;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Account.UserInfo;

public class InviteFriendsDialog {
    public static void show(final BaseActivity activity, final @Nullable Event event,
                            @Nullable final String planId) {
        UserInfo userInfo = new Account(activity).getUserInfo();
        if (event == null || userInfo.name == null || userInfo.phoneNo == null) {
            return;
        }

        final AlertDialog alertDialog = new AlertDialog.Builder(activity)
                .setView(R.layout.dialog_invite_friends)
                .setCancelable(true)
                .create();
        alertDialog.show();

        alertDialog.findViewById(R.id.title).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        alertDialog.findViewById(R.id.invite_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportEventAction(event, "checkWithFriends", "inviteDialog");

                Intent intent = new Intent(activity, PlanActivity.class);
                intent.putExtra(EventDetailActivity.EXTRA_EVENT_PARAM, event);
                if (planId != null) {
                    intent.putExtra(EventDetailActivity.EXTRA_PLAN_ID_PARAM, planId);
                }
                activity.startActivity(intent);
            }
        });
    }
}
