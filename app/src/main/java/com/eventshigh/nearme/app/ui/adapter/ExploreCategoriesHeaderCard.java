package com.eventshigh.nearme.app.ui.adapter;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.EventsGridActivity;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;

/**
 * Created by umesh on 11/06/16.
 */
public class ExploreCategoriesHeaderCard extends RecyclerView.ViewHolder {


    ImageView whatsHot, nearBy, summerCampImage;

    FrameLayout summerCampLayout;

    LinearLayout whatsHotlayout;



    public static ExploreCategoriesHeaderCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.explore_categories_header_card, parent, false);
        return new ExploreCategoriesHeaderCard(view);
    }

    public ExploreCategoriesHeaderCard(View itemView) {
        super(itemView);
        whatsHot = (ImageView) itemView.findViewById(R.id.whats_hot);
        nearBy = (ImageView) itemView.findViewById(R.id.nearby);
        summerCampImage = (ImageView) itemView.findViewById(R.id.holi_img);
        summerCampLayout = (FrameLayout) itemView.findViewById(R.id.holi_layout);
        whatsHotlayout = (LinearLayout) itemView.findViewById(R.id.whats_hot_layout);

    }

    public void bindData(final BaseContextActivity activity, boolean showSummerCampCard, boolean showChristmasTab, int width) {
        whatsHot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showSearchView(EventsHighEndpoints.QUERY_FEATURED);
            }
        });
        Drawable drawable = activity.getDrawable(R.drawable.ic_summer_camp);
        int height = (drawable.getIntrinsicHeight() * width) / drawable.getIntrinsicWidth();
        summerCampLayout.getLayoutParams().width = width;
        summerCampLayout.getLayoutParams().height = height;

        if (!showSummerCampCard) {
            summerCampLayout.setVisibility(View.GONE);
            summerCampImage.setVisibility(View.GONE);
        } else {
            summerCampLayout.setVisibility(View.VISIBLE);
            summerCampImage.setVisibility(View.VISIBLE);
            summerCampImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.showSearchView("summer camp");

                }
            });
        }

        nearBy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("showSearchView", "nearme");
                EventsContext param = null;
                Account account = new Account(activity);
                if (account.getLastLocality() != null) {
                    param = new EventsContext(account.getLastLocality().getLatLng(), EventsHighEndpoints.QUERY_NEARME + " (" + account.getLastLocality().getName() + ")");
                } else if (account.getLastCity() != null) {
                    param = new EventsContext(account.getLastCity().cityBounds.getCenter(), EventsHighEndpoints.QUERY_NEARME);
                } else {

                }
                if (param != null) {
                    param.removeDateFilter();
                    Intent intent = new Intent(activity, EventsGridActivity.class)
                            .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, param);
                    activity.startActivity(intent);
                }
            }
        });
    }

}
