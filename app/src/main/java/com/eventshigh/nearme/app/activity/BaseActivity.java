package com.eventshigh.nearme.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

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

            // Disable GA reporting in debug build.
            if (BuildConfig.DEBUG) {
                googleAnalytics.setAppOptOut(true);
            }
        }

        // Automatic Google Analytics reporting.
        googleAnalytics.reportActivityStart(this);

        // Setup Http cache.
        if (HttpResponseCache.getInstalled() == null) {
            try {
                File httpCacheDir = new File(getCacheDir(), "http");
                long httpCacheSize = 20 * 1024 * 1024; // 20 MB
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

    public void showDirections(Event event) {
        reportActionToAnalytics("showDirections");

        String query = event.address != null ? event.address :
                event.location.latitude + "," + event.location.longitude +  " (" + event.title + ")";
        Uri locationUri = Uri.parse("geo:0,0?q=" + query);
        Intent intent = new Intent(Intent.ACTION_VIEW, locationUri);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open maps.
            Toast.makeText(this, R.string.no_map_app, Toast.LENGTH_SHORT).show();
        }
    }

    public void addToCalendar(Event event, @Nullable Date date) {
        reportActionToAnalytics("addToCalendar");

        String venue = event.address == null ? event.venue : event.address;
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(Events.CONTENT_URI)
                .putExtra(Events.TITLE, event.title)
                .putExtra(Events.EVENT_LOCATION, venue)
                .putExtra(Events.DESCRIPTION,
                        event.getEventDetailsURI().toString() + "\n\n" + event.description);

        long eventTime = date != null ? date.getTime() :
                (event.eventTimings.length > 0 ? event.eventTimings[0] : 0);
        if (eventTime > 0) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventTime);
        }

        String time = Utils.getEventTime(event);
        if (time == null) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open cal.
            Toast.makeText(this, R.string.no_cal_app, Toast.LENGTH_SHORT).show();
        }
    }

}
