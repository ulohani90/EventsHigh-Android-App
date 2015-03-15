package com.eventshigh.nearme.app.activity;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;

public class CustomUrlActivity extends BaseActivity {
    public static final String ADD_EVENT_URL = "http://www.eventshigh.com/add_event";
    public static final String OFFER_URL_PREFIX = "http://www.eventshigh.com/get_event_contest/";
    public static final String EXTRA_TITLE_KEY =  CustomUrlActivity.class.getName() + ".title";

    private WebView webView;
    private View progressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_ticket);
        webView = (WebView) findViewById(R.id.web_view);
        progressBar = findViewById(R.id.top_progress_bar);

        // Enable Javascript
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);

        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setBlockNetworkLoads(false);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);

        // Setup a new web view client so we can listen in on events and also customize web view
        // behavior.
        webView.setWebViewClient(new EHWebViewClient());
        webView.setWebChromeClient(new EHWebChromeClient());

        // Set title.
        String title = getIntent().getStringExtra(EXTRA_TITLE_KEY);

        // Process Add Event request.
        if (getIntent().getDataString().equals(ADD_EVENT_URL)) {
            reportActionToAnalytics("addEvent");
            title = getString(R.string.pref_title_add_event);
        }

        // Process the Offer request.
        if (getIntent().getDataString().startsWith(OFFER_URL_PREFIX)) {
            Preferences.getInstance(this).deleteOfferURI();
            getIntent().setData(getIntent().getData().buildUpon()
                .appendQueryParameter("reflink", new Account(this).getAppDownloadLink()).build());
            if (title == null) {
                title = "EventsHigh Offer";
            }
        }

        if (title != null) {
            getSupportActionBar().setTitle(title);
        }

        // If its notification action, report it accordingly.
        String action = getIntent().getAction();
        if (action != null && action.equals(NOTIFICATION_ACTION)) {
            reportActionToAnalytics("openNotification");
        }

        // Load the url in the web view.
        webView.loadUrl(getIntent().getDataString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_book, menu);
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
            }
            return  true;
        }

        return super.onOptionsItemSelected(item);
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

    private class EHWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            reportActionToAnalytics("startLoading");
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            reportActionToAnalytics("finishLoading");
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // Returning true here means that when opening new links from this page will open
            // the default app that can handle the link. For example clicking on a map link will
            // open the app chooser dialog with android maps app, chrome plus any other app that
            // the user has installed and that can handle the maps link. Returning true here also
            // means that if during the booking process if there is a follow up link then that will
            // be opened in the browser and no longer inside the app. Hence returning false always
            // so that all links will open in the web view itself. May have bad side effects in
            // some cases. ideally we should selectively open links in the web view vs launch an
            // app chooser using some code like
            //   if (url.startsWith("something")) {
            //     Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            //     view.getContext().startActivity(intent);
            //     return true;
            //   }
            if (url.contains("eventshigh.com") || url.contains("twitter.com")) {
                reportActionToAnalytics("openLink", url);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                view.getContext().startActivity(intent);
                return true;
            }
            return false;
        }
    }

    private class EHWebChromeClient extends WebChromeClient {

    }
}
