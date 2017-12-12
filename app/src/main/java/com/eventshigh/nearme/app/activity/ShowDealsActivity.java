package com.eventshigh.nearme.app.activity;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.DealsObject;
import com.eventshigh.nearme.app.network.GetHotDealsRequest;
import com.eventshigh.nearme.app.ui.adapter.DataType;
import com.eventshigh.nearme.app.ui.adapter.EventsAdapter;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

/**
 * Created by umesh on 12/12/17.
 */

public class ShowDealsActivity extends BaseContextActivity {


    View topProgressBar;
    AutofitRecyclerView offersList;
    TextView noDealsView;

    View retryView;

    EventsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deals_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Hot Deals");
        topProgressBar = findViewById(R.id.top_progress_bar);
        offersList = (AutofitRecyclerView) findViewById(R.id.event_grid);
        offersList.addItemDecoration(new SpaceItemDecorator((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()),
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics())));
        retryView = findViewById(R.id.view_retry);
        noDealsView = (TextView) findViewById(R.id.no_tickets_view);
        adapter = new EventsAdapter(this);
        offersList.setAdapter(adapter);
        makeDealsServerRequest();
    }

    public void makeDealsServerRequest() {
        topProgressBar.setVisibility(View.VISIBLE);
        Account account = new Account(this);
        if (account.getLastCity() != null) {
            GetHotDealsRequest.submit(this, account.getLastCity().name(), Request.Priority.HIGH, null, true, new Response.Listener<DealsObject>() {
                @Override
                public void onResponse(DealsObject dealsObject, boolean b) {
                    if (dealsObject != null) {

                        offersList.setVisibility(View.VISIBLE);
                        noDealsView.setVisibility(View.GONE);
                        retryView.setVisibility(View.GONE);
                        if (dealsObject.getHelloBarDeals() != null && dealsObject.getHelloBarDeals().size() > 0)
                            adapter.setHelloBarDeals(dealsObject.getHelloBarDeals());

                        if (dealsObject.getHotDeals() != null && dealsObject.getHotDeals().size() > 0)
                            adapter.setHotDealsData(dealsObject.getHotDeals());

                    } else {
                        offersList.setVisibility(View.GONE);
                        noDealsView.setVisibility(View.VISIBLE);
                        noDealsView.setText("No deals available");
                    }
                    topProgressBar.setVisibility(View.GONE);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    retryView.setVisibility(View.VISIBLE);
                    offersList.setVisibility(View.GONE);
                    noDealsView.setVisibility(View.GONE);
                    topProgressBar.setVisibility(View.GONE);
                }
            });
        } else {

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void onRetry(View view) {
        makeDealsServerRequest();
    }

    public class SpaceItemDecorator extends RecyclerView.ItemDecoration {
        int space;
        int smallSpace;

        public SpaceItemDecorator(int space, int smallSpace) {
            this.space = space;
            this.smallSpace = smallSpace;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                                   RecyclerView.State state) {

            if (parent.getAdapter().getItemViewType(parent.getChildAdapterPosition(view)) == DataType.HELLOBAR_DEAL_CARD.typeId) {
                outRect.top = space;
                outRect.bottom = 0;
                outRect.left = space;
                outRect.right = space;
            } else if (parent.getAdapter().getItemViewType(parent.getChildAdapterPosition(view)) == DataType.HOT_DEALS_CARD.typeId) {
                outRect.top = space;
                outRect.left = smallSpace;
                outRect.right = smallSpace;
                if (parent.getChildAdapterPosition(view) == parent.getAdapter().getItemCount() - 1) {
                    outRect.bottom = space;
                }
            }

        }


    }
}
