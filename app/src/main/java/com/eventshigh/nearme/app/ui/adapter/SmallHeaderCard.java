package com.eventshigh.nearme.app.ui.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;

public class SmallHeaderCard extends ViewHolder {
    private final TextView titleView;

    public static SmallHeaderCard newInstance(Activity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_header_small, parent, false);
        return new SmallHeaderCard(view);
    }

    public SmallHeaderCard(View cardView) {
        super(cardView);
        this.titleView = (TextView) cardView.findViewById(R.id.header);
    }

    public void bindHeaderView(final SmallHeaderData header) {
        titleView.setText(header.header);
    }
}
