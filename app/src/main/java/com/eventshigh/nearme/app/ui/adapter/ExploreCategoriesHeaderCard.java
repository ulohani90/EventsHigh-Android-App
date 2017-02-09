package com.eventshigh.nearme.app.ui.adapter;

import android.content.Intent;
import android.provider.Settings;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.EventsGridActivity;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;

/**
 * Created by umesh on 11/06/16.
 */
public class ExploreCategoriesHeaderCard extends RecyclerView.ViewHolder {


    ImageView whatsHot, nearBy, newYearImage;

    public static ExploreCategoriesHeaderCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.explore_categories_header_card, parent, false);
        return new ExploreCategoriesHeaderCard(view);
    }

    public ExploreCategoriesHeaderCard(View itemView) {
        super(itemView);
        whatsHot = (ImageView) itemView.findViewById(R.id.whats_hot);
        nearBy = (ImageView) itemView.findViewById(R.id.nearby);
        newYearImage = (ImageView) itemView.findViewById(R.id.new_year_img);
    }

    public void bindData(final BaseContextActivity activity, boolean showNewYearCard) {
        whatsHot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showSearchView(EventsHighEndpoints.QUERY_FEATURED);
            }
        });

        if (!showNewYearCard) {
            newYearImage.setVisibility(View.GONE);
        } else {
            newYearImage.setVisibility(View.VISIBLE);
            newYearImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.showSearchView("New Year Parties");
                }
            });
        }


        nearBy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("showSearchView", "nearme");
                EventsContext param;
                Account account = new Account(activity);
                if (account.getLastLocality() != null) {
                    param = new EventsContext(account.getLastLocality().getLatLng(), EventsHighEndpoints.QUERY_NEARME + " (" + account.getLastLocality().getName() + ")");
                } else {
                    param = new EventsContext(account.getLastCity().cityBounds.getCenter(), EventsHighEndpoints.QUERY_NEARME);
                }

                param.removeDateFilter();
                Intent intent = new Intent(activity, EventsGridActivity.class)
                        .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
                activity.startActivity(intent);
            }
        });
    }

}
