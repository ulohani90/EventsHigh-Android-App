package com.eventshigh.nearme.app.ui;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.ViewGroup;

import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.ui.EventsAdapter.OfferCard;

import java.util.List;

/**
 * Adapter used to show offers information in RecyclerView.
 */
public class OffersAdapter extends RecyclerView.Adapter<ViewHolder> {
    private final BaseActivity activity;
    private final List<Offer> offers;

    public OffersAdapter(BaseActivity activity, List<Offer> offers) {
        this.activity = activity;
        this.offers = offers;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
        return OfferCard.newInstance(activity, viewGroup);
    }

    @Override
    public void onBindViewHolder(ViewHolder card, int position) {
        offers.get(position).populateOfferCard(card.itemView, activity);
    }

    @Override
    public int getItemCount() {
        return offers.size();
    }
}
