package com.eventshigh.nearme.app.activity;

import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.BlogEntry;
import com.eventshigh.nearme.app.network.BlogFeedRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.adapter.BlogEntriesAdapter;
import com.eventshigh.nearme.app.view.AutofitRecyclerView;

import java.util.List;

public class BlogFeedActivity extends BaseActivity {
    private View topProgressBar;
    private View retryView;

    private BlogEntriesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_contacts);

        // Setup the adapter to show data.
        adapter = new BlogEntriesAdapter(this);
        AutofitRecyclerView gridView = (AutofitRecyclerView) findViewById(R.id.grid);
        gridView.setHorizontalSpacing(1);
        gridView.setVerticalSpacing(1);
        gridView.setAdapter(adapter);

        // Setup the refresh on swipe down.
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                reportActionToAnalytics("swipeRefresh");
                swipeRefreshLayout.setRefreshing(false);
                fetchNewListing(true /* bypass cache*/);
            }
        });
        swipeRefreshLayout.setColorSchemeResources(R.color.primary);

        // More views.
        topProgressBar = findViewById(R.id.top_progress_bar);
        retryView = findViewById(R.id.view_retry);

        // load data.
        fetchNewListing(false);
    }

    @Override
    public View getViewForSnackbar() {
        return topProgressBar;
    }

    public void onRetry(View view) {
        fetchNewListing(false);
    }

    private void fetchNewListing(boolean shouldBypassCache) {
        topProgressBar.setVisibility(View.VISIBLE);
        retryView.setVisibility(View.GONE);

        // Stop all requests associated with this fragment and then submit new request.
        VolleyHelper.getRequestQueue(this).cancelAll(this);
        BlogFeedRequest.submit(this, Priority.IMMEDIATE, shouldBypassCache,
                mBlogEntriesListener, mErrorListener);
    }

    private Listener<List<BlogEntry>> mBlogEntriesListener = new Listener<List<BlogEntry>>() {
        @Override
        public void onResponse(List<BlogEntry> blogEntries, boolean isIntermediate) {
            if (!isIntermediate) {
                topProgressBar.setVisibility(View.GONE);

                if (blogEntries.isEmpty()) {
                    retryView.setVisibility(View.VISIBLE);
                }
            }

            if (!isIntermediate || !blogEntries.isEmpty()) {
                adapter.setBlogEntries(blogEntries);
            }
        }
    };

    private ErrorListener mErrorListener = new ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            topProgressBar.setVisibility(View.GONE);
            if (adapter.getItemCount() > 0) {
                showMessage(R.string.failed_refresh);
            } else {
                retryView.setVisibility(View.VISIBLE);
            }

            VolleyHelper.log(BlogFeedActivity.this, volleyError);
        }
    };
}
