package com.eventshigh.nearme.app.ui.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;

import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;

public class FacebookAppInviteData implements AdapterData {
    private final Activity activity;

    public FacebookAppInviteData(Activity activity) {
        this.activity = activity;
    }

    @Override
    public DataType getType() {
        return DataType.FACEBOOK_APP_INVITE;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        card.itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                AppInviteContent content = new AppInviteContent.Builder()
                        .setApplinkUrl("https://fb.me/419675911562215")
                        .setPreviewImageUrl("https://assets.eventshigh.com/trending_topics/party.jpg")
                        .build();
                AppInviteDialog.show(activity, content);
            }
        });
    }

    @Override
    public String getId() {
        return getType().toString();
    }
}
