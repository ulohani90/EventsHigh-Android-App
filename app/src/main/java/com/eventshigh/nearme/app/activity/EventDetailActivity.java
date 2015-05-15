package com.eventshigh.nearme.app.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.network.EventRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.security.Signer;
import com.eventshigh.nearme.app.ui.RateAppDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.AccountStateReporter;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.utils.ZendeskUtils;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.model.LatLng;
import com.zendesk.sdk.feedback.ui.ContactZendeskActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

/**
 * An activity representing a single Event detail screen. This activity can be called from deep
 * link or from Events{Grid,Maps}Activity. In both cases, event data is not available so
 * this activity fetches the event data and shows it using the EventDetailFragment.
 */
public class EventDetailActivity extends BaseActivity implements LocationListener {
    public static final String EXTRA_EVENT_PARAM = EventDetailActivity.class.getSimpleName() + "_event";

    // Request code for location settings check intent
    private static final int REQUEST_CHECK_SETTINGS = 1020;

    public static final String PACKAGE_NAME_FACEBOOK = "com.facebook.katana";
    public static final String PACKAGE_NAME_TWITTER = "com.twitter.android";
    public static final String PACKAGE_NAME_EMAIL = "com.google.android.gm";
    public static final String PACKAGE_NAME_WHATSAPP = "com.whatsapp";

    private static final long LOCATION_LOCK_MAX_TIME_MILLIS = 10 * DateUtils.SECOND_IN_MILLIS;
    private static final float LOCATION_ACCURACY_REQUIRED_METERS = 300;
    private static final float ALLOWED_USER_DISTANCE_FROM_EVENT_FOR_CHECK_IN_METERS = 300;

    private Toolbar toolbar;
    private View topProgressBar;
    private EventCard eventCard;
    private LatLng userLocation = null;
    private Event event = null;
    private GoogleApiClient client;
    private boolean showRateAppDialog = false;  // TODO: save this in bundle and restore

    private LocationRequest checkInLocationRequest;
    private AlertDialog checkInAlertDialog;
    private long locationRequestStartTime;


    /*****************************************
     Activity lifecycle management utilities
     ***************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_event_detail);
        eventCard = new EventCard();
        topProgressBar = findViewById(R.id.top_progress_bar);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        onNewIntent(getIntent());

        // Initialize location request used for check in.
        checkInLocationRequest = new LocationRequest();
        checkInLocationRequest.setInterval(3000);
        checkInLocationRequest.setFastestInterval(1000);
        checkInLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void onStart() {
        super.onStart();

        String action = getIntent().getAction();
        if (BaseActivity.NOTIFICATION_ACTION.equals(action)) {
            reportActionToAnalytics("openNotification", getIntent().getData().getLastPathSegment());
        }

        findViewById(R.id.event_container).setMinimumHeight(
                (int) (1.33 * getResources().getDisplayMetrics().heightPixels));

        // Get the event from Intent.
        if (getIntent().hasExtra(EXTRA_EVENT_PARAM)) {
            Event event = getIntent().getParcelableExtra(EXTRA_EVENT_PARAM);
            populateView(event);
        } else {
            EventRequest.submit(this, getIntent().getData(), Priority.IMMEDIATE, mEventListener,
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            Toast.makeText(EventDetailActivity.this, R.string.failed_load,
                                    Toast.LENGTH_SHORT).show();
                            Log.e(EventDetailActivity.class.getSimpleName(), volleyError.toString(),
                                    volleyError.getCause());
                            finish();
                        }
                    });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (client != null && client.isConnected()) {
            if (event != null) {
                Uri webUri = event.getEventDetailsURI();
                AppIndex.AppIndexApi.viewEnd(client, this, Utils.getAppUri(webUri));
            }
            client.disconnect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (showRateAppDialog) {
            RateAppDialog.show(this);
            showRateAppDialog = false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // All required changes were successfully made
                        startLocationDetection();
                        break;
                    case Activity.RESULT_CANCELED:
                        // The user was asked to change settings, but chose not to
                        locationDetectionFailed();
                        break;
                    default:
                        break;
                }
                break;
        }
    }


    /**********************************
     Callbacks, action handlers
     **********************************/

    public void save(View view) {
        showRateAppDialog = true;
        reportEventAction(event, "addToCalendar" + (view instanceof TextView ? "" : "2"));

        addToCalendar(event, null);
    }

    public void openSourceSite(View view) {
        reportEventAction(event, "openSource");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.sourceUrl));
        startActivitySafe(intent);
    }

    public  void call(View view) {
        if (event.organizerPhone == null) {
            return;
        }

        showRateAppDialog = true;
        reportEventAction(event, "callOrganizer" + (view instanceof TextView ? "2" : ""));

        Intent intent = new Intent(Intent.ACTION_DIAL)
                .setData(Uri.parse("tel:" + (event.organizerPhone.split(",")[0])));
        startActivitySafe(intent);

    }

    public void openOrganizerLink(View view) {
        reportEventAction(event, "openOrganizerLink");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.organizerLink));
        startActivitySafe(intent);
    }

    public void openOrganizerWebsite(View view) {
        reportEventAction(event, "openOrganizerWebsite");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.organizerWebsite));
        startActivitySafe(intent);
    }

    public void openBookingSite(View view) {
        showRateAppDialog = true;
        reportEventAction(event, "bookTicket");

        final Uri.Builder bookingUriBuilder = Uri.parse(event.bookingUrl).buildUpon();
        if (event.bookingUrl != null && event.bookingUrl.contains("ticketing.eventshigh.com")) {
            bookingUriBuilder.appendQueryParameter("did", Utils.getAndroidId(this));
        }

        CustomUrlActivity.launchCustomUrl(this, bookingUriBuilder.build(),
                getString(R.string.title_book));
    }

    public void openOfferSite(View view) {
        reportEventAction(event, "openOffer");
        CustomUrlActivity.launchCustomUrl(this,
                Uri.parse("http://www.eventshigh.com/get_event_contest/" + event.id),
                event.offerTitle);
    }

    public void onCheckIn(View view) {
        reportEventAction(event, "onCheckIn");

        // Check if the user is logged in
        Pair<String, Boolean> phoneNumberStatus = new Account(this).getPhoneNumber();
        if (!phoneNumberStatus.second) {
            reportEventAction(event, "checkInPhoneNoRequired");
            new AlertDialog.Builder(this)
                .setTitle(R.string.pref_title_phone_no)
                .setMessage(R.string.ui_register_for_check_in)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(EventDetailActivity.this, PhoneLoginActivity.class));
                    }
                })
                .show();
            return;
        }

        // Check if we have high accuracy location.
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
            .addLocationRequest(checkInLocationRequest);
        PendingResult<LocationSettingsResult> result =
            LocationServices.SettingsApi.checkLocationSettings(client, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        startLocationDetection();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(EventDetailActivity.this,
                                REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            locationDetectionFailed();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        locationDetectionFailed();
                        break;
                }
            }
        });

        createAlertDialog(R.string.ui_detecting_location);
        checkInAlertDialog.setOnDismissListener(
                new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        stopLocationDetection();
                        dialog.dismiss();
                    }
                }
        );
        checkInAlertDialog.show();
    }

    public void imagePreview(View view) {
        if (event.imgUrl == null) {
            return;
        }

        reportEventAction(event, "imagePreview");

        final Dialog nagDialog = new Dialog(this,
                android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        nagDialog.setCancelable(true);
        nagDialog.setContentView(R.layout.dialog_image_preview);

        ImageViewTouch preview = (ImageViewTouch) nagDialog.findViewById(R.id.image_preview);
        VolleyHelper.getImageLoader(EventDetailActivity.this).get(
                event.imgUrl, ImageLoader.getImageListener(preview, 0, 0));

        Button btnClose = (Button) nagDialog.findViewById(R.id.btn_close);
        btnClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                nagDialog.dismiss();
            }
        });

        nagDialog.show();
    }

    public void removeFavourite(View v) {
        reportEventAction(event, "removeFavourite");

        EventsMarkerManager.Editor eventsMarkerEditor =
                EventsMarkerManager.getInstance(this).getEditor();
        eventsMarkerEditor.recordEventMark(event, null);
        eventCard.setFavouriteView(false);
        eventsMarkerEditor.close();
    }

    public void addFavourite(View v) {
        reportEventAction(event, "addFavourite");

        EventsMarkerManager.Editor eventsMarkerEditor =
                EventsMarkerManager.getInstance(this).getEditor();
        eventsMarkerEditor.recordEventMark(event, EventMark.FAVOURITE);
        eventCard.setFavouriteView(true);
        eventsMarkerEditor.close();
    }

    public void showDirections(View view) {
        reportEventAction(event, "showDirections");

        Intent intent = event.getShowOnMapIntent();
        if (intent == null) {
            reportActionToAnalytics("skipDirectionsNoLocation");
            Toast.makeText(this, R.string.failed_event_location, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open maps.
            Toast.makeText(this, R.string.no_map_app, Toast.LENGTH_SHORT).show();
        }
    }

    public void showVenue(View view) {
        if (event.isCleanVenue && event.venue != null) {
            reportEventAction(event, "seeVenue", event.venue);
            startActivity(new Intent(EventDetailActivity.this, LaunchActivity.class)
                    .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT,
                            new EventsContext(null, event.venue.toLowerCase())));
        } else {
            showDirections(view);
        }
    }

    public void ama(View view) {
        reportEventAction(event, "ama");

        ZendeskUtils.initZendesk(this);
        ZendeskUtils.setEventFeedbackConfiguration(this, event);
        Intent feedbackIntent = new Intent(this, ContactZendeskActivity.class);
        startActivity(feedbackIntent);
    }

    public void facebook(View view) {
        shareEvent(event, PACKAGE_NAME_FACEBOOK);
        if (ShareDialog.canShow(ShareLinkContent.class)) {
            ShareLinkContent content = new ShareLinkContent.Builder()
                    .setContentUrl(event.getEventShareURI(this, "fb"))
                    .build();
            ShareDialog.show(this, content);
        } else {
            shareEvent(event, PACKAGE_NAME_FACEBOOK);
        }
    }

    public void twitter(View view) {
        shareEvent(event, PACKAGE_NAME_TWITTER);
    }

    public void email(View view) {
        shareEvent(event, PACKAGE_NAME_EMAIL);
    }

    public void whatsapp(View view) {
        shareEvent(event, PACKAGE_NAME_WHATSAPP);
    }


    /**********************************
     Helper methods
     **********************************/

    private void startActivitySafe(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open url. ignore.
        }
    }

    private void populateView(Event event) {
        this.event = event;

        // Set Title.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(event.title);
            if (event.numPeopleInterested <= 0) {
                actionBar.setSubtitle("");
            } else {
                String text = getResources().getQuantityString(R.plurals.people_interested,
                        event.numPeopleInterested, event.numPeopleInterested);
                actionBar.setSubtitle(text);
            }
        }

        // Populate event details.
        toolbar.setAlpha(0f);
        eventCard.populateView(event);

        // Connect to Google API client to notify the view.
        getGoogleApiClient();
    }

    private void setScroll(int scrollValue) {
        float opacity = Math.min(1.0f, scrollValue * 3f / getResources().getDisplayMetrics().heightPixels);
        toolbar.setAlpha(opacity);
    }

    private Listener<Event> mEventListener = new Listener<Event>() {
        @Override
        public void onResponse(final Event event, boolean isIntermediate) {
            populateView(event);
        }
    };

    private void askOverEmail() {
        try {
            int pos = event.title.indexOf(' ', 30);
            String title = pos == -1 ? event.title : event.title.substring(0, pos) + " ...";
            Uri sendTo = Uri.parse("mailto:" + event.organizerEmail +
                    "?cc=support@eventshigh.com&subject=" +
                    URLEncoder.encode("Event Query: " + title, "UTF-8"));
            Intent sendIntent = new Intent(Intent.ACTION_SENDTO, sendTo);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Event Link: " + event.getEventDetailsURI() +
                    "\n\nQuery:\n<please type in your question here>");
            startActivitySafe(sendIntent);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.hasAccuracy() && location.getAccuracy() < LOCATION_ACCURACY_REQUIRED_METERS) {
            // We have enough location accuracy. Stop detecting location, and close dialog
            stopLocationDetection();
            checkInAlertDialog.dismiss();

            // Lets check if the user is near the event
            userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            if (Utils.isDebug(this) ||
                LocationUtils.distanceInMeters(event.location, userLocation)
                    < ALLOWED_USER_DISTANCE_FROM_EVENT_FOR_CHECK_IN_METERS) {
                // Yes, user is near the event, start the check in flow
                checkIn();
            } else {
                // The user is not near the location, let the user know
                reportEventAction(event, "checkInFailedNotAtLocation");
                Toast.makeText(this, R.string.ui_not_at_event, Toast.LENGTH_LONG).show();
            }
        } else {
            // Location is not accurate enough. Wait for some more time until the timeout has
            // reached, and then let the user know that the location could not be detected
            if (System.currentTimeMillis() - locationRequestStartTime >
                LOCATION_LOCK_MAX_TIME_MILLIS) {
                locationDetectionFailed();
            }
        }
    }

    private void locationDetectionFailed() {
        reportEventAction(event, "checkInFailedLocation");
        stopLocationDetection();
        Toast.makeText(this, R.string.failed_location, Toast.LENGTH_LONG).show();
        checkInAlertDialog.dismiss();
    }

    private void createAlertDialog(int messageResourceId) {
        checkInAlertDialog = new AlertDialog.Builder(this).create();
        @SuppressLint("InflateParams")
        View dialogView = checkInAlertDialog.getLayoutInflater().inflate(R.layout.dialog_busy, null);
        ((TextView) dialogView.findViewById(R.id.message)).setText(messageResourceId);
        checkInAlertDialog.setView(dialogView);
    }

    private void startLocationDetection() {
        locationRequestStartTime = System.currentTimeMillis();
        LocationServices.FusedLocationApi.requestLocationUpdates(client, checkInLocationRequest, this);
    }

    private void stopLocationDetection() {
        if (client.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(client,
                    EventDetailActivity.this);
        }
    }

    private void checkIn() {
        reportEventAction(event, "checkInSubmit");
        try {
            Uri requestUrl = AccountStateReporter.getBaseUri(this, "event_checkin")
                .appendQueryParameter("event_id", event.id)
                .appendQueryParameter("timestamp", "" + System.currentTimeMillis())
                .appendQueryParameter("location",
                    URLEncoder.encode(userLocation.toString(), "UTF-8"))
                .build();

            createAlertDialog(R.string.ui_checking_in);
            checkInAlertDialog.show();

            VolleyHelper.addToRequestQueue(this,
                new JsonObjectRequest(Request.Method.GET, Signer.sign(requestUrl).toString(), null,
                    new Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response, boolean isIntermediate) {
                            try {
                                checkInSuccess(response.getString("total_points"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            checkInFailed();
                        }
                    }
                )
            );
        } catch (IOException | GeneralSecurityException e) {
            checkInFailed();
        }
    }

    private void checkInFailed() {
        checkInAlertDialog.dismiss();
        reportActionToAnalytics("checkInFailed");
        Toast.makeText(EventDetailActivity.this, R.string.check_in_failed,
            Toast.LENGTH_SHORT).show();
    }

    private void checkInSuccess(String points) {
        checkInAlertDialog.dismiss();
        reportActionToAnalytics("checkInSuccess");
        String message = String.format(getResources().getString(R.string.check_in_success), points);
        Toast.makeText(EventDetailActivity.this, message, Toast.LENGTH_SHORT).show();
        Preferences.getInstance(this).setPoints(points);
    }

    private class EventCard {
        private final ScrollView eventScrollView;

        private final NetworkImageView bgView;
        private final ImageView recommendedImageView;

        private final TextView titleView;
        private final TextView fromView;

        private final TextView favouriteView;
        private final TextView favouritedView;

        private final TextView venueView;
        private final TextView addressView;
        private final TextView travelTimeView;

        private final LinearLayout timeGroupView;
        private final RelativeLayout eventTimeFirstView;
        private final TextView timeView;
        private final TextView timeDetailView;
        private final TextView alsoOnView;
        private final HorizontalScrollView futureTimesViewGroup;
        private final LinearLayout futureTimesView;

        private final FrameLayout bookView;
        private final FrameLayout checkInView;
        private final FrameLayout callView;
        private final TextView priceView;
        private final TextView offerView;

        private final View tagsHeaderView;
        private final LinearLayout tagsView;
        private final View descriptionHeaderView;
        private final WebView descriptionView;

        private final View performerHeaderView;
        private final LinearLayout performersView;

        private final TextView organizerHeader;
        private final LinearLayout organizerNameRow;
        private final TextView organizerNameView;
        private final TextView organizerLinkView;
        private final LinearLayout organizerEmailRow;
        private final TextView organizerEmailView;
        private final LinearLayout organizerPhoneRow;
        private final TextView organizerPhoneView;
        private final LinearLayout organizerWebsiteRow;
        private final TextView organizerWebsiteView;


        private EventCard() {
            eventScrollView = (ScrollView) findViewById(R.id.event_scroll_view);

            recommendedImageView = (ImageView) findViewById(R.id.eh_recommends);
            bgView = (NetworkImageView) findViewById(R.id.event_bg);

            favouriteView = (TextView) findViewById(R.id.action_favourite);
            favouritedView = (TextView) findViewById(R.id.action_favourited);

            titleView = (TextView) findViewById(R.id.event_title);
            fromView = (TextView) findViewById(R.id.event_from);

            venueView = (TextView) findViewById(R.id.event_venue);
            addressView = (TextView) findViewById(R.id.event_address);
            travelTimeView = (TextView) findViewById(R.id.event_travel_time);

            timeGroupView = (LinearLayout) findViewById(R.id.event_time_group);
            eventTimeFirstView = (RelativeLayout) findViewById(R.id.event_time_first);
            timeView = (TextView) findViewById(R.id.event_time);
            timeDetailView = (TextView) findViewById(R.id.event_time_details);
            alsoOnView = (TextView) findViewById(R.id.also_on);
            futureTimesViewGroup = (HorizontalScrollView) findViewById(R.id.event_future_times_hs);
            futureTimesView = (LinearLayout) findViewById(R.id.event_future_times);

            bookView = (FrameLayout) findViewById(R.id.book_ticket);
            checkInView = (FrameLayout) findViewById(R.id.check_in);
            callView = (FrameLayout) findViewById(R.id.call);
            priceView = (TextView) findViewById(R.id.event_price);
            offerView = (TextView) findViewById(R.id.offer_text);

            tagsHeaderView = findViewById(R.id.tags_header);
            tagsView = (LinearLayout) findViewById(R.id.event_tags);
            descriptionHeaderView = findViewById(R.id.description_header);
            descriptionView = (WebView) findViewById(R.id.event_description);

            performerHeaderView = findViewById(R.id.performer_header);
            performersView = (LinearLayout) findViewById(R.id.performers);

            organizerHeader = (TextView) findViewById(R.id.organizer_header);
            organizerNameRow = (LinearLayout) findViewById(R.id.organizer_name_row);
            organizerNameView = (TextView) findViewById(R.id.organizer_name);
            organizerLinkView = (TextView) findViewById(R.id.organizer_link);
            organizerEmailRow = (LinearLayout) findViewById(R.id.organizer_email_row);
            organizerEmailView = (TextView) findViewById(R.id.organizer_email);
            organizerPhoneRow = (LinearLayout) findViewById(R.id.organizer_phone_row);
            organizerPhoneView = (TextView) findViewById(R.id.organizer_phone);
            organizerWebsiteRow = (LinearLayout) findViewById(R.id.organizer_website_row);
            organizerWebsiteView = (TextView) findViewById(R.id.organizer_website);

            // Set Image view dimensions.
            final DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            ViewGroup.LayoutParams params = bgView.getLayoutParams();
            params.height = 9 * metrics.widthPixels / 16;
            bgView.setLayoutParams(params);
        }

        private void populateView(final Event event) {
            eventScrollView.getViewTreeObserver().addOnScrollChangedListener(
                new OnScrollChangedListener() {
                    @Override
                        public void onScrollChanged() {
                            setScroll(eventScrollView.getScrollY());
                        }
                    });
            eventScrollView.setVisibility(View.VISIBLE);
            topProgressBar.setVisibility(View.GONE);

            bgView.setDefaultImageResId(R.drawable.eh_default_event);
            bgView.setErrorImageResId(R.drawable.eh_default_event);
            bgView.setImageUrl(event.imgUrl, VolleyHelper.getImageLoader(EventDetailActivity.this));

            // Set title
            titleView.setText(event.title);

            // Add attribution.
            if (event.sourceUrl == null) {
                fromView.setVisibility(View.INVISIBLE);
            } else {
                final Uri fromUri = Uri.parse(event.sourceUrl);
                String eventFrom = String.format(
                        getResources().getString(R.string.event_detail_from),
                        fromUri.getHost());
                fromView.setText(eventFrom);
            }

            // Set EH recommendation and favourite views.
            recommendedImageView.setVisibility(event.ehRecommended ? View.VISIBLE : View.GONE);
            setFavouriteView(EventsMarkerManager.getInstance(EventDetailActivity.this)
                    .isFavourite(event.id));

            // Set Venue and address.
            findViewById(R.id.venue_group).setVisibility(View.VISIBLE);
            venueView.setText(event.getShortAddress());
            addressView.setText(event.getFullAddress());
            travelTimeView.setVisibility(View.GONE);

            // Set time.
            EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
            timeGroupView.setVisibility(eventTime == null ? View.GONE : View.VISIBLE);
            if (eventTime != null) {
                timeView.setText(eventTime.toString());
                int numDays = DateTimeUtils.getDaysLater(event);
                if (numDays >= 0) {
                    timeDetailView.setText(
                        MessageFormat.format(getResources().getString(R.string.event_time_details), numDays));
                }

                futureTimesView.removeAllViews();
                if (event.eventTimings.length > 1) {
                    for (int i = 1; i < event.eventTimings.length; i++) {
                        eventTime = DateTimeUtils.getEventTime(event, i);
                        if (eventTime == null) {
                            break;
                        }

                        final Date eventDateCurr = new Date(event.eventTimings[i]);
                        View timeView = getLayoutInflater().inflate(
                                R.layout.view_event_time, futureTimesView, false);
                        ((TextView) timeView.findViewById(R.id.event_day)).setText(
                                eventTime.day + ", " + eventTime.date);
                        ((TextView) timeView.findViewById(R.id.event_time)).setText(eventTime.time);
                        futureTimesView.addView(timeView);
                        timeView.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                addToCalendar(event, eventDateCurr);
                            }
                        });
                    }

                    alsoOnView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (futureTimesViewGroup.getVisibility() == View.GONE) {
                                futureTimesViewGroup.setVisibility(View.VISIBLE);
                                eventTimeFirstView.setVisibility(View.GONE);
                            } else {
                                futureTimesViewGroup.setVisibility(View.GONE);
                                eventTimeFirstView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                } else {
                    alsoOnView.setVisibility(View.GONE);
                }
            }

            // Show offer if its there.
            offerView.setVisibility(event.offerTitle == null ? View.GONE : View.VISIBLE);
            if (event.offerTitle != null) {
                offerView.setVisibility(View.VISIBLE);
                offerView.setText(event.offerTitle);
            }

            // Set action buttons.
            findViewById(R.id.action_button_group).setVisibility(View.VISIBLE);
            bookView.setVisibility(event.bookingUrl != null ? View.VISIBLE : View.GONE);
            callView.setVisibility(event.organizerPhone != null ? View.VISIBLE : View.GONE);
            mayBeShowCheckInButton(event);

            // Show price.
            findViewById(R.id.price_row).setVisibility(View.VISIBLE);
            String priceString = event.getPriceString();
            priceView.setText(priceString == null ? getString(R.string.no_price) : priceString);

            // Show performers if any.
            performersView.removeAllViews();
            performerHeaderView.setVisibility(event.performers.length == 0 ? View.GONE : View.VISIBLE);
            if (event.performers.length > 0) {
                for (final String performer : event.performers) {
                    addTagView(performersView, performer, "performerClick");
                }
            }

            // Show tags.
            tagsView.removeAllViews();
            tagsHeaderView.setVisibility(event.tags.length == 0 ? View.GONE : View.VISIBLE);
            if (event.tags.length > 0) {
                tagsHeaderView.setVisibility(View.VISIBLE);
                for (final String tag : event.tags) {
                    addTagView(tagsView, tag, "tagClick");
                }
            }

            // Set description.
            descriptionHeaderView.setVisibility(event.description.isEmpty() ? View.GONE : View.VISIBLE);
            if (!event.description.isEmpty()) {
                final String html = "<body>" + event.description + "</body>";
                descriptionView.loadData(html, "text/html; charset=UTF-8", null);
                WebSettings webSettings = descriptionView.getSettings();
                webSettings.setDefaultFontSize(12);
                descriptionHeaderView.setVisibility(View.VISIBLE);
            }

            // Organizer Info.
            boolean organizerInfoShown = false;
            organizerNameRow.setVisibility(event.organizerName == null ? View.GONE :View.VISIBLE);
            if (event.organizerName != null) {
                organizerInfoShown = true;
                organizerNameView.setText(event.organizerName);
                if (event.organizerLink != null) {
                    organizerLinkView.setText(event.organizerLink);
                } else {
                    organizerLinkView.setVisibility(View.GONE);
                }
            }

            organizerEmailRow.setVisibility(event.organizerEmail == null ? View.GONE : View.VISIBLE);
            if (event.organizerEmail != null) {
                organizerInfoShown = true;
                organizerEmailView.setText(event.organizerEmail);
                organizerEmailView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        reportEventAction(event, "emailOrganizer2");
                        askOverEmail();
                    }
                });
            }

            organizerPhoneRow.setVisibility(event.organizerPhone == null ? View.GONE : View.VISIBLE);
            if (event.organizerPhone != null) {
                organizerInfoShown = true;
                organizerPhoneView.setText(event.organizerPhone);
            }

            organizerWebsiteRow.setVisibility(event.organizerWebsite == null ? View.GONE : View.VISIBLE);
            if (event.organizerWebsite != null) {
                organizerInfoShown = true;
                organizerWebsiteView.setText(event.organizerWebsite);
            }

            organizerHeader.setVisibility(organizerInfoShown ? View.VISIBLE : View.GONE);

            // Share Buttons.
            findViewById(R.id.share_fb).setVisibility(isInstalled(PACKAGE_NAME_FACEBOOK) ? View.VISIBLE : View.GONE);
            findViewById(R.id.share_twitter).setVisibility(isInstalled(PACKAGE_NAME_TWITTER) ? View.VISIBLE : View.GONE);
            findViewById(R.id.share_email).setVisibility(isInstalled(PACKAGE_NAME_EMAIL) ? View.VISIBLE : View.GONE);
            findViewById(R.id.share_whatsapp).setVisibility(isInstalled(PACKAGE_NAME_WHATSAPP) ? View.VISIBLE : View.GONE);
        }

        private void mayBeShowCheckInButton(Event event) {
            boolean isDebug = Utils.isDebug(EventDetailActivity.this);

            // Check in starts 30 mins before event start time and till 2 hours after event
            // start time.
            long checkInStartTimeMillis = event.eventTimings[0] -
                    (DateUtils.MINUTE_IN_MILLIS * 30 * (isDebug ? 2 : 1));
            long checkInEndTimeMillis = event.eventTimings[0] +
                    (DateUtils.MINUTE_IN_MILLIS * 120 * (isDebug ? 2 : 1));

            long currentTimeMillis = System.currentTimeMillis();
            if (isPlayServicesPresent && checkInStartTimeMillis < currentTimeMillis &&
                    currentTimeMillis < checkInEndTimeMillis) {
                // We have established that the time is right
                checkInView.setVisibility(View.VISIBLE);
            } else {
                checkInView.setVisibility(View.GONE);
            }

            // All three action buttons are not good to show. Hide call in such case.
            if (checkInView.getVisibility() == View.VISIBLE &&
                bookView.getVisibility() == View.VISIBLE && callView.getVisibility() == View.VISIBLE) {
                callView.setVisibility(View.GONE);
            }
        }

        private void addTagView(LinearLayout parent, final String tagName, final String action) {
            getLayoutInflater().inflate(R.layout.view_event_tag, parent);
            View tagView = parent.getChildAt(parent.getChildCount() - 1);
            TextView tagNameView = (TextView) tagView.findViewById(R.id.tag_name);
            tagNameView.setText(tagName);
            tagNameView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEventAction(event, action, tagName);
                    Intent searchIntent = new Intent(EventDetailActivity.this, LaunchActivity.class);
                    searchIntent.putExtra(IntentUtils.EXTRA_EVENT_CONTEXT,
                            new EventsContext(userLocation, tagName.toLowerCase()));
                    startActivity(searchIntent);
                }
            });

            final Account account = new Account(EventDetailActivity.this);
            boolean isFollowing = account.isFollowing(tagName);
            final View followView = tagView.findViewById(R.id.follow_button);
            final View followingView = tagView.findViewById(R.id.following_button);
            followingView.setSelected(true);
            if (isFollowing) {
                followView.setVisibility(View.GONE);
            } else {
                followingView.setVisibility(View.GONE);
            }

            followView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportActionToAnalytics("addFollowing", tagName);
                    account.setIsFollowing(tagName, true);
                    followView.setVisibility(View.GONE);
                    followingView.setVisibility(View.VISIBLE);
                }
            });
            followingView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportActionToAnalytics("removeFollowing", tagName);
                    account.setIsFollowing(tagName, false);
                    followView.setVisibility(View.VISIBLE);
                    followingView.setVisibility(View.GONE);
                }
            });
        }

        private void setFavouriteView(boolean isFavourite) {
            favouritedView.setVisibility(isFavourite ? View.VISIBLE : View.GONE);
            favouriteView.setVisibility(isFavourite ? View.GONE : View.VISIBLE);
        }
    }

    private static final Set<String> INSTALLED_PACKAGES = new HashSet<>();
    private boolean isInstalled(String packageName) {
        synchronized (INSTALLED_PACKAGES) {
            if (INSTALLED_PACKAGES.isEmpty()) {
                getInstalledApps();
            }
        }

        return INSTALLED_PACKAGES.contains(packageName);
    }

    private void getInstalledApps() {
        for(PackageInfo packageInfo : getPackageManager().getInstalledPackages(0)) {
            if (packageInfo.versionName != null && packageInfo.applicationInfo.enabled) {
                INSTALLED_PACKAGES.add(packageInfo.packageName);
            }
        }
    }

    private void populateEventTravelTime() {
        Location location = LocationServices.FusedLocationApi.getLastLocation(client);
        if (location != null) {
            userLocation = LocationUtils.locationToLatLng(location);
        }

        String eventTravelTime = LocationUtils.getTravelTime(EventDetailActivity.this,
            userLocation, event.location);
        eventCard.travelTimeView.setVisibility(eventTravelTime == null ? View.GONE : View.VISIBLE);
        if (eventTravelTime != null) {
            eventCard.travelTimeView.setText(eventTravelTime);
        }
    }

    private void getGoogleApiClient() {
        if (client != null && client.isConnected()) {
            populateEventTravelTime();
            return;
        }

        client = new GoogleApiClient.Builder(this)
                    .addApi(AppIndex.APP_INDEX_API)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {
                            populateEventTravelTime();

                            Uri webUri = event.getEventDetailsURI();
                            AppIndex.AppIndexApi.view(client, EventDetailActivity.this,
                                    Utils.getAppUri(webUri), event.title, webUri, null);
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            // do nothing.
                        }
                    })
                    .build();
        client.connect();
    }
}
