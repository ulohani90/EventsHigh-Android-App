package com.eventshigh.nearme.app.ui.adapter;

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
import com.eventshigh.nearme.app.data.HotDealsObject;
import com.eventshigh.nearme.app.utils.DateTimeUtils;

/**
 * Created by umesh on 12/12/17.
 */

public class HotDealsCard extends RecyclerView.ViewHolder {

    TextView dealText;
    TextView validTill;
    TextView grabNowText;
    LinearLayout parent;

    public static HotDealsCard newInstance(final BaseActivity activity, ViewGroup parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.hot_deals_card, parent, false);
        return new HotDealsCard(view);
    }


    public HotDealsCard(View itemView) {
        super(itemView);
        dealText = (TextView) itemView.findViewById(R.id.deal_text);
        validTill = (TextView) itemView.findViewById(R.id.deal_valid_till);
        grabNowText = (TextView) itemView.findViewById(R.id.grab_now_text);
        parent = (LinearLayout) itemView.findViewById(R.id.parent);
    }

    public void bindData(final BaseContextActivity activity, final HotDealsObject data) {
        dealText.setText(data.getOfferText());
        String dealDateTime = DateTimeUtils.getDealDateTimeFromLongTime(data.getOfferRemovalDate());
        if (dealDateTime != null) {
            validTill.setText("Valid till " + dealDateTime);
            validTill.setVisibility(View.VISIBLE);
        } else {
            validTill.setVisibility(View.GONE);
        }
        SpannableString string = new SpannableString("Grab Now");
        string.setSpan(new UnderlineSpan(), 0, string.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        grabNowText.setText(string);
        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.showEventDetails(data.getEventId(), null, null);
            }
        });

    }


}