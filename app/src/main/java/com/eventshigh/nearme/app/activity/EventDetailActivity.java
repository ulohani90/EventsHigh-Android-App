package com.eventshigh.nearme.app.activity;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
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
import com.eventshigh.nearme.app.data.EventsMarkerManager.Editor;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.network.EventRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.utils.AlarmUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.text.MessageFormat;
import java.util.Date;
import java.util.regex.Pattern;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

/**
 * An activity representing a single Event detail screen. This activity can be called from deep
 * link or from Events{Grid,Maps}Activity. In both cases, event data is not available so
 * this activity fetches the event data and shows it using the EventDetailFragment.
 */
public class EventDetailActivity extends BaseActivity {
    // Regex to check if description is plane text or html.
    private static final Pattern HTML_PATTERN = Pattern.compile(
            "<[A-Za-z].*</[A-Za-z]|<[A-Za-z].*/>");

    private Toolbar toolbar;
    private View topProgressBar;
    private EventCard eventCard;
    private Event event = null;
    private LatLng userLocation = null;
    private boolean hasSetUserLocation = false;
    private GoogleApiClient client;
    private Editor eventsMarkerEditor;


    /**********************************
     Activity lifecycle management utilities
     **********************************/

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
            reportActionToAnalytics("openNotification", getIntent().getDataString());
        }

        findViewById(R.id.event_container).setMinimumHeight(
                (int) (1.33 * getResources().getDisplayMetrics().heightPixels));

        // Get the event from Intent.
        EventRequest.submit(this, getIntent().getData(), Priority.IMMEDIATE, mEventListener,
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Toast.makeText(EventDetailActivity.this, R.string.failed_load, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

        // Setup GoogleApiClient.
        client = new GoogleApiClient.Builder(EventDetailActivity.this)
                .addApi(AppIndex.APP_INDEX_API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Location location = LocationServices.FusedLocationApi.getLastLocation(client);
                        if (location != null) {
                            userLocation = LocationUtils.locationToLatLng(location);
                        }
                        hasSetUserLocation = true;
                        populateView();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        // do nothing.
                    }
                })
                .addOnConnectionFailedListener(new OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        hasSetUserLocation = true;
                        populateView();
                    }
                })
                .build();
        client.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (client != null && client.isConnected()) {
            Uri webUri = event.getEventDetailsURI();
            AppIndex.AppIndexApi.viewEnd(client, this, Utils.getAppUri(webUri));
            client.disconnect();
        }

        if (eventsMarkerEditor != null) {
            eventsMarkerEditor.close();
        }
    }

    private void populateView() {
        if (event != null && hasSetUserLocation) {
            if (client != null && client.isConnected()) {
                Uri webUri = event.getEventDetailsURI();
                AppIndex.AppIndexApi.view(client, EventDetailActivity.this, Utils.getAppUri(webUri),
                        event.title, webUri, null);
            }
            eventCard.populateView(event);
        }
    }

    private void setScroll(int scrollValue) {
        float opacity = Math.min(1.0f, scrollValue * 3f / getResources().getDisplayMetrics().heightPixels);
        toolbar.setAlpha(opacity);
    }

    private Listener<Event> mEventListener = new Listener<Event>() {
        @Override
        public void onResponse(final Event event, boolean isIntermediate) {
            eventsMarkerEditor =  EventsMarkerManager.getInstance(EventDetailActivity.this).getEditor();
            toolbar.setTitle(event.title);
            if (event.numPeopleInterested <= 0) {
                toolbar.setSubtitle("");
            } else {
                String text = getResources().getQuantityString(R.plurals.people_interested,
                        event.numPeopleInterested, event.numPeopleInterested);
                toolbar.setSubtitle(text);
            }
            toolbar.setAlpha(0f);

            EventDetailActivity.this.event = event;
            populateView();
        }
    };

    /**********************************
     Callbacks, action handlers
     **********************************/

    private void openSourceSite() {
        reportEventAction(event, "openSource");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.sourceUrl));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open url. ignore.
        }
    }

    private void call() {
        reportEventAction(event, "callOrganizer");
        Intent intent = new Intent(Intent.ACTION_DIAL)
                .setData(Uri.parse("tel:" + event.organizerPhone));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to call. ignore.
        }
    }

    private void openOrganizerWebsite() {
        reportEventAction(event, "openOrganizerWebsite");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.organizerWebsite));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open url. ignore.
        }
    }

    private void openBookingSite() {
        reportEventAction(event, "bookTicket");
        Intent intent = new Intent(this, CustomUrlActivity.class);
        intent.setData(Uri.parse(event.bookingUrl));
        intent.putExtra(CustomUrlActivity.EXTRA_TITLE_KEY, getString(R.string.title_book));
        startActivity(intent);
    }

    private void openOfferSite() {
        reportEventAction(event, "openOffer");
        IntentUtils.processContestViewIntent(this,
                Uri.parse("http://www.eventshigh.com/get_event_contest/" + event.id),
                event.offerTitle);
    }


    /**********************************
     Helper class to hold all UI elements.
     **********************************/

    private class EventCard {
        private final ScrollView eventScrollView;
        private final View shareContentsView;

        private final NetworkImageView bgView;
        private final ImageView recommendedImageView;
        private final FrameLayout favouriteParent;
        private final TextView favouriteView;
        private final TextView favouritedView;

        private final TextView titleView;
        private final TextView fromView;

        private final RelativeLayout venueGroupView;
        private final TextView venueView;
        private final TextView addressView;
        private final TextView travelTimeView;
        private final View naviagationView;

        private final LinearLayout timeGroupView;
        private final RelativeLayout eventTimeFirstView;
        private final TextView timeView;
        private final TextView timeDetailView;
        private final TextView alsoOnView;
        private final HorizontalScrollView futureTimesViewGroup;
        private final LinearLayout futureTimesView;

        private final FrameLayout bookView;
        private final FrameLayout callView;
        private final FrameLayout shareView;
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
        private final LinearLayout organizerPhoneRow;
        private final TextView organizerPhoneView;
        private final LinearLayout organizerWebsiteRow;
        private final TextView organizerWebsiteView;

        private final View ama;

        private EventCard() {
            eventScrollView = (ScrollView) findViewById(R.id.event_scroll_view);
            shareContentsView = findViewById(R.id.share_view);

            recommendedImageView = (ImageView) findViewById(R.id.eh_recommend_banner);
            bgView = (NetworkImageView) findViewById(R.id.event_bg);
            favouriteView = (TextView) findViewById(R.id.action_favourite);
            favouritedView = (TextView) findViewById(R.id.action_favourited);
            favouriteParent = (FrameLayout) findViewById(R.id.action_favourite_parent);

            titleView = (TextView) findViewById(R.id.event_title);
            fromView = (TextView) findViewById(R.id.event_from);

            venueGroupView = (RelativeLayout) findViewById(R.id.event_venue_group);
            venueView = (TextView) findViewById(R.id.event_venue);
            addressView = (TextView) findViewById(R.id.event_address);
            travelTimeView = (TextView) findViewById(R.id.event_travel_time);
            naviagationView = findViewById(R.id.navigate_icon);

            timeGroupView = (LinearLayout) findViewById(R.id.event_time_group);
            eventTimeFirstView = (RelativeLayout) findViewById(R.id.event_time_first);
            timeView = (TextView) findViewById(R.id.event_time);
            timeDetailView = (TextView) findViewById(R.id.event_time_details);
            alsoOnView = (TextView) findViewById(R.id.also_on);
            futureTimesViewGroup = (HorizontalScrollView) findViewById(R.id.event_future_times_hs);
            futureTimesView = (LinearLayout) findViewById(R.id.event_future_times);

            bookView = (FrameLayout) findViewById(R.id.book_ticket);
            callView = (FrameLayout) findViewById(R.id.call);
            shareView = (FrameLayout) findViewById(R.id.share);
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
            organizerPhoneRow = (LinearLayout) findViewById(R.id.organizer_phone_row);
            organizerPhoneView = (TextView) findViewById(R.id.organizer_phone);
            organizerWebsiteRow = (LinearLayout) findViewById(R.id.organizer_website_row);
            organizerWebsiteView = (TextView) findViewById(R.id.organizer_website);

            ama =  findViewById(R.id.ama);
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

            // Set Image
            final DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            ViewGroup.LayoutParams params = bgView.getLayoutParams();
            params.height = (int) (0.3 * metrics.heightPixels);
            bgView.setLayoutParams(params);

            bgView.setDefaultImageResId(R.drawable.eh_default_event_detail);
            bgView.setErrorImageResId(R.drawable.eh_default_event_detail);
            bgView.setImageUrl(event.imgUrl, VolleyHelper.getImageLoader(EventDetailActivity.this));
            bgView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (event.imgUrl == null) {
                        return;
                    }
                    reportEventAction(event, "imagePreview");
                    final Dialog nagDialog = new Dialog(EventDetailActivity.this,
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
            });

            // Set EH recommendation and favourite views.
            recommendedImageView.setVisibility(event.ehRecommended ? View.VISIBLE : View.GONE);
            setFavouriteView(eventsMarkerEditor.getEventsMarkerManager().getEventMark(event.id));
            favouriteParent.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isFavourite = eventsMarkerEditor.getEventsMarkerManager().isFavourite(event.id);
                    if (isFavourite) {
                        reportEventAction(event, "removeFavourite");
                        eventsMarkerEditor.recordEventMark(event, null);
                        setFavouriteView(null);
                        AlarmUtils.cancelEventAlarm(EventDetailActivity.this, event);
                    } else {
                        reportEventAction(event, "addFavourite");
                        eventsMarkerEditor.recordEventMark(event, EventMark.FAVOURITE);
                        setFavouriteView(EventMark.FAVOURITE);
                        AlarmUtils.setEventAlarm(EventDetailActivity.this, event);
                    }
                }
            });

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
                fromView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openSourceSite();
                    }
                });
            }

            // Set Venue and address.
            venueView.setText(event.getShortAddress());
            addressView.setText(event.getFullAddress());
            String eventTravelTime = LocationUtils.getTravelTime(EventDetailActivity.this,
                    userLocation, event.location);
            if (eventTravelTime != null) {
                travelTimeView.setText(eventTravelTime);
                travelTimeView.setVisibility(View.VISIBLE);
            } else {
                travelTimeView.setVisibility(View.GONE);
            }
            venueGroupView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (event.venue != null) {
                        reportEventAction(event, "seeVenue", event.venue);
                        startActivity(new Intent(EventDetailActivity.this, LaunchActivity.class)
                            .putExtra(IntentUtils.EXTRA_EVENT_CONTEXT, new EventsContext(null, event.venue)));
                    } else {
                        showDirections(event);
                    }
                }
            });
            naviagationView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDirections(event);
                }
            });

            // Set action buttons.
            if (event.bookingUrl == null) {
                bookView.setVisibility(View.GONE);
            } else {
                bookView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openBookingSite();
                    }
                });
            }

            if (event.organizerPhone == null) {
                callView.setVisibility(View.GONE);
            } else {
                callView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        call();
                    }
                });
            }

            shareView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareEvent(shareContentsView, event);
                }
            });

            // Set time.
            EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
            if (eventTime == null) {
                timeGroupView.setVisibility(View.GONE);
            } else {
                timeView.setText(eventTime.toString());
                timeGroupView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addToCalendar(event, null);
                    }
                });

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
                                R.layout.event_time, futureTimesView, false);
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
            if (event.offerTitle != null) {
                offerView.setVisibility(View.VISIBLE);
                offerView.setText(event.offerTitle);
                offerView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openOfferSite();
                    }
                });
            } else {
                offerView.setVisibility(View.GONE);
            }

            // Show performers if any.
            performersView.removeAllViews();
            if (event.performers.length == 0) {
                performerHeaderView.setVisibility(View.GONE);
            } else {
                performerHeaderView.setVisibility(View.VISIBLE);
                for (final String performer : event.performers) {
                    addTagView(performersView, performer, "performerClick");
                }
            }

            // Set description.
            if (event.description.isEmpty()) {
                descriptionHeaderView.setVisibility(View.GONE);
            } else {
                descriptionHeaderView.setVisibility(View.VISIBLE);
                if (HTML_PATTERN.matcher(event.description).find()) {
                    descriptionView.setText(Html.fromHtml(event.description));
                    descriptionView.setMovementMethod(LinkMovementMethod.getInstance());
                    descriptionView.setTextIsSelectable(false);
                } else {
                    descriptionView.setText(event.description);
                    descriptionView.setTextIsSelectable(true);
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
            if (event.organizerName == null) {
                organizerNameRow.setVisibility(View.GONE);
            } else {
                organizerInfoShown = true;
                organizerNameView.setText(event.organizerName);
            }

            if (event.organizerPhone == null) {
                organizerPhoneRow.setVisibility(View.GONE);
            } else {
                organizerInfoShown = true;
                organizerPhoneView.setText(event.organizerPhone);
                organizerPhoneView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        call();
                    }
                });
            }

            if (event.organizerWebsite == null) {
                organizerWebsiteRow.setVisibility(View.GONE);
            } else {
                organizerInfoShown = true;
                organizerWebsiteView.setText(event.organizerWebsite);
                organizerWebsiteView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openOrganizerWebsite();
                    }
                });
            }

            if (!organizerInfoShown) {
                organizerHeader.setVisibility(View.GONE);
            } else {
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

            // Show tags.
            tagsView.removeAllViews();
            if (event.tags.length == 0) {
                tagsHeaderView.setVisibility(View.GONE);
            } else {
                tagsHeaderView.setVisibility(View.VISIBLE);
                for (final String tag : event.tags) {
                    addTagView(tagsView, tag, "tagClick");
                }
            }

            // AMA.
            ama.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEventAction(event, "ama");
                    Intent sendIntent = new Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("mailto:info@eventshigh.com?subject=Need%20More%20Info"));
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "Event: " + event.getEventDetailsURI() + "\n\nQuestion:\n<please type in your query here>" );
                    startActivity(sendIntent);
                }
            });
        }

        private void addTagView(LinearLayout parent, final String tagName, final String action) {
            getLayoutInflater().inflate(R.layout.event_tag, parent);
            View tagView = parent.getChildAt(parent.getChildCount() - 1);
            TextView tagNameView = (TextView) tagView.findViewById(R.id.tag_name);
            tagNameView.setText(tagName);
            tagNameView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEventAction(event, action, tagName);
                    Intent searchIntent = new Intent(EventDetailActivity.this, LaunchActivity.class);
                    searchIntent.setAction(Intent.ACTION_SEARCH);
                    searchIntent.putExtra(SearchManager.QUERY, tagName);
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
                    reportActionToAnalytics("addFollowing");
                    account.setIsFollowing(tagName, true);
                    followView.setVisibility(View.GONE);
                    followingView.setVisibility(View.VISIBLE);
                }
            });
            followingView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportActionToAnalytics("removeFollowing");
                    account.setIsFollowing(tagName, false);
                    followView.setVisibility(View.VISIBLE);
                    followingView.setVisibility(View.GONE);
                }
            });
        }

        private void setFavouriteView(@Nullable EventMark pref) {
            boolean isFavourite = EventMark.isFavourite(pref);
            favouritedView.setVisibility(isFavourite ? View.VISIBLE : View.GONE);
            favouriteView.setVisibility(isFavourite ? View.GONE : View.VISIBLE);
        }
    }
}

