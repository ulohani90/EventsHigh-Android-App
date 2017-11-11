package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;

/**
 * Created by umesh on 09/11/17.
 */

public class BrowseHeaderCardLayout extends RecyclerView.ViewHolder {

    TextView headerTitle;

    public static BrowseHeaderCardLayout newInstance(BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.browse_header_card_layout, parent, false);
        return new BrowseHeaderCardLayout(view);
    }

    public BrowseHeaderCardLayout(View itemView) {
        super(itemView);
        headerTitle = (TextView) itemView.findViewById(R.id.header_title);
    }

    public void populate(final BaseActivity activity, String text) {
        headerTitle.setText(text);
    }
}
