package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Offer;
import com.eventshigh.nearme.app.network.OffersRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.HideActionBarOnScroll;
import com.eventshigh.nearme.app.ui.OffersAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;


/**
 * Fragment which is used to show the offers tab.
 */
public class OffersFragment extends Fragment {
    private BaseContextActivity activity;

    private AutofitRecyclerView offersGridView;
    private View topProgressBar;
    private OffersAdapter offersAdapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseContextActivity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        VolleyHelper.getRequestQueue(activity).cancelAll(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_offers, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        offersGridView = (AutofitRecyclerView) view.findViewById(R.id.offers);
        offersGridView.setOnScrollListener(new HideActionBarOnScroll(activity));
        topProgressBar = view.findViewById(R.id.top_progress_bar);
        OffersRequest.submit(activity, Priority.IMMEDIATE, this, false, mOffersCallback,
                mErrorListener);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (offersAdapter != null) {
            offersAdapter.notifyDataSetChanged();
        }
    }

    private Listener<List<Offer>> mOffersCallback = new Listener<List<Offer>>() {
        @Override
        public void onResponse(List<Offer> offers, boolean isIntermediate) {
            topProgressBar.setVisibility(View.GONE);
            offersAdapter = new OffersAdapter(activity, offers);
            offersGridView.setAdapter(offersAdapter);
        }
    };

    private ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            // do nothing.
            topProgressBar.setVisibility(View.GONE);
        }
    };
}
