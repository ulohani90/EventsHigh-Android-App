package com.eventshigh.nearme.app.activity;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnScrollChangedListener;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request.Priority;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.data.EventsContext;
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.network.EventRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.ui.RateAppDialog;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.utils.ZendeskUtils;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.zendesk.sdk.feedback.ui.ContactZendeskActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

/**
 * An activity representing a single Event detail screen. This activity can be called from deep
 * link or from Events{Grid,Maps}Activity. In both cases, event data is not available so
 * this activity fetches the event data and shows it using the EventDetailFragment.
 */
public class EventDetailActivity extends BaseActivity {
    public static final String EXTRA_EVENT_PARAM = EventDetailActivity.class.getSimpleName() + "_event";

    // Regex to check if description is plane text or html.
    private static final Pattern HTML_PATTERN = Pattern.compile(
            "<[A-Za-z].*</[A-Za-z]|<[A-Za-z].*/>");

    public static final String PACKAGE_NAME_FACEBOOK = "com.facebook.katana";
    public static final String PACKAGE_NAME_TWITTER = "com.twitter.android";
    public static final String PACKAGE_NAME_EMAIL = "com.google.android.gm";
    public static final String PACKAGE_NAME_WHATSAPP = "com.whatsapp";

    private Toolbar toolbar;
    private View topProgressBar;
    private EventCard eventCard;
    private LatLng userLocation = null;
    private Event event = null;
    private GoogleApiClient client;
    private boolean showRateAppDialog = false;  // TODO: save this in bundle and restore


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
                            Log.e(EventDetailActivity.class.getSimpleName(), volleyError.toString(), volleyError.getCause());
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
        private final FrameLayout callView;
        private final TextView priceView;
        private final TextView offerView;

        private final View tagsHeaderView;
        private final LinearLayout tagsView;
        private final View descriptionHeaderView;
        private final TextView descriptionView;
        private final TextView readMoreView;

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
            callView = (FrameLayout) findViewById(R.id.call);
            priceView = (TextView) findViewById(R.id.event_price);
            offerView = (TextView) findViewById(R.id.offer_text);

            tagsHeaderView = findViewById(R.id.tags_header);
            tagsView = (LinearLayout) findViewById(R.id.event_tags);
            descriptionHeaderView = findViewById(R.id.description_header);
            descriptionView = (TextView) findViewById(R.id.event_description);
            readMoreView = (TextView) findViewById(R.id.read_more);

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
                descriptionHeaderView.setVisibility(View.VISIBLE);
                String description;
                try {
                    description = new String(event.description.getBytes("ISO-8859-1"), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    description = event.description;
                    e.printStackTrace();
                }

                if (HTML_PATTERN.matcher(event.description).find()) {
                    descriptionView.setText(Html.fromHtml(description));
                    descriptionView.setMovementMethod(LinkMovementMethod.getInstance());
                    descriptionView.setTextIsSelectable(false);
                } else {
                    descriptionView.setText(description);
                    descriptionView.setTextIsSelectable(true);
                    Linkify.addLinks(descriptionView, Linkify.ALL);
                }

                readMoreView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        descriptionView.setMaxLines(Integer.MAX_VALUE);
                        readMoreView.setVisibility(View.GONE);
                    }
                });
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
            if (organizerInfoShown) {
                Utils.waitForViewVisible(descriptionView, new Runnable() {
                    @Override
                    public void run() {
                        if (descriptionView.getLineCount() > 8) {
                            descriptionView.setMaxLines(5);
                            readMoreView.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }

            // Share Buttons.
            findViewById(R.id.share_fb).setVisibility(isInstalled(PACKAGE_NAME_FACEBOOK) ? View.VISIBLE : View.GONE);
            findViewById(R.id.share_twitter).setVisibility(isInstalled(PACKAGE_NAME_TWITTER) ? View.VISIBLE : View.GONE);
            findViewById(R.id.share_email).setVisibility(isInstalled(PACKAGE_NAME_EMAIL) ? View.VISIBLE : View.GONE);
            findViewById(R.id.share_whatsapp).setVisibility(isInstalled(PACKAGE_NAME_WHATSAPP) ? View.VISIBLE : View.GONE);
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
