package com.eventshigh.nearme.app.activity;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.PluginState;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.Utils;

import java.io.File;

public class CustomUrlActivity extends BaseActivity {
    public static final String BLOG_HOST = "blog.eventshigh.com";
    public static final String OFFER_URL_PREFIX = "http://www.eventshigh.com/get_event_contest/";
    public static final String EXTRA_TITLE_KEY =  CustomUrlActivity.class.getName() + ".title";

    private WebView webView;

    public static void launchCustomUrl(Context context, Uri webUri, @Nullable String title) {
        Intent intent = new Intent(context,
            webUri.getHost().equalsIgnoreCase(BLOG_HOST) ? BlogEntryActivity.class : CustomUrlActivity.class);
        intent.setData(webUri);
        if (title != null) {
            intent.putExtra(CustomUrlActivity.EXTRA_TITLE_KEY, title);
        }
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_url);
        webView = (WebView) findViewById(R.id.web_view);
        setupWebView(webView, this);

        // Set title.
        String title = getIntent().getStringExtra(EXTRA_TITLE_KEY);

        if (Utils.checkIfUnknown(getIntent().getDataString()) == null) {
            // nothing to do
            finish();
            return;
        }

        // Process the Offer request.
        if (getIntent().getDataString().startsWith(OFFER_URL_PREFIX)) {
            getIntent().setData(getIntent().getData().buildUpon()
                .appendQueryParameter("reflink", new Account(this).getAppDownloadLink()).build());
            if (title == null) {
                title = getString(R.string.pref_title_offers);
            }
        }

        if (title != null) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
            }
        }

        // If its notification action, report it accordingly.
        String action = getIntent().getAction();
        if (action != null && action.startsWith(NOTIFICATION_ACTION)) {
            reportActionToAnalytics("openNotification");
            if (!getIntent().getDataString().startsWith(OFFER_URL_PREFIX)) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, getIntent().getData()));
                    finish();
                    return;
                } catch (Exception e) {
                    // do nothing
                    Crashlytics.getInstance().core.logException(e);
                }
            }
        }

        // Load the url in the web view.
        webView.loadUrl(getIntent().getDataString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_custom_url, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_open_in_browser) {
            reportActionToAnalytics("openInBrowser");
            Intent intent = new Intent(Intent.ACTION_VIEW, getIntent().getData());
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // No application to open url. ignore.
                Crashlytics.getInstance().core.logException(e);
            }
            return  true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View getViewForSnackbar() {
        return webView;
    }

    @Override
    public void onBackPressed() {
        // Handle web view back button before exiting the activity
        if(webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @SuppressLint("SetJavaScriptEnabled")
    public static void setupWebView(WebView webView, BaseActivity activity) {
        // Enable Javascript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);

        // Enable Caching.
        File dir = activity.getCacheDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        webSettings.setAppCachePath(dir.getPath());
        webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
        webSettings.setAppCacheEnabled(true);
        webSettings.setAllowFileAccess(true);

        // Enable rich content.
        webSettings.setAllowContentAccess(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);

        // Setup a new web view client so we can listen in on events and also customize
        // web view behavior.
        webView.setWebViewClient(new EHWebViewClient(activity, activity.findViewById(R.id.top_progress_bar)));
        webView.setWebChromeClient(new WebChromeClient());
        webSettings.setPluginState(PluginState.ON);
    }

    public static class EHWebViewClient extends WebViewClient {
        private BaseActivity activity;
        private View progressBar;

        public EHWebViewClient(BaseActivity activity, View progressBar) {
            this.activity = activity;
            this.progressBar = progressBar;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            activity.reportActionToAnalytics("startLoading");
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            activity.reportActionToAnalytics("finishLoading");
            progressBar.setVisibility(View.GONE);
            }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Returning true here means that when opening new links from this page will open
            // the default app that can handle the link.
            if (url.contains("www.eventshigh.com")) {
                activity.reportActionToAnalytics("openEhLink", url);
                Intent intent = new Intent(activity, LaunchActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                activity.startActivity(intent);
                return true;
            } else if (url.contains("twitter.com") || url.contains("facebook.com")) {
                activity.reportActionToAnalytics("openLink", url);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                activity.startActivity(intent);
                return true;
            }
            return false;
        }
    }
}
