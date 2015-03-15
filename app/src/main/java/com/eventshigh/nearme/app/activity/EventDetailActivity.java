package com.eventshigh.nearme.app.activity;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
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
import com.eventshigh.nearme.app.data.EventsMarkerManager;
import com.eventshigh.nearme.app.data.EventsMarkerManager.Editor;
import com.eventshigh.nearme.app.data.EventsMarkerManager.EventMark;
import com.eventshigh.nearme.app.network.EventRequest;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.AlarmUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.IntentUtils;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;

import org.apmem.tools.layouts.FlowLayout;

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
    private Event event;
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

    public void setScroll(int scrollValue) {
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
            eventCard.populateView(event);

            // Setup GoogleApiClient
            client = new GoogleApiClient.Builder(EventDetailActivity.this).addApi(AppIndex.APP_INDEX_API).build();
            client.registerConnectionCallbacks(new ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    Uri webUri = event.getEventDetailsURI();
                    AppIndex.AppIndexApi.view(client, EventDetailActivity.this, Utils.getAppUri(webUri),
                            event.title, webUri, null);
                }

                @Override
                public void onConnectionSuspended(int i) {
                    // do nothing.
                }
            });
            client.connect();
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

    private void shareEvent() {
        int favouriteViewVisibility = eventCard.favouriteView.getVisibility();
        int favouritedViewVisibility = eventCard.favouritedView.getVisibility();
        eventCard.favouriteView.setVisibility(View.GONE);
        eventCard.favouritedView.setVisibility(View.GONE);
        eventCard.dismissView.setVisibility(View.GONE);

        shareEvent(eventCard.shareContentsView, event);

        eventCard.dismissView.setVisibility(View.VISIBLE);
        eventCard.favouriteView.setVisibility(favouriteViewVisibility);
        eventCard.favouritedView.setVisibility(favouritedViewVisibility);
    }

    private void openBookingSite() {
        reportEventAction(event, "bookTicket");
        Intent intent = new Intent(this, CustomUrlActivity.class);
        intent.setData(Uri.parse(event.bookingUrl));
        intent.putExtra(CustomUrlActivity.EXTRA_TITLE_KEY, getString(R.string.title_book));
        startActivity(intent);
    }

    private void openContestSite() {
        reportEventAction(event, "openContest");
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
        private final TextView favouriteView;
        private final TextView favouritedView;
        private final TextView dismissView;

        private final TextView titleView;
        private final TextView fromView;

        private final RelativeLayout venueGroupView;
        private final TextView venueView;
        private final TextView addressView;

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

        private final TextView contestView;
        private final FlowLayout tagsView;
        private final TextView descriptionView;
        private final TextView readMoreView;

        private final TextView organizerHeader;
        private final LinearLayout organizerNameRow;
        private final TextView organizerNameView;
        private final LinearLayout organizerPhoneRow;
        private final TextView organizerPhoneView;
        private final LinearLayout organizerWebsiteRow;
        private final TextView organizerWebsiteView;

        private EventCard() {
            eventScrollView = (ScrollView) findViewById(R.id.event_scroll_view);
            shareContentsView = findViewById(R.id.share_view);

            recommendedImageView = (ImageView) findViewById(R.id.eh_recommend_banner);
            bgView = (NetworkImageView) findViewById(R.id.event_bg);
            favouriteView = (TextView) findViewById(R.id.action_favourite);
            favouritedView = (TextView) findViewById(R.id.action_favourited);
            dismissView = (TextView) findViewById(R.id.action_dismiss);

            titleView = (TextView) findViewById(R.id.event_title);
            fromView = (TextView) findViewById(R.id.event_from);

            venueGroupView = (RelativeLayout) findViewById(R.id.event_venue_group);
            venueView = (TextView) findViewById(R.id.event_venue);
            addressView = (TextView) findViewById(R.id.event_address);

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

            contestView = (TextView) findViewById(R.id.contest);
            tagsView = (FlowLayout) findViewById(R.id.event_tags);
            descriptionView = (TextView) findViewById(R.id.event_description);
            readMoreView = (TextView) findViewById(R.id.read_more);

            organizerHeader = (TextView) findViewById(R.id.organizer_header);
            organizerNameRow = (LinearLayout) findViewById(R.id.organizer_name_row);
            organizerNameView = (TextView) findViewById(R.id.organizer_name);
            organizerPhoneRow = (LinearLayout) findViewById(R.id.organizer_phone_row);
            organizerPhoneView = (TextView) findViewById(R.id.organizer_phone);
            organizerWebsiteRow = (LinearLayout) findViewById(R.id.organizer_website_row);
            organizerWebsiteView = (TextView) findViewById(R.id.organizer_website);
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
            favouriteView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEventAction(event, "addFavourite");
                    eventsMarkerEditor.recordEventMark(event, EventMark.FAVOURITE);
                    setFavouriteView(EventMark.FAVOURITE);
                    AlarmUtils.setAlarm(EventDetailActivity.this, event);
                }
            });
            favouritedView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEventAction(event, "removeFavourite");
                    eventsMarkerEditor.recordEventMark(event, null);
                    setFavouriteView(null);
                    AlarmUtils.cancelAlarm(EventDetailActivity.this, event);
                }
            });

            eventCard.dismissView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportEventAction(event, "dismiss");
                    eventsMarkerEditor.recordEventMark(event, EventMark.DISMISSED);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
            venueGroupView.setOnClickListener(new OnClickListener() {
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
                    shareEvent();
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

            // Set contest.
            if (event.offerTitle != null) {
                contestView.setVisibility(View.VISIBLE);
                contestView.setText(event.offerTitle);
                contestView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openContestSite();
                    }
                });
            } else {
                contestView.setVisibility(View.GONE);
            }

            // Set description.
            if (HTML_PATTERN.matcher(event.description).find()) {
                descriptionView.setText(Html.fromHtml(event.description));
                descriptionView.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                descriptionView.setText(event.description);
            }
            readMoreView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    descriptionView.setMaxLines(Integer.MAX_VALUE);
                    readMoreView.setVisibility(View.GONE);
                }
            });

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
            for (final String tag : event.tags) {
                getLayoutInflater().inflate(R.layout.event_tag, tagsView);
                TextView tagView = (TextView) tagsView.getChildAt(tagsView.getChildCount() - 1);
                tagView.setText(tag);
                tagView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        reportEventAction(event, "tagClick");
                        Intent searchIntent = new Intent(EventDetailActivity.this, LaunchActivity.class);
                        searchIntent.setAction(Intent.ACTION_SEARCH);
                        searchIntent.putExtra(SearchManager.QUERY, tag);
                        startActivity(searchIntent);
                    }
                });
            }
        }

        private void setFavouriteView(@Nullable EventMark pref) {
            boolean isFavourite = EventMark.isFavourite(pref);
            favouritedView.setVisibility(isFavourite ? View.VISIBLE : View.GONE);
            favouriteView.setVisibility(isFavourite ? View.GONE : View.VISIBLE);
        }
    }
}

