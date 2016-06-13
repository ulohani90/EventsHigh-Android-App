package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;

/**
 * Created by umesh on 11/06/16.
 */
public class ExploreCategoriesHeaderCard extends RecyclerView.ViewHolder {


    ImageView whatsHot, nearBy;

    public static ExploreCategoriesHeaderCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.explore_categories_header_card, parent, false);
        return new ExploreCategoriesHeaderCard(view);
    }

    public ExploreCategoriesHeaderCard(View itemView) {
        super(itemView);
        whatsHot = (ImageView) itemView.findViewById(R.id.whats_hot);
        nearBy = (ImageView) itemView.findViewById(R.id.nearby);
    }

    public void bindData(final BaseContextActivity activity) {
        whatsHot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showSearchView(EventsHighEndpoints.QUERY_FEATURED);
            }
        });


        nearBy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
    }

}
