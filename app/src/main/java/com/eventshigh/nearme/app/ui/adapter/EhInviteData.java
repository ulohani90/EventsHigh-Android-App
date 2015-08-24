package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;

public class EhInviteData implements AdapterData {
    private final BaseActivity activity;

    public EhInviteData(final BaseActivity activity) {
        this.activity = activity;
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        card.itemView.findViewById(R.id.share_app).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.shareApp();
            }
        });
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
