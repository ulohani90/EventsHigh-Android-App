package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.HotDealsObject;

/**
 * Created by umesh on 12/12/17.
 */

public class HelloBarDealData implements AdapterData {


    BaseContextActivity activity;
    HotDealsObject dealsObject;
    boolean showHomePageNeedData;

    public HelloBarDealData(BaseContextActivity activity, HotDealsObject dealsObject, boolean showHomePageNeedData) {
        this.activity = activity;
        this.dealsObject = dealsObject;
        this.showHomePageNeedData = showHomePageNeedData;
    }

    @Override
    public DataType getType() {
        return DataType.HELLOBAR_DEAL_CARD;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((HelloBarDealCard) card).bindData(activity, dealsObject, showHomePageNeedData);
    }

    @Override
    public String getId() {
        return dealsObject.getOfferText();
    }
}
