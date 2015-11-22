package com.eventshigh.nearme.app.activity;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.network.EventRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.PhoneVerificationDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.Account.UserInfo;
import com.eventshigh.nearme.app.user.UserActionHelper;
import com.eventshigh.nearme.app.user.UserActionHelper.EventAction;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Date;

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
    private GoogleApiClient client;
    private boolean addToFavourite = false;


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
        toolbar.setTitle(R.string.loading);

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
                shareEvent(event, null);
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

        if (addToFavourite && event != null && !isFavourite(event)) {
            addFavourite(null);
        }
        addToFavourite = false;
    }


    /**********************************
     Callbacks, action handlers
     **********************************/

    public void save(View view) {
        addToFavourite = true;
        reportEventAction(event, "addToCalendar");

        new UserActionHelper(this).recordAction(EventAction.SAVE, event.id);
        addToCalendar(event, null);
    }

    public void openSourceSite(View view) {
        reportEventAction(event, "organizer", view.getId() == R.id.join_event ? "joinEvent" : "openSource");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.sourceUrl));
        startActivitySafe(intent);
    }

    public  void call(View view) {
        if (event.organizerPhone == null) {
            return;
        }
        Account account = new Account(this);
        UserInfo userInfo = account.getUserInfo();
        if (userInfo.phoneNo == null || userInfo.name == null) {
            PhoneVerificationDialog.show(this, R.string.ui_verify_phone, R.string.ui_phone_verify_plan);
            return;
        }

        addToFavourite = true;
        reportEventAction(event, "organizer", "call");
        new UserActionHelper(this).recordAction(EventAction.CALL, event.id);

        Intent intent = new Intent(Intent.ACTION_DIAL)
                .setData(Uri.parse("tel:" + (event.organizerPhone.split(",")[0])));
        startActivitySafe(intent);
    }

    public void openOrganizerLink(View view) {
        reportEventAction(event, "organizer", "openLink");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.organizerLink));
        startActivitySafe(intent);
    }

    public void openOrganizerWebsite(View view) {
        reportEventAction(event, "organizer", "openWebsite");

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.organizerWebsite));
        startActivitySafe(intent);
    }

    @SuppressWarnings("all")
    public void openBookingSite(View view) {
        Account account = new Account(this);
        UserInfo userInfo = account.getUserInfo();
        if (userInfo.phoneNo == null || userInfo.name == null) {
            PhoneVerificationDialog.show(this, R.string.ui_verify_phone, R.string.ui_phone_verify_book);
            return;
        }

        addToFavourite = true;
        reportEventAction(event, "bookTicket");
        new UserActionHelper(this).recordAction(EventAction.BOOK, event.id);

        final Uri.Builder bookingUriBuilder = Uri.parse(event.bookingUrl).buildUpon();
        if (event.bookingUrl.contains("ticketing.eventshigh.com")) {
            bookingUriBuilder.appendQueryParameter("did", Utils.getAndroidId(this));
            bookingUriBuilder.appendQueryParameter("name", userInfo.name);
            bookingUriBuilder.appendQueryParameter("mobile", userInfo.phoneNo);
        }

        CustomUrlActivity.launchCustomUrl(this, bookingUriBuilder.build(),
                getString(R.string.title_book));
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

    public void checkWithFriends(View view) {
        shareEvent(event, null);
    }

    public void playYouTube(View view) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + event.youtubeVideoId));
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Intent intent= new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://www.youtube.com/watch?v=" + event.youtubeVideoId));
            startActivity(intent);
        }
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

    private void populateView(final Event event) {
        this.event = event;

        // Report the Event View.
        new UserActionHelper(this).recordAction(EventAction.VIEW_EVENT, event.id);

        // Set Title.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(event.title);
        }

        // Populate event details.
        toolbar.setAlpha(0f);
        eventCard.populateView(event);
        findViewById(R.id.check_with_friends).setVisibility(View.VISIBLE);

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
            // do nothing.
        }
    }

    private class EventCard {
        private final ScrollView eventScrollView;

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

        private final View bookView;
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

        private EventCard() {
            eventScrollView = (ScrollView) findViewById(R.id.event_scroll_view);

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

            bookView = findViewById(R.id.book_ticket);
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
        }

        @SuppressLint("SetTextI18n")
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
            playYoutubeView.setVisibility(event.youtubeVideoId == null ? View.GONE : View.VISIBLE);

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
            bookView.setVisibility(event.bookingUrl != null ? View.VISIBLE : View.GONE);
            joinView.setVisibility(
                (bookView.getVisibility() != View.VISIBLE && event.sourceUrl != null &&
                    event.sourceUrl.contains("facebook.com/"))
                ? View.VISIBLE : View.GONE);

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
            tagsHeaderView.setVisibility(tagsView.getChildCount() > 0 ? View.GONE : View.VISIBLE);

            // Set description.
            descriptionHeaderView.setVisibility(event.description.isEmpty() ? View.GONE : View.VISIBLE);
            if (!event.description.isEmpty()) {
                CustomUrlActivity.setupWebView(descriptionView, EventDetailActivity.this, false);
                descriptionView.loadData(toHtmlNoFrame(event.description), "text/html; charset=UTF-8", null);
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

    private static String toHtmlNoFrame(String html) {
        return "<body>" + html.replaceAll("<iframe.*/iframe>", "") + "</body>";
    }
}
