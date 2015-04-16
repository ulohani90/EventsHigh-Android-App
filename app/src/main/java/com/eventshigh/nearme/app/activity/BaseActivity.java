package com.eventshigh.nearme.app.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
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
import com.eventshigh.nearme.app.data.UserActionDbHelper;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.GAHelper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

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
    protected boolean isPlayServicesPresent;
    private GAHelper gaHelper;

    // Check out the share event timings.
    protected long shareEventInitiatedTimestamp = 0;
    protected long shareAppInitiatedTimestamp = 0;


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
        if (!BuildConfig.DEBUG) {
            Fabric.with(this, new Crashlytics());
        }

        // Setup Google Analytics.
        isPlayServicesPresent = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
        if (isPlayServicesPresent) {
            gaHelper = GAHelper.getInstance(this);
            if (BuildConfig.DEBUG) {
                gaHelper.setAppOptOut(true);
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

        // Report app to Facebook
        com.facebook.AppEventsLogger.activateApp(this, "196111897251952");

        // Find out share action result.
        if (shareAppInitiatedTimestamp > 0) {
            long secForShare = (System.currentTimeMillis() - shareAppInitiatedTimestamp) / 1000;
            reportActionToAnalytics(secForShare > 5 ? "shareApp" : "appShareDismissed", Long.toString(secForShare));
        } else if (shareEventInitiatedTimestamp > 0) {
            long secForShare = (System.currentTimeMillis() - shareEventInitiatedTimestamp) / 1000;
            reportActionToAnalytics(secForShare > 5 ? "shareEvent" : "eventShareDismissed", Long.toString(secForShare));
        }
        shareAppInitiatedTimestamp = 0;
        shareEventInitiatedTimestamp = 0;
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
     * Helper method to start activity which lets user share the app.
     */
    public void shareApp(@Nullable String message) {
        reportActionToAnalytics("appShareInitiated");
        shareAppInitiatedTimestamp = System.currentTimeMillis();

        if (message == null) {
            message = getString(R.string.share_app_text);
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT,
                String.format(message, new Account(this).getAppDownloadLink()));
        intent.setType("text/plain");
        startActivity(intent);
    }

    /**
     * Helper method to share an Event.
     */
    public void shareEvent(Event event) {
        reportEventAction(event, "eventShareInitiated");
        shareEventInitiatedTimestamp = System.currentTimeMillis();

        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    String.format(getResources().getString(R.string.share_event_text),
                            event.title + (event.isCleanVenue ? " @ " + event.venue : ""),
                            event.getEventShareURI(this))
            );
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.failed_share, Toast.LENGTH_SHORT).show();
            Log.w(LOG_TAG, "failed sharing", e);
        }
    }

    public void showEventDetails(Event event) {
        UserActionDbHelper.getInstance(this).recordAction(
                UserActionDbHelper.EventAction.OPEN_EVENT_DETAIL, event.id);
        showEventDetails(event.getEventDetailsURI());
    }

    public void showEventDetails(Uri eventDetailsURI) {
        Intent detailIntent = new Intent(this, EventDetailActivity.class);
        detailIntent.setData(eventDetailsURI);
        startActivity(detailIntent);
    }

    public void addToCalendar(Event event, @Nullable Date date) {
        reportEventAction(event, "addToCalendar", GAHelper.getDateReportString(date));

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(Events.CONTENT_URI)
                .putExtra(Events.TITLE, event.title)
                .putExtra(Events.EVENT_LOCATION, event.getFullAddress())
                .putExtra(Events.DESCRIPTION,
                        event.getEventShareURI(this) + "\n\n" + event.description);

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

    protected void showVerifyPhoneSnackbar() {
        boolean isVerificationPending = Account.isPhoneVerifyPending(this);
        final View view = findViewById(R.id.verify_phone_container);
        view.setVisibility(isVerificationPending ? View.VISIBLE :View.GONE);
        view.findViewById(R.id.verify_phone).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(BaseActivity.this, PhoneLoginActivity.class));
                view.setVisibility(View.GONE);
            }
        });
        view.findViewById(R.id.verify_phone_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Account.disablePhoneVerifySnackbar();
                view.setVisibility(View.GONE);
            }
        });
    }
}
