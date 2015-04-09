package com.eventshigh.nearme.app.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.network.OffersRequest;
import com.eventshigh.nearme.app.ui.OffersAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

public class OffersActivity extends BaseActivity {
    public static final String OFFER_EXTRA_PARAM = OffersActivity.class.getName() + "_offer";

    private AutofitRecyclerView offersView;
    private View retryView;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Parse the intent.
        Offer offer = null;
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra(OFFER_EXTRA_PARAM)) {
            offer = intent.getParcelableExtra(OFFER_EXTRA_PARAM);
        }

        if (offer != null) {
            setOfferView(offer);
            return;
        }

        // No offer in incoming intent. Show all offers.
        setContentView(R.layout.activity_offers);
        offersView = (AutofitRecyclerView) findViewById(R.id.offers);
        retryView = findViewById(R.id.retry);
        progressBar = findViewById(R.id.top_progress_bar);
        onRetry(null);
    }

    public void claimOffer(View view) {
        shareApp();
    }

    public void shareApp(View view) {
        shareApp();
    }

    public void onRetry(View view) {
        progressBar.setVisibility(View.VISIBLE);
        OffersRequest.submit(this, Priority.IMMEDIATE, new Listener<List<Offer>>() {
            @Override
            public void onResponse(List<Offer> offers, boolean isIntermediate) {
                progressBar.setVisibility(View.GONE);
                offersView.setVisibility(View.VISIBLE);
                retryView.setVisibility(View.GONE);

                OffersAdapter adapter = new OffersAdapter(OffersActivity.this, offers);
                offersView.setAdapter(adapter);
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                progressBar.setVisibility(View.GONE);
                offersView.setVisibility(View.GONE);
                retryView.setVisibility(View.VISIBLE);

                Log.w(OffersActivity.class.getSimpleName(),
                        "Failed to fetch offers: " + volleyError.getMessage(), volleyError.getCause());
            }
        });
    }

    private void setOfferView(Offer offer) {
        setContentView(R.layout.activity_offer);

        TextView messageView = (TextView) findViewById(R.id.offer_message);
        messageView.setText(offer.message);

        View shareAppButton = findViewById(R.id.share_app);
        shareAppButton.setVisibility(offer.isExpired() ? View.GONE : View.VISIBLE);

        TextView claimCountView = (TextView) findViewById(R.id.claim_count);
        claimCountView.setText(Integer.toString(offer.claimCount));

        View claimButton = findViewById(R.id.claim_offer);
        claimButton.setEnabled(offer.claimCount > 0);
    }
}
