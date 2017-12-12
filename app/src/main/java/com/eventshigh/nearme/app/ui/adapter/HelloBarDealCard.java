package com.eventshigh.nearme.app.ui.adapter;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.activity.ShowDealsActivity;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.HotDealsObject;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

/**
 * Created by umesh on 12/12/17.
 */

public class HelloBarDealCard extends RecyclerView.ViewHolder {

    TextView dealText;

    TextView viewAllOffers;

    LinearLayout parent;

    public static HelloBarDealCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.hello_bar_deal_card, parent, false);
        return new HelloBarDealCard(view);
    }


    public HelloBarDealCard(View itemView) {
        super(itemView);
        dealText = (TextView) itemView.findViewById(R.id.deal_text);
        viewAllOffers = (TextView) itemView.findViewById(R.id.view_all_deals);

        parent = (LinearLayout) itemView.findViewById(R.id.parent);
    }

    public void bindData(final BaseContextActivity activity, final HotDealsObject data, boolean showHomePageNeedData) {
        dealText.setText(data.getOfferText());
        if (showHomePageNeedData) {
            viewAllOffers.setVisibility(View.VISIBLE);
            SpannableString string = new SpannableString("View All Deals");
            string.setSpan(new UnderlineSpan(), 0, string.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            viewAllOffers.setText(string);
            parent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });
            viewAllOffers.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(activity, ShowDealsActivity.class);
                    activity.startActivity(intent);
                }
            });
        } else {
            viewAllOffers.setVisibility(View.GONE);
        }
    }


}
