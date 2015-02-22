package com.eventshigh.nearme.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.GAHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import io.fabric.sdk.android.Fabric;

/**
 * Base activity class which does the common things like initialization of Google Analytics.
 * This class also provides some useful functions to be used across other activities.
 */
public abstract class BaseActivity extends ActionBarActivity {
    private static final String LOG_TAG = BaseActivity.class.getSimpleName();

    // This constant defines the app specific intent action for notification.
    public static final String NOTIFICATION_ACTION = "com.eventshigh.nearme.app.notification";

    // Google Analytics
    protected GAHelper gaHelper;


    // ***********************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // ***********************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Animation.
        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_translate);

        // Twitter CrashAnalytics
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }

        // Setup Google Analytics.
        gaHelper = GAHelper.getInstance(getApplicationContext());
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Google Analytics reporting.
        gaHelper.reportActivityStart(this);

        // Report app to Facebook
        com.facebook.AppEventsLogger.activateApp(this, "196111897251952");
    }

    @Override
    protected void onStop() {
        // Stop all requests associated with this activity.
        VolleyHelper.getRequestQueue(getApplicationContext()).cancelAll(this);

        // Google Analytics reporting.
        gaHelper.reportActivityStop(this);

        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportActionBar().collapseActionView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void navigateUp() {
        finish();
    }

    /**
     * Helper method which can be used to report any action in analytics.
     * @param actionName name of action to be reported.
     */
    public void reportActionToAnalytics(String actionName) {
        reportActionToAnalytics(actionName, "");
    }

    public void reportActionToAnalytics(String actionName, String label) {
        reportActionToAnalytics(actionName, label, 1);
    }

    public void reportActionToAnalytics(String actionName, String label, long value,
                                        String... customValues) {
        if (gaHelper != null) {
            gaHelper.reportActionToAnalytics(getClass().getSimpleName(), actionName, label, value, customValues);
        }
    }

    /**
     * Helper method to start activity which lets user share the app.
     */
    public void shareApp() {
        reportActionToAnalytics("shareApp");
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                String.format(
                        getResources().getString(R.string.share_app_text),
                        new Account(this).getUserReferrerCode())
        );
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    /**
     * Helper method to share an Event. This method coverts and view
     * into jpeg image which is then shared with external tool.
     */
    public void shareEvent(View eventView, Event event) {
        reportEventAction(event, "shareEvent");

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
                    String.format(getResources().getString(R.string.share_event_text),
                            event.getEventDetailsURI()));
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

    public void showEventDetails(Event event) {
        Intent detailIntent = new Intent(this, EventDetailActivity.class);
        detailIntent.putExtra(EventDetailFragment.ARG_EVENT_INFO, event);
        startActivity(detailIntent);
    }

    public void showDirections(Event event) {
        reportEventAction(event, "showDirections");

        String query = event.address;
        if (query == null) {
            if (event.location == null) {
                reportActionToAnalytics("skipDirectionsNoLocation");
                Toast.makeText(this, R.string.failed_event_location, Toast.LENGTH_SHORT).show();
                return;
            }
            query = event.location.latitude + "," + event.location.longitude +  " (" + event.title + ")";
        }

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
        reportEventAction(event, "addToCalendar", GAHelper.getDateReportString(date));

        String venue = event.address == null ? event.venue : event.address;
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(Events.CONTENT_URI)
                .putExtra(Events.TITLE, event.title)
                .putExtra(Events.EVENT_LOCATION, venue)
                .putExtra(Events.DESCRIPTION,
                        event.getEventDetailsURI().toString() + "?src=ehm \n\n" + event.description);

        if (date == null && event.eventTimings.length > 0) {
            date = new Date(event.eventTimings[0]);
        }

        if (date != null) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, date.getTime());
            if (DateTimeUtils.getTimeString(date, TimeZone.getTimeZone(event.city.timeZone)) == null) {
                intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);
            }
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open cal.
            Toast.makeText(this, R.string.no_cal_app, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isFavourite(Event event) {
        return EventsMarkerManager.getInstance(this).isFavourite(event.id);
    }

    public void reportEventAction(Event event, String actionName) {
        reportEventAction(event, actionName, null);
    }

    public void reportEventAction(Event event, String actionName, @Nullable String label) {
        reportActionToAnalytics(actionName,
                label == null ? "" : label,
                1,
                isFavourite(event) ? "Favourite" : "No-Favourite",
                event.ehRecommended ? "Recommended" : "Non-Recommended");
    }
}
