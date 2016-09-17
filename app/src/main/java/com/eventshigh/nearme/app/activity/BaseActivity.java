package com.eventshigh.nearme.app.activity;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.MovieDetailObject;
import com.eventshigh.nearme.app.data.MovieInfoObject;
import com.eventshigh.nearme.app.data.MovieMarkerManager;
import com.eventshigh.nearme.app.data.ProfileInfo;
import com.eventshigh.nearme.app.data.stream.OfferObject;
import com.eventshigh.nearme.app.network.URLShortenerRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.AskForContactsDialog;
import com.eventshigh.nearme.app.ui.OneSecDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.user.UserActionHelper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.GAHelper;
import com.eventshigh.nearme.app.utils.Utils;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;

/**
 * Base activity class which does the common things like initialization of Google Analytics.
 * This class also provides some useful functions to be used across other activities.
 */
public abstract class BaseActivity extends AppCompatActivity {
    private static final String LOG_TAG = BaseActivity.class.getSimpleName();

    // This constant defines the app specific intent action for notification.
    public static final String NOTIFICATION_ACTION = "com.eventshigh.nearme.app.notification";
    public static final int PLUS_ONE_REQUEST_CODE = 111;
    public static final int PERMISSIONS_REQUEST_READ_CONTACTS = 34;
    public static final int PERMISSIONS_REQUEST_LOCATION = 42;

    public static final String PACKAGE_NAME_WHATSAPP = "com.whatsapp";
    public static final String PACKAGE_NAME_FACEBOOK = "com.facebook.katana";
    public static final String PACKAGE_NAME_TWITTER = "com.twitter.android";
    public static final String PACKAGE_NAME_EMAIL = "com.google.android.gm";

    // Google Analytics
    protected boolean isPlayServicesPresent;
    private GAHelper gaHelper;
    private boolean isRunning = false;

    // Check out the share event timings.
    protected long shareEventInitiatedTimestamp = 0;
    protected long shareEventsInitiatedTimestamp = 0;
    protected long shareOfferInitiatedTimestamp = 0;
    protected long shareMovieInitiatedTimestamp = 0;
    protected long shareTicketInitiatedTimestamp = 0;
    protected long shareProfileInitiatedTimestamp = 0;


    // **********************************************
    // Activity lifecycle  Methods
    // See http://developer.android.com/training/basics/activity-lifecycle/starting.html
    // **********************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Animation.
        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_translate);


        // Report app to Facebook
        FacebookSdk.sdkInitialize(getApplicationContext());

        // Setup Google Analytics.
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        isPlayServicesPresent = apiAvailability.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
        if (isPlayServicesPresent) {
            gaHelper = GAHelper.getInstance(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isFinishing()) {
            return;
        }

        if (isPlayServicesPresent) {
            // Google Analytics reporting.
            gaHelper.reportActivityStart(this);
        }

        UpdateAccountInfoService.run(this, false);
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
        isRunning = true;

        // FB.
        AppEventsLogger.activateApp(this);

        // Find out share action result.
        if (shareEventInitiatedTimestamp > 0) {
            long secForShare = (System.currentTimeMillis() - shareEventInitiatedTimestamp) / 1000;
            reportActionToAnalytics(secForShare > 5 ? "shareEvent" : "eventShareDismissed",
                    Long.toString(secForShare));

            Preferences preferences = Preferences.getInstance(this);
            if (secForShare > 5 && !preferences.canUploadContacts()) {
                AskForContactsDialog.show(this, preferences);

            }
        }
        if (shareEventsInitiatedTimestamp > 0) {
            long secForShare = (System.currentTimeMillis() - shareEventsInitiatedTimestamp) / 1000;
            reportActionToAnalytics(secForShare > 5 ? "shareEvents" : "eventsShareDismissed",
                    Long.toString(secForShare));
        }

        if (shareOfferInitiatedTimestamp > 0) {
            long secForShare = (System.currentTimeMillis() - shareOfferInitiatedTimestamp) / 1000;
            reportActionToAnalytics(secForShare > 5 ? "shareOffers" : "offerShareDismissed",
                    Long.toString(secForShare));
        }

        if (shareMovieInitiatedTimestamp > 0) {
            long secForShare = (System.currentTimeMillis() - shareMovieInitiatedTimestamp) / 1000;
            reportActionToAnalytics(secForShare > 5 ? "shareMovies" : "movieShareDismissed",
                    Long.toString(secForShare));
        }

        if (shareTicketInitiatedTimestamp > 0) {
            long secForShare = (System.currentTimeMillis() - shareTicketInitiatedTimestamp) / 1000;
            reportActionToAnalytics(secForShare > 5 ? "shareTickets" : "ticketShareDismissed",
                    Long.toString(secForShare));
        }
        if (shareProfileInitiatedTimestamp > 0) {
            long secForShare = (System.currentTimeMillis() - shareProfileInitiatedTimestamp) / 1000;
            reportActionToAnalytics(secForShare > 5 ? "shareProfile" : "shareProfileDismissed",
                    Long.toString(secForShare));
        }


        shareProfileInitiatedTimestamp = 0;
        shareEventInitiatedTimestamp = 0;
        shareMovieInitiatedTimestamp = 0;
        shareOfferInitiatedTimestamp = 0;
        shareTicketInitiatedTimestamp = 0;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isRunning = false;

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
            if (isRunning())
                onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AskForContactsDialog.doNeedful(this);
            } else {
                // permission denied.
                Preferences preferences = Preferences.getInstance(this);
                preferences.setCanUploadContacts(false);
            }
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public View getViewForSnackbar() {
        return null;
    }

    public void showMessage(@StringRes int messageId) {
        showMessage(getString(messageId));
    }

    public void showMessage(String message) {
        View snackBarView = getViewForSnackbar();
        if (snackBarView != null) {
            Snackbar.make(snackBarView, message, Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Helper method which can be used to report any action in analytics.
     *
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

    public void shareApp() {
        reportActionToAnalytics("shareApp", PACKAGE_NAME_WHATSAPP);

        BranchUniversalObject branchObject = new BranchUniversalObject();

        final String referralCode = new Account(this).getReferrerCode();
       /* if (referralLink == null) {
            referralLink = "https://play.google.com/store/apps/details?id=com.eventshigh.nearme.app&referrer=" + Utils.getAndroidId(this);
        }
*/
        branchObject.setCanonicalIdentifier("shareApp")
                .setContentImageUrl("https://pbs.twimg.com/profile_images/720541352556015617/xKvkzMHE.jpg")
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE);
        branchObject.registerView();

        LinkProperties linkProperties = new LinkProperties()
                .setChannel(PACKAGE_NAME_WHATSAPP)
                .setFeature("sharing")
                .addControlParameter("$always_deeplink", "true")
                .addControlParameter("$desktop_url", "https://play.google.com/store/apps/details?id=com.eventshigh.nearme.app");
        final ProgressDialog dialog = OneSecDialog.show(this);
        branchObject.generateShortUrl(this, linkProperties, new Branch.BranchLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, BranchError error) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (error == null) {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, String.format(getString(R.string.share_app_text), referralCode, url));
                    shareIntent.setType("text/plain");
                    shareIntent.setPackage(PACKAGE_NAME_WHATSAPP);
                    try {
                        startActivity(shareIntent);
                    } catch (ActivityNotFoundException e) {
                        showMessage(R.string.no_whatsapp);
                    }
                } else {
                    //   if (error.getErrorCode() == -113) {
                    showMessage(error.getMessage());
                    // }
                }
            }
        });


    }


    /**
     * Helper method to share an Event.
     */
    public void shareEvent(final Event event, @Nullable final String packageName) {
        String src = null;
        if (packageName != null) {
            src = packageName.split("\\.")[1];
        }
        final String eventShareUri = event.getEventShareURI(src).toString();

        final ProgressDialog dialog = OneSecDialog.show(this);
        URLShortenerRequest.submit(this, eventShareUri,
                new Listener<String>() {
                    @Override
                    public void onResponse(String shortenUri, boolean isIntermediate) {
                        dialog.dismiss();
                        shareEvent(event, shortenUri, packageName, null);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        dialog.dismiss();
                        shareEvent(event, eventShareUri, packageName, null);
                    }
                }
        );
    }

    public void shareEventWithBranch(final Event event, @Nullable final String packageName, @Nullable final String label) {

        BranchUniversalObject branchObject = new BranchUniversalObject();


       /* if (referralLink == null) {
            referralLink = "https://play.google.com/store/apps/details?id=com.eventshigh.nearme.app&referrer=" + Utils.getAndroidId(this);
        }
*/
        branchObject.setCanonicalIdentifier(event.id).setTitle(event.title.replaceAll("\"", " &quot "))
                .addContentMetadata("event_id", event.id)
                .addContentMetadata("city_name", event.city.toString())
                .setContentDescription(event.description.replaceAll("\"", " &quot "))
                .setContentImageUrl(event.imgUrl)
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE);
        branchObject.registerView();
        String src = null;
        if (packageName != null) {
            src = packageName.split("\\.")[1];
        }
        LinkProperties linkProperties = new LinkProperties()
                .setChannel(packageName)
                .setFeature("sharing")
                .addControlParameter("$always_deeplink", "true")
                .addControlParameter("$desktop_url", event.getEventShareURI(src).toString());
        //.addControlParameter("$android_url", referralLink)
        //.addControlParameter("$ios_url", "http://www.eventshigh.com");
        final ProgressDialog dialog = OneSecDialog.show(this);
        branchObject.generateShortUrl(this, linkProperties, new Branch.BranchLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, BranchError error) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (error == null) {
                    shareEvent(event, url, packageName, label);
                } else {
                    //   if (error.getErrorCode() == -113) {
                    showMessage(error.getMessage());
                    // }
                }
            }
        });
    }

    public void shareEvent(Event event, String eventUri, @Nullable String packageName, @Nullable String label) {
        String referralCode = new Account(this).getReferrerCode();
        reportEventAction(event, "eventShareInitiated", label == null ? packageName : label);
        shareEventInitiatedTimestamp = System.currentTimeMillis();
        new UserActionHelper(this).recordShareAction(event.id, event.title, packageName, eventUri);

        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    String.format(
                            getString(PACKAGE_NAME_TWITTER.equals(packageName) ?
                                    R.string.share_event_twitter_text : R.string.share_event_text),
                            event.title + (event.isCleanVenue ? " @ " + event.venue : ""), eventUri, referralCode)
            );

            sendIntent.setType("text/plain");
            if (packageName != null) {
                sendIntent.setPackage(packageName);
                startActivity(sendIntent);
            } else {
                startActivity(Intent.createChooser(sendIntent, "Share"));
            }
        } catch (ActivityNotFoundException e) {
            Crashlytics.getInstance().core.logException(e);
            showMessage(R.string.failed_share);
            Log.w(LOG_TAG, "failed sharing", e);
        }
    }

    public void shareEvents(EventsContext eventsContext) {
        reportActionToAnalytics("eventsShareInitiated", eventsContext.getLabel());
        shareEventsInitiatedTimestamp = System.currentTimeMillis();

        String uri = EventsHighEndpoints.getWebUri(eventsContext).buildUpon()
                .appendQueryParameter("src", "ehm").toString();
        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, eventsContext.toString() + "\n\n" + uri);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        } catch (ActivityNotFoundException e) {
            Crashlytics.getInstance().core.logException(e);
            showMessage(R.string.failed_share);
        }
    }

    public void shareEventsWithBranch(final EventsContext eventsContext, @Nullable String imageUrl) {
        String uri = EventsHighEndpoints.getWebUri(eventsContext).buildUpon()
                .appendQueryParameter("src", "ehm").toString();
        // String referralId = new Account(this).getReferrerId();
        /*if (referralLink == null) {
            referralLink = "https://play.google.com/store/apps/details?id=com.eventshigh.nearme.app&referrer=" + Utils.getAndroidId(this);
        }*/
        final String referralCode = new Account(this).getReferrerCode();
        BranchUniversalObject branchObject = new BranchUniversalObject();
        branchObject.setCanonicalIdentifier(eventsContext.getLabel()).setTitle(eventsContext.toString())
                .addContentMetadata("event_uri", uri)
                .setContentDescription(eventsContext.toString())
                .setContentImageUrl(imageUrl)
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE);
        branchObject.registerView();
        LinkProperties linkProperties = new LinkProperties()
                .setChannel("facebook")
                .setFeature("sharing")
                .addControlParameter("$desktop_url", uri);
        // .addControlParameter("$android_url", referralLink);
        // .addControlParameter("$ios_url", "http://www.eventshigh.com");
        final ProgressDialog dialog = OneSecDialog.show(this);

        branchObject.generateShortUrl(this, linkProperties, new Branch.BranchLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, BranchError error) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (error == null) {
                    reportActionToAnalytics("eventsShareInitiated", eventsContext.getLabel());
                    shareEventsInitiatedTimestamp = System.currentTimeMillis();


                    try {
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);
                        sendIntent.putExtra(Intent.EXTRA_TEXT, eventsContext.toString() + "\n\n" + url + "\n\nUse my referral code " + referralCode);
                        sendIntent.setType("text/plain");
                        startActivity(Intent.createChooser(sendIntent, "Share"));
                    } catch (ActivityNotFoundException e) {
                        Crashlytics.getInstance().core.logException(e);
                        showMessage(R.string.failed_share);
                    }
                } else {
                    //   if (error.getErrorCode() == -113) {
                    Toast.makeText(BaseActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    // }
                }
            }
        });

    }

    public void addToCalendar(Event event, @Nullable Date date) {
        reportEventAction(event, "addToCalendar", GAHelper.getDateReportString(date));

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(Events.CONTENT_URI)
                .putExtra(Events.TITLE, event.title)
                .putExtra(Events.EVENT_LOCATION, event.getFullAddress())
                .putExtra(Events.DESCRIPTION, event.getEventShareURI())
                .putExtra(Events.EVENT_LOCATION, event.getShortAddress());

        if (date == null && event.eventTimings.size() > 0) {
            date = new Date(event.eventTimings.get(0));
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
            showMessage(R.string.no_cal_app);
        }
    }

    public void shareCoupon(final OfferObject offer) {
        final String referralCode = new Account(this).getReferrerCode();
        shareOfferInitiatedTimestamp = System.currentTimeMillis();
        BranchUniversalObject branchObject = new BranchUniversalObject();
        String referrerId = new Account(this).getReferrerId();
        branchObject.setCanonicalIdentifier(offer.id + "").setTitle(offer.name.replaceAll("\"", " &quot "))
                .addContentMetadata("offer_id", offer.id + "")
                .setContentDescription(offer.name.replaceAll("\"", " &quot "))
                .setContentImageUrl(offer.imgUrl)
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE);
        branchObject.registerView();

        LinkProperties linkProperties = new LinkProperties()
                .setFeature("sharing")
                .addControlParameter("$always_deeplink", "true")
                .addControlParameter("$desktop_url", "https://play.google.com/store/apps/details?id=com.eventshigh.nearme.app");
        //.addControlParameter("$android_url", referralLink)
        //.addControlParameter("$ios_url", "http://www.eventshigh.com");
        final ProgressDialog dialog = OneSecDialog.show(this);
        branchObject.generateShortUrl(this, linkProperties, new Branch.BranchLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, BranchError error) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (error == null) {
                    reportActionToAnalytics("offerShareInitiated", offer.name);
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT,
                            String.format(
                                    getString(
                                            R.string.share_coupon_text),
                                    offer.name, url, referralCode)
                    );

                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, "Share"));
                } else {
                    //   if (error.getErrorCode() == -113) {
                    showMessage(error.getMessage());
                    // }
                }
            }
        });
    }

    public boolean isFavourite(Event event) {
        return EventsMarkerManager.getInstance(this).isFavourite(event.id);
    }

    public boolean isMovieFavourite(MovieInfoObject movie) {
        return MovieMarkerManager.getInstance(this).isFavourite(movie.getId() + "");
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

    public void reportMovieAction(MovieInfoObject movieInfoObject, String actionName, @Nullable String label) {
        if (movieInfoObject != null) {
            reportActionToAnalytics(actionName,
                    label == null ? "" : label,
                    1,
                    isMovieFavourite(movieInfoObject) ? "Favourite" : "No-Favourite");
        }
    }

    public void reportCampaignParams(String campaignData) {
        if (isPlayServicesPresent) {
            gaHelper.reportCampaignParams(campaignData);
        }
    }

    private static final Set<String> INSTALLED_PACKAGES = new HashSet<>();

    protected boolean isInstalled(String packageName) {
        return isInstalled(this, packageName);
    }

    public static boolean isInstalled(Context context, String packageName) {
        synchronized (INSTALLED_PACKAGES) {
            if (INSTALLED_PACKAGES.isEmpty()) {
                for (PackageInfo packageInfo : context.getPackageManager().getInstalledPackages(0)) {
                    if (packageInfo.versionName != null && packageInfo.applicationInfo.enabled) {
                        INSTALLED_PACKAGES.add(packageInfo.packageName);
                    }
                }
            }
        }

        return INSTALLED_PACKAGES.contains(packageName);
    }


    public void showRedeemCouponActivity(OfferObject offer, long totalPoints) {
        Intent intent = new Intent(this, RedeemCouponActivity.class);
        intent.putExtra("offer", offer);
        intent.putExtra("total_points", totalPoints);
        startActivity(intent);
        //overridePendingTransition(R.anim.animate_bottom_up, R.anim.stay);
    }

    public void showOfferSignUpActivity(OfferObject offer) {
        Intent intent = new Intent(this, OfferSignUpActivity.class);
        intent.putExtra("offer", offer);
        startActivity(intent);
        overridePendingTransition(R.anim.animate_bottom_up, R.anim.stay);
    }

    public boolean isOfferActed(int id) {
        Preferences preferences = Preferences.getInstance(this);
        ArrayList<String> ids = new ArrayList<>();
        if (preferences.getPrefOfferActedId().length() > 0) {
            ids.addAll(Arrays.asList(preferences.getPrefOfferActedId().split(",")));
            for (String value : ids) {
                if (Integer.parseInt(value) == id)
                    return true;

            }
        }
        return false;
    }

    public void shareMovie(final MovieDetailObject movie) {
        shareMovieInitiatedTimestamp = System.currentTimeMillis();
        BranchUniversalObject branchObject = new BranchUniversalObject();
        String referrerId = new Account(this).getReferrerId();
        branchObject.setCanonicalIdentifier(movie.getMovieInfo().getId() + "").setTitle(movie.getMovieInfo().getName().replaceAll("\"", " &quot "))
                .addContentMetadata("movie_id", movie.getMovieInfo().getId() + "")
                .setContentDescription(movie.getMovieInfo().getName().replaceAll("\"", " &quot "))
                .setContentImageUrl(movie.getMovieInfo().getImg_url())
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE);
        branchObject.registerView();
        String uri = EventsHighEndpoints.WEB_URI_BASE + "movie/" + movie.getMovieInfo().getId();
        LinkProperties linkProperties = new LinkProperties()
                .setFeature("sharing")
                .addControlParameter("$always_deeplink", "true")
                .addControlParameter("$desktop_url", uri);
        //.addControlParameter("$android_url", referralLink)
        //.addControlParameter("$ios_url", "http://www.eventshigh.com");
        final ProgressDialog dialog = OneSecDialog.show(this);
        branchObject.generateShortUrl(this, linkProperties, new Branch.BranchLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, BranchError error) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (error == null) {
                    reportActionToAnalytics("movieShareInitiated", movie.getMovieInfo().getName());
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT,
                            String.format(
                                    getString(
                                            R.string.share_movie_text),
                                    movie.getMovieInfo().getName(), url)
                    );

                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, "Share"));
                } else {
                    //   if (error.getErrorCode() == -113) {
                    showMessage(error.getMessage());
                    // }
                }
            }
        });

    }

    void startTicketShareActivity(int noOfTickets, Event event, String ticketLink) {
        shareMovieInitiatedTimestamp = System.currentTimeMillis();
        reportActionToAnalytics("ticketShareInitiated", ticketLink);
        String shareText = "Hey, I just book " + noOfTickets + " tickets for " + event.title + " on EventsHigh. It'll be fun.\n" + ticketLink;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        this.startActivity(Intent.createChooser(shareIntent, "Share"));

    }

    //share profile
    public void shareProfileWithBranch(final ProfileInfo profileInfo, String email, @Nullable final String packageName, @Nullable final String label) {

        BranchUniversalObject branchObject = new BranchUniversalObject();


       /* if (referralLink == null) {
            referralLink = "https://play.google.com/store/apps/details?id=com.eventshigh.nearme.app&referrer=" + Utils.getAndroidId(this);
        }
        */
        branchObject.setCanonicalIdentifier(email)
                .setTitle(profileInfo.getName())//event.title.replaceAll("\"", " &quot "))
                .addContentMetadata("profile_id", email)
                .setContentDescription("Check Events High Profile")//event.description.replaceAll("\"", " &quot "))
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PRIVATE);
        if (!Utils.checkIfStringEmpty(profileInfo.getProfilePic())) {
            branchObject.setContentImageUrl(profileInfo.getProfilePic());
        }
        branchObject.registerView();
        String src = null;
        if (packageName != null) {
            src = packageName.split("\\.")[1];
        }
        LinkProperties linkProperties = new LinkProperties()
                .setChannel(packageName)
                .setFeature("sharing")
                .addControlParameter("$always_deeplink", "true")
                .addControlParameter("$desktop_url", "http://www.eventshigh.com");//profileInfo.getProfileShareURI(src).toString());
        //.addControlParameter("$android_url", referralLink)
        //.addControlParameter("$ios_url", "http://www.eventshigh.com");
        final ProgressDialog dialog = OneSecDialog.show(this);
        branchObject.generateShortUrl(this, linkProperties, new Branch.BranchLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, BranchError error) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                if (error == null) {
                    shareProfile(profileInfo, url, packageName, label);
                } else {
                    //   if (error.getErrorCode() == -113) {
                    showMessage(error.getMessage());
                    // }
                }
            }
        });

    }

    public void shareProfile(ProfileInfo profileInfo, String profileUri,
                             @Nullable String packageName, @Nullable String label) {

        shareProfileInitiatedTimestamp = System.currentTimeMillis();
        reportActionToAnalytics("profileShareInitiated", profileUri);
        String referralCode = new Account(this).getReferrerCode();

        long shareProfileInitiatedTimestamp = System.currentTimeMillis();


        try {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    String.format("Check out " + profileInfo.getName() + "'s profile on Events High! - "
                            + profileUri)
            );

            sendIntent.setType("text/plain");
            if (packageName != null) {
                sendIntent.setPackage(packageName);
                startActivity(sendIntent);
            } else {
                startActivity(Intent.createChooser(sendIntent, "Share"));
            }
        } catch (ActivityNotFoundException e) {
            Crashlytics.getInstance().core.logException(e);
            showMessage(R.string.failed_share);
            Log.w(LOG_TAG, "failed sharing", e);
        }
    }

}