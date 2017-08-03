package com.eventshigh.nearme.app.ui.adapter;

import android.support.v7.widget.RecyclerView;

import com.eventshigh.nearme.app.activity.BaseContextActivity;
import com.eventshigh.nearme.app.data.stream.OfferObject;

/**
 * Created by umesh on 16/04/16.
 */
public class OfferData implements AdapterData{

    public final OfferObject offer;
    public final BaseContextActivity activity;
    public final long totalPoints;
    @Override
    public DataType getType() {
        return DataType.OFFER;
    }

    public OfferData(OfferObject offer,BaseContextActivity activity,long totalPoints){
        this.activity = activity;
        this.offer = offer;
        this.totalPoints = totalPoints;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder card, int position) {
        ((OfferCard)card).bindOfferView(offer,activity,totalPoints);
    }

    @Override
    public String getId() {
        return offer.id+" ";
    }
}
