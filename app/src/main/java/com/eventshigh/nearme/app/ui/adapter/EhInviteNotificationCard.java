package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;

public class EhInviteNotificationCard extends ViewHolder {
    private final View shareAppButton;

    public static EhInviteNotificationCard newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_eh_invite_notification, parent, false);
        return new EhInviteNotificationCard(view);
    }

    public EhInviteNotificationCard(View itemView) {
        super(itemView);

        shareAppButton = itemView.findViewById(R.id.share_app);
    }

    public void populate(final BaseActivity activity) {
        shareAppButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.shareApp();
            }
        });
    }
}
