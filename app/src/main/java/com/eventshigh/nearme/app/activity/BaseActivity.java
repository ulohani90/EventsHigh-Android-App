package com.eventshigh.nearme.app.activity;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
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
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import com.amplitude.api.Amplitude;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.BuildConfig;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.GAHelper;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import io.fabric.sdk.android.Fabric;

/**
 * Base activity class which does the common things like
 * initialization of Google Analytics.
 */
public abstract class BaseActivity extends FragmentActivity {
    private static final String LOG_TAG = BaseActivity.class.getSimpleName();

    // Google Analytics
    protected GoogleAnalytics googleAnalytics;
    protected Tracker tracker;
    protected Preferences pref;
    protected GcmRegistration gcmRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        // Animation.
        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_translate);

        // Twitter Digits and CrashAnalytics
        // TwitterAuthConfig authConfig = TwitterUtils.getAuthConfig();
        // Fabric.with(this, new Twitter(authConfig), new Crashlytics());
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }

        // Setup Google Analytics.
        if (googleAnalytics == null) {
            Pair<GoogleAnalytics, Tracker> trackerInfo =
                    GAHelper.getTracker(this);

            googleAnalytics = trackerInfo.first;
            tracker = trackerInfo.second;
        }

        if (!BuildConfig.DEBUG) {
            // Setup Flurry.
            FlurryAgent.setVersionName(BuildConfig.VERSION_NAME);
            FlurryAgent.init(this, "2MD4D4TP7WQZH6Q6257T");
            FlurryAgent.setLogEnabled(false);
            FlurryAgent.setReportLocation(false);

            // Setup Amplitude
            Amplitude.initialize(this, "41ed6c5c945d7f1c2f2d829b90288562");
        }
    }

    protected void onStart() {
        super.onStart();

        // Google Analytics reporting.
        googleAnalytics.reportActivityStart(this);

        // Flurry, Amplitude reporting
        if (!BuildConfig.DEBUG) {
            FlurryAgent.onStartSession(this);
            FlurryAgent.logEvent(getClass().getSimpleName());

            Amplitude.startSession();
            Amplitude.logEvent(getClass().getSimpleName());
        }

        // Register with GCM if needed. GCM is used for notifications messages.
        gcmRegistration = new GcmRegistration(this);
        gcmRegistration.updateGcmRegistrationIdIfNeeded();

        // Check if this is first activity by user, if yes report special event.
        // See if this is first action by user. If yes, report it.
        pref = new Preferences(this);
        int numActivities = pref.reportActivityStart();
        if (numActivities == 1) {
            reportActionToAnalytics("firstActivity");
        }

        if (numActivities == 10) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(R.drawable.ic_launcher);
            builder.setTitle(R.string.action_share_app);
            builder.setMessage(R.string.message_share_app);

            // Set up the buttons
            builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, String.format(
                            getResources().getString(R.string.share_app_text),
                            new Account(BaseActivity.this).getUserReferrerCode())
                    );
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                }
            });
            builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();
        }
    }

    protected void onStop() {
        super.onStop();

        // Google Analytics reporting.
        googleAnalytics.reportActivityStop(this);

        // Flurry, Amplitude reporting
        if (!BuildConfig.DEBUG) {
            FlurryAgent.onEndSession(this);
            Amplitude.endSession();
        }

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
    public void reportActionToAnalytics(String actionName) {
        reportActionToAnalytics(actionName, "");
    }

    public void reportActionToAnalytics(String actionName, String label) {
        reportActionToAnalytics(actionName, label, 1);
    }

    public void reportActionToAnalytics(String actionName, String label, long value) {
        if (tracker != null) {
            tracker.send(new HitBuilders.EventBuilder()
                    .setCategory(getClass().getSimpleName())
                    .setAction(actionName)
                    .setLabel(label)
                    .setValue(value)
                    .build());

            if (!BuildConfig.DEBUG) {
                FlurryAgent.logEvent(actionName);
                Amplitude.logEvent(actionName);
            }
        }
    }

    /**
     * Open events details page.
     * @param event event for which to show details page.
     */
    public void showEventDetails(Event event) {
        reportActionToAnalytics("showEventDetails");
        Intent detailIntent = new Intent(this, EventDetailActivity.class);
        detailIntent.putExtra(EventDetailFragment.ARG_EVENT_INFO, event);
        startActivity(detailIntent);
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

        String time = DateTimeUtils.getEventTime(event, false);
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
