package com.eventshigh.nearme.app.activity;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventDescriptionSection;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.data.SocialFriend;
import com.eventshigh.nearme.app.data.stream.EhPrices;
import com.eventshigh.nearme.app.network.EventRequest;
import com.eventshigh.nearme.app.network.SocialActionsRequest;
import com.eventshigh.nearme.app.network.SocialActionsRequest.SocialActions;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest;
import com.eventshigh.nearme.app.network.SocialInvitationsRequest.SocialInvite;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.AskForContactsDialog;
import com.eventshigh.nearme.app.ui.InviteFriendsDialog;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.ui.RateAppDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Account.UserInfo;
import com.eventshigh.nearme.app.user.Preferences;
import com.eventshigh.nearme.app.user.UserActionHelper;
import com.eventshigh.nearme.app.user.UserActionHelper.EventAction;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.utils.ZendeskUtils;
import com.eventshigh.nearme.app.view.ContactListView;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.plus.PlusOneButton;
import com.google.android.gms.plus.PlusOneButton.OnPlusOneClickListener;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.zendesk.sdk.feedback.ui.ContactZendeskActivity;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import io.branch.referral.Branch;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;

/**
 * An activity representing a single Event detail screen. This activity can be called from deep
 * link or from Events{Grid,Maps}Activity. In both cases, event data is not available so
 * this activity fetches the event data and shows it using the EventDetailFragment.
 */
public class EventDetailActivity extends BaseActivity {
    public static final String EXTRA_EVENT_PARAM = EventDetailActivity.class.getSimpleName() + "_event";
    public static final String EXTRA_PLAN_ID_PARAM = EventDetailActivity.class.getSimpleName() + "_plan_id";

    private Toolbar toolbar;
    private View topProgressBar;
    private EventCard eventCard;

    private LatLng userLocation = null;
    private Event event = null;
    private Account account;
    private String planId = null;
    private GoogleApiClient client;
    private Action viewAction = null;
    private boolean showRateAppDialog = false;  // TODO: save this in bundle and restore
    private boolean showInviteDialog = false;
    private boolean addToFavourite = false;


    CollapsingToolbarLayout collapsingToolbar;


    /*****************************************
     * Activity lifecycle management utilities
     ***************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_event_detail);
        eventCard = new EventCard();
        topProgressBar = findViewById(R.id.top_progress_bar);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        setSupportActionBar(toolbar);
        toolbar.setTitle(R.string.loading);
        collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);

        // Account.
        account = new Account(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_event_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            if (event != null) {
                showRateAppDialog = true;
                //shareEvent(event, null);
                shareEventWithBranch(event, null, "Toolbar");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View getViewForSnackbar() {
        return toolbar;
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
                            VolleyHelper.log(EventDetailActivity.this, volleyError);
                            finish();
                        }
                    });
        }

    }

    @Override
    protected void onStop() {
        super.onStop();

        if (client != null && client.isConnected()) {
            if (viewAction != null) {
                AppIndex.AppIndexApi.end(client, viewAction);
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
        } else if (showInviteDialog) {
            InviteFriendsDialog.show(this, event, planId);
            showInviteDialog = false;
        }

        if (addToFavourite && event != null && !isFavourite(event)) {
            addFavourite(null);
        }
        addToFavourite = false;

        final String url = event == null ?
                getIntent().getData().buildUpon().appendQueryParameter("src", "ehm_gp1").toString() :
                event.getEventShareURI("gp1").toString();

        PlusOneButton plusOneButton = (PlusOneButton) findViewById(R.id.plus_one_button);
        plusOneButton.initialize(url, PLUS_ONE_REQUEST_CODE);
        plusOneButton.setOnPlusOneClickListener(new OnPlusOneClickListener() {
            @Override
            public void onPlusOneClick(Intent intent) {
                reportActionToAnalytics("plusOne", url);
                startActivityForResult(intent, PLUS_ONE_REQUEST_CODE);
            }
        });


           /* if (Branch.isAutoDeepLinkLaunch(this)) {
                try {
                    String eventId = Branch.getInstance().getLatestReferringParams().getString("event_id");
                    City city = City.BANGALORE;
                    EventRequest.submit(this, EventsHighEndpoints.getEventDetailsURI(city,eventId), Priority.IMMEDIATE, mEventListener,
                            new ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError volleyError) {
                                    Toast.makeText(EventDetailActivity.this, R.string.failed_load,
                                            Toast.LENGTH_SHORT).show();
                                    VolleyHelper.log(EventDetailActivity.this, volleyError);
                                    finish();
                                }
                            });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
*/


        // }
    }


    /**********************************
     * Callbacks, action handlers
     **********************************/

    public void save(View view) {
        showRateAppDialog = true;
        addToFavourite = true;
        reportEventAction(event, "addToCalendar");

        new UserActionHelper(this).recordAction(EventAction.SAVE, event.id);
        addToCalendar(event, null);
    }

    public void openSourceSite(View view) {
        showInviteDialog = true;
        reportEventAction(event, "organizer", view.getId() == R.id.join_event ? "joinEvent" : "openSource");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.sourceUrl));
        startActivitySafe(intent);
    }

    public void call(View view) {
        if (event.organizerPhone == null) {
            return;
        }
        Account account = new Account(this);
        UserInfo userInfo = account.getUserInfo();
        if (userInfo.phoneNo == null || userInfo.name == null) {
            PhoneVerificationDialog.show(this, R.string.ui_verify_phone, R.string.ui_phone_verify_plan);
            return;
        }

        showInviteDialog = true;
        addToFavourite = true;
        reportEventAction(event, "organizer", "call");
        new UserActionHelper(this).recordAction(EventAction.CALL, event.id);

        Intent intent = new Intent(Intent.ACTION_DIAL)
                .setData(Uri.parse("tel:" + (event.organizerPhone.split(",")[0])));
        startActivitySafe(intent);
    }

    public void openOrganizerLink(View view) {
        if (event.organizerLink != null) {

            showInviteDialog = true;
            reportEventAction(event, "organizer", "openLink");

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.organizerLink));
            startActivitySafe(intent);
        }
    }

    public void openOrganizerWebsite(View view) {
        if (event.organizerWebsite != null) {
            showInviteDialog = true;
            reportEventAction(event, "organizer", "openWebsite");

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.organizerWebsite));
            startActivitySafe(intent);
        }
    }

    @SuppressWarnings("all")
    public void openBookingSite(View view) {
        Account account = new Account(this);
        UserInfo userInfo = account.getUserInfo();
        if (userInfo.phoneNo == null || userInfo.name == null) {
            PhoneVerificationDialog.show(this, R.string.ui_verify_phone, R.string.ui_phone_verify_book);
            return;
        }

        showRateAppDialog = true;
        addToFavourite = true;
        reportEventAction(event, "bookTicket");
        new UserActionHelper(this).recordAction(EventAction.BOOK, event.id);

        final Uri.Builder bookingUriBuilder = Uri.parse(event.bookingUrl).buildUpon();
        if (event.bookingUrl.contains("ticketing.eventshigh.com")) {
           /* bookingUriBuilder.appendQueryParameter("did", Utils.getAndroidId(this));
            bookingUriBuilder.appendQueryParameter("name", userInfo.name);
            bookingUriBuilder.appendQueryParameter("mobile", userInfo.phoneNo);
            bookingUriBuilder.appendQueryParameter("src", "eh-android");*/
            Intent intent = new Intent(this, EventBookingDetailActivity.class);
            intent.putExtra("event", event);
            startActivity(intent);
        } else {
            try {
                CustomUrlActivity.launchCustomUrl(this, bookingUriBuilder.build(),
                        getString(R.string.title_book));
            } catch (Exception e) {
                Crashlytics.getInstance().core.logException(e);
                showMessage(R.string.retry);
            }
        }

    }

    public void imagePreview(View view) {
        if (event == null || (event != null && event.imgUrl == null)) {
            return;
        }

        reportEventAction(event, "imagePreview");

        final Dialog nagDialog = new Dialog(this,
                android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
        nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        nagDialog.setCancelable(true);
        nagDialog.setContentView(R.layout.dialog_image_preview);

        ImageViewTouch preview = (ImageViewTouch) nagDialog.findViewById(R.id.image_preview);
        Glide.with(this).load(event.imgUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .crossFade()
                .into(preview);

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
        if (v != null) {
            reportEventAction(event, "addFavourite");

            Account account = new Account(this);
            if (!account.getUserInfo().isVerified) {
                PhoneVerificationDialog.show(this, R.string.ui_verify_phone, R.string.ui_phone_verify_pa);
            }
        }

        EventsMarkerManager.Editor eventsMarkerEditor =
                EventsMarkerManager.getInstance(this).getEditor();
        eventsMarkerEditor.recordEventMark(event, EventMark.FAVOURITE);
        eventCard.setFavouriteView(true);
        eventsMarkerEditor.close();
    }

    public void showDirections(View view) {
        addToFavourite = true;
        reportEventAction(event, "showDirections");

        Intent intent = event.getShowDirectionsOnMapIntent();
        if (intent == null) {
            reportActionToAnalytics("skipDirectionsNoLocation");
            showMessage(R.string.failed_event_location);
            return;
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open maps.
            Crashlytics.getInstance().core.logException(e);
            showMessage(R.string.no_map_app);
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
        Account account = new Account(this);
        UserInfo userInfo = account.getUserInfo();
        if (userInfo.phoneNo == null || userInfo.name == null) {
            PhoneVerificationDialog.show(this, R.string.ui_verify_phone, R.string.ui_phone_verify_plan);
            return;
        }

        Preferences preferences = Preferences.getInstance(this);
        if (!preferences.canUploadContacts()) {
            if (AskForContactsDialog.checkIfToShow(this, preferences)) {
                return;
            }
        }


        reportEventAction(event, "ama");
        ZendeskUtils.initZendesk(this);
        ZendeskUtils.setEventFeedbackConfiguration(this, event);
        Intent feedbackIntent = new Intent(this, ContactZendeskActivity.class);
        startActivity(feedbackIntent);
    }

    public void checkWithFriends(View view) {
        reportEventAction(event, "checkWithFriends");

        UserInfo userInfo = new Account(this).getUserInfo();
        if (userInfo.name == null || userInfo.phoneNo == null) {
            PhoneVerificationDialog.show(this, R.string.ui_verify_phone, R.string.ui_phone_verify_plan);
            return;
        }

        Intent intent = new Intent(this, PlanActivity.class);
        intent.putExtra(EXTRA_EVENT_PARAM, event);
        if (planId != null) {
            intent.putExtra(EXTRA_PLAN_ID_PARAM, planId);
        }
        startActivity(intent);
    }

    public void whatsapp(View view) {
        shareEventWithBranch(event, PACKAGE_NAME_WHATSAPP, " bottombar");
    }

    public void playYouTube(View view) {
        reportEventAction(event, "playYoutube", event.youtubeVideoId);
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + event.youtubeVideoId));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://www.youtube.com/watch?v=" + event.youtubeVideoId));
                startActivity(intent);
            } catch (Exception e) {
                Crashlytics.getInstance().core.logException(e);
            }
        }
    }

    /**********************************
     * Helper methods
     **********************************/

    private void startActivitySafe(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open url. ignore.
            Crashlytics.getInstance().core.logException(e);
        }
    }

    private void populateView(final Event event) {
        this.event = event;

        // Report the Event View.
        new UserActionHelper(this).recordAction(EventAction.VIEW_EVENT, event.id);

        // Set Title.
        if (collapsingToolbar != null) {
            collapsingToolbar.setTitle(event.title);
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }

        // Populate event details.
        eventCard.populateView(event);
        findViewById(R.id.check_with_friends).setVisibility(View.VISIBLE);
        findViewById(R.id.share_whatsapp).setVisibility(View.VISIBLE);
        // Connect to Google API client to notify the view.
        getGoogleApiClient();

        // Show social data.
        SocialActionsRequest.submit(this, Priority.LOW, this, false,
                new Listener<SocialActions>() {
                    @Override
                    public void onResponse(SocialActions socialActions, boolean isIntermediate) {
                        Set<SocialFriend> likedBy = socialActions.eventFavourites.get(event.id);
                        reportActionToAnalytics("showSocialInfo", "likes",
                                likedBy == null ? 0 : likedBy.size());
                        ((ContactListView) findViewById(R.id.followed_by)).setFollowers(
                                EventDetailActivity.this, likedBy);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        VolleyHelper.log(EventDetailActivity.this, volleyError);
                    }
                }
        );
        SocialInvitationsRequest.submit(this, Priority.LOW, this, false,
                new Listener<SocialInvitationsRequest.CommonInviteObject>() {
                    @Override
                    public void onResponse(SocialInvitationsRequest.CommonInviteObject commonInviteObject, boolean isIntermediate) {
                        SocialInvite invite = commonInviteObject.getInvites().get(event.id);
                        if (invite == null || invite.getInvitedBy() == null) {
                            reportActionToAnalytics("showSocialInfo", "invitedBy", 0);
                            return;
                        }

                        planId = invite.getPlanId();
                        Set<SocialFriend> allInvitedBy = invite.getAllInvitedBy();
                        Set<SocialFriend> allParticipants = invite.getAllParticipants();
                        reportActionToAnalytics("showSocialInfo", "invitedBy", allInvitedBy.size());

                        String prefix = allInvitedBy.size() == 1 ?
                                allInvitedBy.iterator().next().getName() : "";
                        String suffix = allParticipants.size() < 3 ? "" : "and " + (allParticipants.size() - 2) + " more friends";

                        ContactListView invitedByView = (ContactListView) findViewById(R.id.invited_by);
                        invitedByView.setVisibility(View.VISIBLE);
                        invitedByView.setText(prefix + " has invited you " + suffix);
                        invitedByView.setFollowers(EventDetailActivity.this, allInvitedBy, allParticipants);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        VolleyHelper.log(EventDetailActivity.this, volleyError);
                    }
                }
        );
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
            // do nothing.
            Crashlytics.getInstance().core.logException(e);
        }
    }

    private class EventCard {
        private final NestedScrollView eventScrollView;

        private final ImageView bgView;
        private final View recommendedView;
        private final View playYoutubeView;

        private final TextView titleView;
        private final TextView fromView;
        private final TextView statsView;

        private final View favouriteView;
        private final View favouritedView;

        private final TextView venueView;
        private final TextView addressView;
        private final TextView travelTimeView;

        private final View timeGroupView;
        private final View eventTimeFirstView;
        private final TextView timeView;
        private final TextView timeDetailView;
        private final TextView alsoOnView;
        private final HorizontalScrollView futureTimesViewGroup;
        private final LinearLayout futureTimesView;

        private final TextView bookView;
        private final View callView;
        private final View joinView;
        private final TextView priceView;

        private final View tagsHeaderView;
        private final LinearLayout tagsView;
        private final View descriptionHeaderView;
        private final WebView descriptionView;

        private final View performerHeaderView;
        private final LinearLayout performersView;

        private final View organizerHeader;
        private final View organizerNameRow;
        private final TextView organizerNameView;
        private final TextView organizerLinkView;
        private final View organizerEmailRow;
        private final TextView organizerEmailView;
        private final View organizerPhoneRow;
        private final TextView organizerPhoneView;
        private final View organizerWebsiteRow;
        private final TextView organizerWebsiteView;
        FrameLayout frameParent;

        private EventCard() {
            eventScrollView = (NestedScrollView) findViewById(R.id.event_scroll_view);

            bgView = (ImageView) findViewById(R.id.event_bg);
            playYoutubeView = findViewById(R.id.play_youtube);
            recommendedView = findViewById(R.id.eh_recommends);

            favouriteView = findViewById(R.id.action_favourite);
            favouritedView = findViewById(R.id.action_favourited);

            titleView = (TextView) findViewById(R.id.event_title);
            fromView = (TextView) findViewById(R.id.event_from);
            statsView = (TextView) findViewById(R.id.event_stats);

            venueView = (TextView) findViewById(R.id.event_venue);
            addressView = (TextView) findViewById(R.id.event_address);
            travelTimeView = (TextView) findViewById(R.id.event_travel_time);

            timeGroupView = findViewById(R.id.event_time_group);
            eventTimeFirstView = findViewById(R.id.event_time_first);
            timeView = (TextView) findViewById(R.id.event_time);
            timeDetailView = (TextView) findViewById(R.id.event_time_details);
            alsoOnView = (TextView) findViewById(R.id.also_on);
            futureTimesViewGroup = (HorizontalScrollView) findViewById(R.id.event_future_times_hs);
            futureTimesView = (LinearLayout) findViewById(R.id.event_future_times);

            bookView = (TextView) findViewById(R.id.book_ticket);
            callView = findViewById(R.id.call);
            joinView = findViewById(R.id.join_event);
            priceView = (TextView) findViewById(R.id.event_price);

            tagsHeaderView = findViewById(R.id.tags_header);
            tagsView = (LinearLayout) findViewById(R.id.event_tags);
            descriptionHeaderView = findViewById(R.id.description_header);
            descriptionView = (WebView) findViewById(R.id.event_description);

            performerHeaderView = findViewById(R.id.performer_header);
            performersView = (LinearLayout) findViewById(R.id.performers);

            organizerHeader = findViewById(R.id.organizer_header);
            organizerNameRow = findViewById(R.id.organizer_name_row);
            organizerNameView = (TextView) findViewById(R.id.organizer_name);
            organizerLinkView = (TextView) findViewById(R.id.organizer_link);
            organizerEmailRow = findViewById(R.id.organizer_email_row);
            organizerEmailView = (TextView) findViewById(R.id.organizer_email);
            organizerPhoneRow = findViewById(R.id.organizer_phone_row);
            organizerPhoneView = (TextView) findViewById(R.id.organizer_phone);
            organizerWebsiteRow = findViewById(R.id.organizer_website_row);
            organizerWebsiteView = (TextView) findViewById(R.id.organizer_website);

            // Set Image view dimensions.
            final DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            ViewGroup.LayoutParams params = bgView.getLayoutParams();
            params.height = 9 * metrics.widthPixels / 16;
            bgView.setLayoutParams(params);
            frameParent = (FrameLayout) findViewById(R.id.frame_parent);
            ViewGroup.LayoutParams lp = frameParent.getLayoutParams();
            lp.height = 9 * metrics.widthPixels / 16;
            frameParent.setLayoutParams(lp);
        }

        @SuppressLint("SetTextI18n")
        private void populateView(final Event event) {
            eventScrollView.getViewTreeObserver().addOnScrollChangedListener(
                    new OnScrollChangedListener() {
                        @Override
                        public void onScrollChanged() {
                            // setScroll(eventScrollView.getScrollY());
                        }
                    });
            eventScrollView.setVisibility(View.VISIBLE);
            topProgressBar.setVisibility(View.GONE);
            frameParent.setVisibility(View.VISIBLE);
            // Image
            Glide.with(EventDetailActivity.this).load(event.imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.eh_default_event).crossFade().centerCrop()
                    .into(bgView);

            // Set title
            titleView.setText(event.title);

            // Show stats
            ((View) statsView.getParent()).setVisibility(event.numViews > 5 ? View.VISIBLE : View.GONE);
            statsView.setText("" + event.numViews + " views");

            // Add attribution.
            String sourceHost = event.sourceUrl == null ? null : Uri.parse(event.sourceUrl).getHost();
            if (sourceHost == null) {
                fromView.setVisibility(View.GONE);
            } else {
                String eventFrom = String.format(getString(R.string.event_detail_from), sourceHost);
                fromView.setText(eventFrom);
            }

            // Set EH recommendation and favourite views.
            recommendedView.setVisibility(event.ehRecommended ? View.VISIBLE : View.GONE);
            setFavouriteView(EventsMarkerManager.getInstance(EventDetailActivity.this)
                    .isFavourite(event.id));

            // Set Youtube play button.
            playYoutubeView.setVisibility((event.youtubeVideoId != null && event.youtubeVideoId.length() > 0) ? View.VISIBLE : View.GONE);

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
                    timeDetailView.setText(MessageFormat.format(
                            getString(R.string.event_time_details), numDays));
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

            // Set action buttons.
            findViewById(R.id.action_button_group).setVisibility(View.VISIBLE);
            callView.setVisibility(event.organizerPhone != null ? View.VISIBLE : View.GONE);
            bookView.setVisibility(event.bookingUrl != null && event.bookingUrl.length() > 0 ? View.VISIBLE : View.GONE);
            if (event.bookingText != null) {
                bookView.setText(event.bookingText);
            }
            joinView.setVisibility(
                    (bookView.getVisibility() != View.VISIBLE && event.sourceUrl != null &&
                            event.sourceUrl.contains("facebook.com/"))
                            ? View.VISIBLE : View.GONE);

            // Show price.
            findViewById(R.id.price_row).setVisibility(View.VISIBLE);
            LinearLayout ehPriceContainer = (LinearLayout) findViewById(R.id.eh_price_container);
            if (event.ehPrices.size() > 0) {
                ehPriceContainer.removeAllViews();
                ehPriceContainer.setVisibility(View.VISIBLE);
                for (EhPrices ehPrice : event.ehPrices) {
                    View view = LayoutInflater.from(EventDetailActivity.this).inflate(R.layout.event_detail_price_layout, ehPriceContainer, false);
                    if (ehPrice.discountValue > 0) {
                        ((TextView) view.findViewById(R.id.event_price)).setText(ehPrice.name + " - " + ehPrice.currency + " " + ehPrice.discountValue);
                    } else {
                        ((TextView) view.findViewById(R.id.event_price)).setText(ehPrice.name + " - " + ehPrice.currency + " " + ehPrice.value);
                    }

                    if (ehPrice.note != null && ehPrice.note.length() > 0) {
                        TextView note = (TextView) view.findViewById(R.id.event_note);
                        note.setVisibility(View.VISIBLE);
                        note.setText("( " + ehPrice.note + " )");
                    } else {
                        ((TextView) view.findViewById(R.id.event_note)).setVisibility(View.GONE);
                    }
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                    ehPriceContainer.addView(view, lp);
                }
                priceView.setVisibility(View.GONE);
            } else {
                ehPriceContainer.setVisibility(View.GONE);
                priceView.setVisibility(View.VISIBLE);
                String priceString = event.getPriceString(event.minPrice, event.maxPrice, event.currency);
                priceView.setText(priceString == null ? getString(R.string.no_price) : priceString);

            }


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
            if (event.isCleanVenue) {
                addTagView(tagsView, event.venue, "venueAsTag");
            }
            if (event.locality != null) {
                addTagView(tagsView, event.locality, "localityAsTag");
            }
            if (event.tags.length > 0) {
                for (final String tag : event.tags) {
                    addTagView(tagsView, tag, "tagClick");
                }
            }
            tagsHeaderView.setVisibility(tagsView.getChildCount() > 0 ? View.VISIBLE : View.GONE);

            // Set description.
            descriptionHeaderView.setVisibility(event.description.isEmpty() ? View.GONE : View.VISIBLE);
            if (!event.description.isEmpty()) {
                CustomUrlActivity.setupWebView(descriptionView, EventDetailActivity.this, false);
                descriptionView.loadData(toHtmlNoFrame(event.description), "text/html; charset=UTF-8", null);
            }

            // Organizer Info.
            boolean organizerInfoShown = false;
            organizerNameRow.setVisibility(event.organizerName == null ? View.GONE : View.VISIBLE);
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
                        showInviteDialog = true;
                        reportEventAction(event, "organizer", "email");
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

            // Event Description Section.
            LinearLayout eventContainer = (LinearLayout) findViewById(R.id.event_container);
            for (EventDescriptionSection descriptionSection : event.descriptionSections) {
                View descriptionSectionView = getLayoutInflater().inflate(R.layout.view_description_section, eventContainer, false);
                ((TextView) descriptionSectionView.findViewById(R.id.description_header)).setText(descriptionSection.name);
                WebView descriptionView = (WebView) descriptionSectionView.findViewById(R.id.event_description);
                CustomUrlActivity.setupWebView(descriptionView, EventDetailActivity.this, false);
                descriptionView.loadData(toHtmlNoFrame(descriptionSection.description), "text/html; charset=UTF-8", null);
                eventContainer.addView(descriptionSectionView);
            }


            //Adding youtube view
            if (event.youtubeVideoId != null && event.youtubeVideoId.length() > 0) {
                LinearLayout linearLayout = (LinearLayout) findViewById(R.id.youtube_fragment);
                LinearLayout ll = new LinearLayout(EventDetailActivity.this);
                ll.setId(View.generateViewId());
                YouTubePlayerSupportFragment youTubePlayerSupportFragment = YouTubePlayerSupportFragment.newInstance();
                youTubePlayerSupportFragment.initialize(Utils.YOUTUBE_API_KEY, new YouTubePlayer.OnInitializedListener() {
                    @Override
                    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
                        // youTubePlayer.loadVideo(event.youtubeVideoId);
                        youTubePlayer.cueVideo(event.youtubeVideoId);
                        youTubePlayer.setShowFullscreenButton(false);
                    }

                    @Override
                    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

                        (findViewById(R.id.youtube_fragment)).setVisibility(View.GONE);

                    }
                });
                getSupportFragmentManager().beginTransaction().add(ll.getId(), youTubePlayerSupportFragment).commit();
                linearLayout.addView(ll);
            } else {
                (findViewById(R.id.youtube_fragment)).setVisibility(View.GONE);
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

                    if (!account.getUserInfo().isVerified) {
                        PhoneVerificationDialog.show(EventDetailActivity.this,
                                R.string.ui_verify_phone, R.string.ui_phone_verify_pa);
                    }
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

    private void populateEventTravelTime() {
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
    }

    private void getGoogleApiClient() {
        if (client != null && client.isConnected()) {
            populateEventTravelTime();
            return;
        }

        client = new GoogleApiClient.Builder(this)
                .addApi(AppIndex.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        populateEventTravelTime();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        // do nothing.
                    }
                })
                .build();
        client.connect();
        Uri webUri = event.getEventDetailsURI();
        viewAction = new Action.Builder(Action.TYPE_VIEW)
                .setObject(new Thing.Builder()
                        .setName(event.title)
                        .setId(webUri.toString())
                        .setUrl(Utils.getAppUri(webUri))
                        .build())
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    private static String toHtmlNoFrame(String html) {
        return "<body>" + html.replaceAll("<iframe.*/iframe>", "") + "</body>";
    }
}
