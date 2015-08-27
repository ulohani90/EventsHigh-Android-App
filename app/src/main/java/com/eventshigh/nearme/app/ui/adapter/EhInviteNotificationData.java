package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseActivity;

public class EhInviteNotificationData implements AdapterData {
    private final BaseActivity activity;

    public EhInviteNotificationData(final BaseActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        ((EhInviteNotificationCard) card).populate(activity);
    }

    @Override
    public String getId() {
        return EhInviteData.class.getCanonicalName();
    }

    @Override
    public DataType getType() {
        return DataType.EH_INVITE_NOTIFICATION;
    }
}
