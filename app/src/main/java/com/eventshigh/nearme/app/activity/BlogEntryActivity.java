package com.eventshigh.nearme.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import com.eventshigh.nearme.app.view.ObservableWebView;
import com.eventshigh.nearme.app.view.ObservableWebView.OnScrollChangedCallback;

public class BlogEntryActivity extends BaseActivity {
    public static final String EXTRA_BLOG_ENTRY_PARAM = BlogEntryActivity.class.getSimpleName() + "_blog_entry";

    private Toolbar toolbar;
    private View topProgressBar;
    private ObservableWebView webView;

    private BlogEntry blogEntry = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_blog_entry);
        topProgressBar = findViewById(R.id.top_progress_bar);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.loading);
        setSupportActionBar(toolbar);

        webView = (ObservableWebView) findViewById(R.id.web_view);
        CustomUrlActivity.setupWebView(webView, this, true);

        setShareVisibility(R.id.share_whatsapp, PACKAGE_NAME_WHATSAPP);
        setShareVisibility(R.id.share_fb, PACKAGE_NAME_FACEBOOK);
        setShareVisibility(R.id.share_twitter, PACKAGE_NAME_TWITTER);
        setShareVisibility(R.id.share_email, PACKAGE_NAME_EMAIL);
    }

    protected void onStart() {
        super.onStart();

        String action = getIntent().getAction();
        if (action != null && action.startsWith(BaseActivity.NOTIFICATION_ACTION)) {
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

    public void whatsapp(View view) {
        shareBlog(blogEntry, PACKAGE_NAME_WHATSAPP);
    }

    public void facebook(View view) {
        shareBlog(blogEntry, PACKAGE_NAME_FACEBOOK);
    }

    public void twitter(View view) {
        shareBlog(blogEntry, PACKAGE_NAME_TWITTER);
    }

    public void email(View view) {
        shareBlog(blogEntry, PACKAGE_NAME_EMAIL);
    }

    private void setShareVisibility(@IdRes int shareButtonId, String packageName) {
        findViewById(shareButtonId).setVisibility(isInstalled(packageName) ? View.VISIBLE : View.GONE);
    }

    private void populateBlog(BlogEntry blogEntry) {
        this.blogEntry = blogEntry;
        topProgressBar.setVisibility(View.GONE);
        toolbar.setTitle(Html.fromHtml(blogEntry.title));

        String html = String.format(BLOG_HTML_FORMAT, blogEntry.title, blogEntry.thumbnail, blogEntry.contents);
        webView.loadDataWithBaseURL(blogEntry.url, html, "text/html", "UTF-8", "");

        final int screenHeight = getResources().getDisplayMetrics().heightPixels;
        toolbar.setAlpha(0);
        webView.setOnScrollChangedCallback(new OnScrollChangedCallback() {
            @Override
            public void onScroll(int l, int t) {
                float opacity = Math.min(1.0f, t * 3f / screenHeight);
                toolbar.setAlpha(opacity);
            }
        });

        findViewById(R.id.share_row).setVisibility(View.VISIBLE);
    }

    private void shareBlog(BlogEntry blogEntry, @Nullable String packageName) {
        reportActionToAnalytics("shareBlog", packageName);

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

    private static final String BLOG_HTML_FORMAT =
            "<html>\n" +
            "<head>\n" +
            "  <link rel='stylesheet' id='foundation_pushit-css' href='https://ehblogassets.eventshigh.com/wp-content/plugins/wptouch/themes/foundation/modules/pushit/pushit.css?d62120' type='text/css' media='all'>\n" +
            "  <link rel='stylesheet' id='wptouch-parent-theme-css-css' href='https://ehblogassets.eventshigh.com/wp-content/plugins/wptouch/themes/foundation/default/style.css?d62120' type='text/css' media='all'>\n" +
            "  <style id='wptouch-parent-theme-css-inline-css' type='text/css'>.page-wrapper{background-color:#f9f9f8}a{color:#2d353f}body,header,.wptouch-menu,.pushit,#search-dropper,.date-circle{background-color:#2d353f}a,#slider a p:after{color:#35c4ff}.dots li.active,#switch .active{background-color:#35c4ff}.bauhaus,.wptouch-login-wrap,form#commentform button#submit{background-color:#21759b}</style>\n" +
            "  <link rel='stylesheet' id='wptouch-theme-css-css' href='https://ehblogassets.eventshigh.com/wp-content/plugins/wptouch/themes/bauhaus/default/style.css?d62120' type='text/css' media='all'>\n" +
            "  <meta name='viewport' content='initial-scale=1.0, maximum-scale=1.0, user-scalable=no, width=device-width'>\n" +
            "  <link rel='apple-touch-icon-precomposed' href='https://ehblogassets.eventshigh.com/wp-content/plugins/wptouch/admin/images/default-bookmark.png?d62120'>\n" +
            "  <style> .applink { display: none } </style>\n" +
            "</head>\n" +
            "<body class='single single-post single-format-standard custom-background css-videos body-font smartphone portrait android theme-bauhaus dark-header light-body dark-post-head circles no-com-bubbles off-canvas fonts-lato_roboto'>\n" +
            "  <div class='page-wrapper' style='position: relative; transition: -webkit-transform 0.33s cubic-bezier(0.29, 0.05, 0.14, 0.87);'>\n" +
            "    <div id='content'>\n" +
            "      <div class='post section post-1530 post-name-onam-in-bangalore post-author-5 single not-page has-thumbnail show-thumbs'>\n" +
            "        <div class='post-page-head-area bauhaus'>\n" +
            "          <h2 class='post-title heading-font'>%s</h2>\n" +
            "        </div>\n" +
            "        <div class='post-page-content'>\n" +
            "          <div class='post-page-thumbnail'>\n" +
            "            <img src='%s' class='post-thumbnail wp-post-image wp-post-image'>\n" +
            "          </div>\n" +
            "          %s\n" +
            "        </div>\n" +
            "      </div>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "</body>\n" +
            "</html>";
}
