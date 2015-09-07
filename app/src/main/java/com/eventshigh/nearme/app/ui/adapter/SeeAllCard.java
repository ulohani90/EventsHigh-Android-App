package com.eventshigh.nearme.app.ui.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.utils.Utils;

public class SeeAllCard extends ViewHolder {
    private final TextView seeAllView;

    public static SeeAllCard newInstance(Activity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.card_see_all, parent, false);
        return new SeeAllCard(view);
    }

    public SeeAllCard(View cardView) {
        super(cardView);
        this.seeAllView = (TextView) cardView.findViewById(R.id.see_all);
    }

    public void bindHeaderView(final BaseContextActivity activity, final SeeAllData seeAllData) {
        seeAllView.setText("See All " + Utils.capitalize(seeAllData.category) + " Events");
        seeAllView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.seeAll();
            }
        });
    }
}
