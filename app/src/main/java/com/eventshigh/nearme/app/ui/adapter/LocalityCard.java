package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.utils.Utils;

/**
 * Created by umesh on 11/03/16.
 */
public class LocalityCard extends RecyclerView.ViewHolder {

private TextView titleView;
    private BaseContextActivity activity;
    public static LocalityCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_explore_location, parent, false);
        return new LocalityCard(view);
    }

    public LocalityCard(View itemView) {
        super(itemView);

        titleView = (TextView)itemView.findViewById(R.id.cat_title);
    }

    public void populateTrendingCategoryData(final BaseContextActivity activity,final LocalityData data,int color) {

        titleView.setText(data.locality.name);
        titleView.setBackgroundColor(activity.getResources().getColor(color));

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showSearchView(data.locality.name);
            }
        });

        Utils.waitForViewVisible(titleView, new Runnable() {
            @Override
            public void run() {
                ViewGroup.LayoutParams lp = titleView.getLayoutParams();
                lp.height = 3 * titleView.getWidth() / 4;
                titleView.setLayoutParams(lp);
            }
        });
    }
}
