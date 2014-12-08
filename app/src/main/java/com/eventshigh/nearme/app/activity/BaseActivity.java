package com.eventshigh.nearme.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Base activity class which does the common things like
 * initialization of Google Analytics.
 */
public abstract class BaseActivity extends FragmentActivity {
    private static final String LOG_TAG = BaseActivity.class.getSimpleName();

    // Google Analytics
    protected GoogleAnalytics googleAnalytics;
    protected Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup Google Analytics.
        if (googleAnalytics == null) {
            googleAnalytics = GoogleAnalytics.getInstance(this);
            tracker = googleAnalytics.newTracker(R.xml.analytics);
            tracker.enableAdvertisingIdCollection(true);
        }

        // Automatic Google Analytics reporting.
        googleAnalytics.reportActivityStart(this);

        // Setup Http cache.
        if (HttpResponseCache.getInstalled() == null) {
            try {
                File httpCacheDir = new File(getCacheDir(), "http");
                long httpCacheSize = 10 * 1024 * 1024; // 10 MB
                HttpResponseCache.install(httpCacheDir, httpCacheSize);
            } catch (IOException e) {
                Log.w(LOG_TAG, "HTTP response cache installation failed!", e);
            }
        }
    }

    protected void onStop() {
        super.onStop();

        // Automatic Google Analytics reporting.
        googleAnalytics.reportActivityStop(this);

        // Save the Http cache.
        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
    }

    /**
     * Helper method which can be used to report any action.
     * @param actionName name of action to be reported.
     */
    protected void reportActionToAnalytics(String actionName) {
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(getClass().getSimpleName())
                .setAction(actionName)
                .setLabel("")
                .setValue(1)
                .build());
    }

    /**
     * Helper method to share an Event. This method coverts and view
     * into jpeg image which is then shared with external tool.
     */
    public void shareEvent(View eventView, Event event) {
        reportActionToAnalytics("shareEvent");

        eventView.setDrawingCacheEnabled(true);
        Bitmap bitmap = eventView.getDrawingCache();

        try {
            File file = File.createTempFile("event", ".jpg", getCacheDir());
            FileOutputStream oStream = new FileOutputStream(file);
            bitmap.compress(CompressFormat.JPEG, 90, oStream);
            oStream.close();

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(this,
                            "com.eventshigh.nearme.app.fileprovider", file));
            sendIntent.putExtra(Intent.EXTRA_TITLE, event.title);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    event.getEventDetailsURI() + " (shared via EventsHigh)");
            sendIntent.setType("image/jpeg");
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(sendIntent);
        } catch (IOException e) {
            Toast.makeText(this, R.string.failed_save, Toast.LENGTH_SHORT).show();
            Log.w(LOG_TAG, "failed to create file for sharing", e);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.failed_share, Toast.LENGTH_SHORT).show();
            Log.w(LOG_TAG, "failed sharing", e);
        }
    }
}
