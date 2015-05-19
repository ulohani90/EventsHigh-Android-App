package com.eventshigh.nearme.app.ui;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.activity.BaseActivity;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.OffersAdapter.OfferCard;
import com.eventshigh.nearme.app.user.Preferences;

import java.util.List;

/**
 * Adapter used to show offers information in RecyclerView.
 */
public class OffersAdapter extends RecyclerView.Adapter<OfferCard> {
    private final BaseActivity activity;
    private final List<Offer> offers;

    public OffersAdapter(BaseActivity activity, List<Offer> offers) {
        this.activity = activity;
        this.offers = offers;
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? 0 : 1;
    }

    @Override
    public OfferCard onCreateViewHolder(ViewGroup parent, int type) {
        if (type == 0) {
            View view = activity.getLayoutInflater().inflate(R.layout.card_offer_header, parent, false);
            return new OfferCard(view);
        }

        View view = activity.getLayoutInflater().inflate(R.layout.card_offer, parent, false);
        return new OfferCard(view);
    }

    @Override
    public void onBindViewHolder(OfferCard card, int position) {
        if (position == 0) {
            TextView numPointsView = (TextView) card.itemView.findViewById(R.id.num_points);
            numPointsView.setText(Preferences.getInstance(activity).getPoints());
        }
        if (position > 0) {
            card.populate(offers.get(position - 1), activity);
        }
    }

    @Override
    public int getItemCount() {
        return offers.size() + 1;
    }

    public static class OfferCard extends ViewHolder {
        private final NetworkImageView imageView;
        private final TextView messageView;
        private final TextView actionView;
        private final View expiredView;

        public OfferCard(View itemView) {
            super(itemView);

            imageView = (NetworkImageView) itemView.findViewById(R.id.offer_image);
            messageView = (TextView) itemView.findViewById(R.id.offer_message);
            actionView = (TextView) itemView.findViewById(R.id.offer_action);
            expiredView = itemView.findViewById(R.id.expired);
        }

        public void populate(Offer offer, final BaseActivity activity) {
            imageView.setDefaultImageResId(R.drawable.eh_default_event);
            imageView.setDefaultImageResId(R.drawable.eh_default_event);
            imageView.setImageUrl(offer.imgUrl.toString(), VolleyHelper.getImageLoader(activity));

            messageView.setText(offer.message);

            actionView.setText(offer.actionName);
            actionView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(activity, "To Be Implemented ...", Toast.LENGTH_SHORT).show();
                }
            });
            expiredView.setVisibility(offer.isExpired() ? View.VISIBLE : View.GONE);
        }

    }
}
