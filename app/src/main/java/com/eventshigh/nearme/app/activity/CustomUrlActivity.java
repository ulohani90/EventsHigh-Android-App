package com.eventshigh.nearme.app.activity;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.UrlQuerySanitizer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.util.Log;
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
import com.eventshigh.nearme.app.utils.Utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class CustomUrlActivity extends BaseActivity {
    public static final String BLOG_HOST = "blog.eventshigh.com";
    public static final String EXTRA_TITLE_KEY =  CustomUrlActivity.class.getName() + ".title";

    private WebView webView;
    private static boolean isPayment;


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
        isPayment = getIntent().getBooleanExtra(TicketReviewActivity.IS_PAYMENT,false);
        setupWebView(webView, this, true);

        // Set title.
        String title = getIntent().getStringExtra(EXTRA_TITLE_KEY);

        if (Utils.checkIfUnknown(getIntent().getDataString()) == null) {
            // nothing to do
            finish();
            return;
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
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, getIntent().getData()));
                finish();
                return;
            } catch (Exception e) {
                // do nothing
                Crashlytics.getInstance().core.logException(e);
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
    public static void setupWebView(WebView webView, BaseActivity activity, boolean useProgressBar) {
        // Enable Javascript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);
        WebView.setWebContentsDebuggingEnabled(true);

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
        webView.setWebViewClient(new EHWebViewClient(activity,isPayment,
                useProgressBar ? activity.findViewById(R.id.top_progress_bar) : null));
        webView.setWebChromeClient(new WebChromeClient());
        webSettings.setPluginState(PluginState.ON);
    }

    public static class EHWebViewClient extends WebViewClient {
        private BaseActivity activity;
        @Nullable private View progressBar;
        private boolean isPayment;

        public EHWebViewClient(BaseActivity activity,boolean isPayment, @Nullable View progressBar) {
            this.activity = activity;
            this.progressBar = progressBar;
            this.isPayment = isPayment;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (progressBar != null) {
                activity.reportActionToAnalytics("startLoading");
                progressBar.setVisibility(View.VISIBLE);
            }
            if(isPayment){
                if (isPayment){
                        Log.e("custom URLs ",url);
                        if(url.toLowerCase().contains(("ticket.jsp").toLowerCase())){
                            UrlQuerySanitizer sanitizer = new UrlQuerySanitizer(url);
                            Intent intent = new Intent();
                            intent.putExtra(TicketReviewActivity.BOOKING_ID, sanitizer.getValue("booking"));
                            intent.putExtra(TicketReviewActivity.TICKETING_LINK, url);
                            intent.putExtra(TicketReviewActivity.IS_PAYMENT_SUCCESS, true);
                            activity.setResult(RESULT_OK, intent);
                            activity.finish();
                        }else if(url.toLowerCase().contains(("failed.html").toLowerCase())){
                            Intent intent = new Intent();
                            intent.putExtra(TicketReviewActivity.IS_PAYMENT_SUCCESS,false);
                            activity.setResult(RESULT_OK, intent);
                            activity.finish();
                        }
                }
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (progressBar != null) {
                activity.reportActionToAnalytics("finishLoading");
                progressBar.setVisibility(View.GONE);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Returning true here means that when opening new links from this page will open
            // the default app that can handle the link.
            if (url.contains("www.eventshigh.com")){
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
