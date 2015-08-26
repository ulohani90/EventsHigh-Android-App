package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;

import com.eventshigh.nearme.app.activity.BaseActivity;

public class EhInviteData implements AdapterData {
    private final BaseActivity activity;

    public EhInviteData(final BaseActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        ((EhInviteCard) card).populate(activity);
    }

    @Override
    public String getId() {
        return EhInviteData.class.getCanonicalName();
    }

    @Override
    public DataType getType() {
        return DataType.EH_INVITE;
    }
}
