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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.eventshigh.nearme.app.R;

public class BookTicketActivity extends BaseActivity {
    public static final String EVENT_BOOKING_URL_INTENT_KEY = "EVENT_BOOKING_URL_INTENT_KEY";

    private String url;
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

        // Setup a new web view client so we can listen in on events and also customize web view
        // behavior.
        webView.setWebViewClient(new BookTicketWebViewClient());

        // Load the url in the web view
        url = getIntent().getExtras().getString(EVENT_BOOKING_URL_INTENT_KEY);
        webView.loadUrl(url);
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
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
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

    private class BookTicketWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
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
            return false;
        }
    }
}
