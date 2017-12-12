package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.HotDealsObject;

/**
 * Created by umesh on 12/12/17.
 */

public class HotDealsData implements AdapterData {

    BaseContextActivity activity;

    HotDealsObject dealsObject;

    public HotDealsData(BaseContextActivity activity, HotDealsObject dealsObject) {
        this.activity = activity;
        this.dealsObject = dealsObject;
    }

    @Override
    public DataType getType() {
        return DataType.HOT_DEALS_CARD;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((HotDealsCard) card).bindData(activity, dealsObject);
    }

    @Override
    public String getId() {
        return dealsObject.getOfferText();
    }
}
