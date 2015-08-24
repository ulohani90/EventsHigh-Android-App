package com.eventshigh.nearme.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.BlogEntry;
import com.eventshigh.nearme.app.network.BlogEntryRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;

public class BlogEntryActivity extends BaseActivity {
    public static final String EXTRA_BLOG_ENTRY_PARAM = BlogEntryActivity.class.getSimpleName() + "_blog_entry";

    private View topProgressBar;
    private BlogEntry blogEntry = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_custom_url);
        topProgressBar = findViewById(R.id.top_progress_bar);
        setTitle(R.string.loading);
    }

    protected void onStart() {
        super.onStart();

        String action = getIntent().getAction();
        if (BaseActivity.NOTIFICATION_ACTION.equals(action)) {
            reportActionToAnalytics("openNotification", getIntent().getData().getLastPathSegment());
        }

        // Get the event from Intent.
        if (getIntent().hasExtra(EXTRA_BLOG_ENTRY_PARAM)) {
            BlogEntry blogEntry = getIntent().getParcelableExtra(EXTRA_BLOG_ENTRY_PARAM);
            populateBlog(blogEntry);
        } else {
            BlogEntryRequest.submit(this, getIntent().getDataString(), Priority.IMMEDIATE, false,
                new Listener<BlogEntry>() {
                    @Override
                    public void onResponse(BlogEntry blogEntry, boolean isIntermediate) {
                        populateBlog(blogEntry);
                    }
                },
                new ErrorListener() {
                   @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Toast.makeText(BlogEntryActivity.this, R.string.failed_load,
                                Toast.LENGTH_SHORT).show();
                        VolleyHelper.log(BlogEntryActivity.this, volleyError);
                        finish();
                    }
                }
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_event_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            if (blogEntry != null) {
                shareBlog(blogEntry, null);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View getViewForSnackbar() {
        return topProgressBar;
    }

    private void populateBlog(BlogEntry blogEntry) {
        this.blogEntry = blogEntry;
        topProgressBar.setVisibility(View.GONE);
        setTitle(blogEntry.title);

        WebView webview = (WebView) findViewById(R.id.web_view);
        webview.loadDataWithBaseURL(blogEntry.url, blogEntry.contents, "text/html", "UTF-8", "");
    }

    private void shareBlog(BlogEntry blogEntry, @Nullable String packageName) {
        reportActionToAnalytics("shareBlog", blogEntry.url);

        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                String.format(getResources().getString(R.string.share_blog_text), blogEntry.title,
                    blogEntry.url)
            );

            sendIntent.setType("text/plain");
            if (packageName != null) {
                sendIntent.setPackage(packageName);
            }
            startActivity(sendIntent);
        } catch (ActivityNotFoundException e) {
            Crashlytics.getInstance().core.logException(e);
            showMessage(R.string.failed_share);
        }
    }
}
