package com.eventshigh.nearme.app.activity;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.network.URLShortenerRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.UserActionHelper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.GAHelper;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.Date;
import java.util.TimeZone;

import io.fabric.sdk.android.Fabric;

/**
 * Base activity class which does the common things like initialization of Google Analytics.
 * This class also provides some useful functions to be used across other activities.
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final String LOG_TAG = BaseActivity.class.getSimpleName();

    // This constant defines the app specific intent action for notification.
    public static final String NOTIFICATION_ACTION = "com.eventshigh.nearme.app.notification";

    // Google Analytics
    protected boolean isPlayServicesPresent;
    private GAHelper gaHelper;

    // Check out the share event timings.
    protected long shareEventInitiatedTimestamp = 0;

    public static Typeface fontQuicksand;
    public static Typeface fontQuicksandBold;
    public static final Object lock = new Object();
    public static boolean loadedFonts = false;

    // **********************************************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // **********************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Animation.
        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_translate);

        // Twitter CrashAnalytics
        Fabric.with(this, new Crashlytics());

        // Report app to Facebook
        FacebookSdk.sdkInitialize(getApplicationContext());

        // Setup Google Analytics.
        isPlayServicesPresent = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
        if (isPlayServicesPresent) {
            gaHelper = GAHelper.getInstance(this);
        }

        synchronized (lock) {
            if (!loadedFonts) {
                fontQuicksand = Typeface.createFromAsset(getAssets(), "Quicksand-Regular.ttf");
                fontQuicksandBold = Typeface.createFromAsset(getAssets(), "Quicksand-Bold.ttf");
                loadedFonts = true;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Google Analytics reporting.
        if (isPlayServicesPresent) {
            gaHelper.reportActivityStart(this);
        }
    }

    @Override
    protected void onStop() {
        // Stop all requests associated with this activity.
        VolleyHelper.getRequestQueue(this).cancelAll(this);

        // Google Analytics reporting.
        if (isPlayServicesPresent) {
            gaHelper.reportActivityStop(this);
        }

        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.collapseActionView();
        }

        // FB.
        AppEventsLogger.activateApp(this);

        // Find out share action result.
        if (shareEventInitiatedTimestamp > 0) {
            long secForShare = (System.currentTimeMillis() - shareEventInitiatedTimestamp) / 1000;
            reportActionToAnalytics(secForShare > 5 ? "shareEvent" : "eventShareDismissed", Long.toString(secForShare));
        }
        shareEventInitiatedTimestamp = 0;
    }

    @Override
    protected void onPause() {
        super.onPause();

        // FB.
        AppEventsLogger.deactivateApp(this);
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
        if (isPlayServicesPresent && gaHelper != null) {
            gaHelper.reportActionToAnalytics(getClass().getSimpleName(),
                    actionName, label, value, customValues);
        }
    }

    /**
     * Helper method to share an Event.
     */
    public void shareEvent(final Event event, @Nullable final String packageName) {
        String src = null;
        if (packageName != null) {
            src = packageName.split("\\.")[1];
        }
        final String eventShareUri = event.getEventShareURI(this, src).toString();

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("One sec ...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        URLShortenerRequest.submit(this, eventShareUri,
            new Listener<String>() {
                @Override
                public void onResponse(String shortenUri, boolean isIntermediate) {
                    dialog.dismiss();
                    shareEvent(event, shortenUri, packageName);
                }
            },
            new ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    dialog.dismiss();
                    shareEvent(event, eventShareUri, packageName);
                }
            }
        );
    }

    public void shareEvent(Event event, String eventUri, @Nullable String packageName) {
        reportEventAction(event, "eventShareInitiated", packageName);
        shareEventInitiatedTimestamp = System.currentTimeMillis();
        new UserActionHelper(this).recordShareAction(event.id, packageName, null);

        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);

            if (packageName != null && packageName.equals(EventDetailActivity.PACKAGE_NAME_TWITTER)) {
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        event.title + (event.isCleanVenue ? " @ " + event.venue : "") +
                                "\n@eventshighapp\n" + eventUri);
            } else {
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                    String.format(getResources().getString(R.string.share_event_text),
                        event.title + (event.isCleanVenue ? " @ " + event.venue : ""), eventUri)

                );
            }
            sendIntent.setType("text/plain");
            if (packageName != null) {
                sendIntent.setPackage(packageName);
            }
            startActivity(sendIntent);
        } catch (ActivityNotFoundException e) {
            Crashlytics.getInstance().core.logException(e);
            Snackbar.make(this.getViewForSnackbar(), R.string.failed_share, Snackbar.LENGTH_SHORT).show();
            Log.w(LOG_TAG, "failed sharing", e);
        }
    }

    public void addToCalendar(Event event, @Nullable Date date) {
        reportEventAction(event, "addToCalendar", GAHelper.getDateReportString(date));

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(Events.CONTENT_URI)
                .putExtra(Events.TITLE, event.title)
                .putExtra(Events.EVENT_LOCATION, event.getFullAddress())
                .putExtra(Events.DESCRIPTION, event.getEventShareURI(this))
                .putExtra(Events.EVENT_LOCATION, event.getShortAddress());

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
            Crashlytics.getInstance().core.logException(e);
            Snackbar.make(this.getViewForSnackbar(), R.string.no_cal_app, Snackbar.LENGTH_SHORT).show();
        }
    }

    public boolean isFavourite(Event event) {
        return EventsMarkerManager.getInstance(this).isFavourite(event.id);
    }

    public void reportEventAction(Event event, String actionName) {
        reportEventAction(event, actionName, null);
    }

    public void reportEventAction(Event event, String actionName, @Nullable String label) {
        if (event != null) {
            reportActionToAnalytics(actionName,
                    label == null ? "" : label,
                    1,
                    isFavourite(event) ? "Favourite" : "No-Favourite",
                    event.ehRecommended ? "Recommended" : "Non-Recommended");
        }
    }

    public void reportCampaignParams(String campaignData) {
        if (isPlayServicesPresent) {
            gaHelper.reportCampaignParams(campaignData);
        }
    }

    public abstract View getViewForSnackbar();
}
