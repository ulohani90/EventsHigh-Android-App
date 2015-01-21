package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;

import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

/**
 * A {@link Fragment} which shows the event details.
 *
 * Use the {@link EventDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EventDetailFragment extends Fragment {

    /**********************************
     CONSTANTS
     **********************************/

    // The argument representing the event that this activity represents.
    public static final String ARG_EVENT_INFO = "event_info";

    // Regex to check if description is plane text or html.
    private static final Pattern htmlCheckPattern = Pattern.compile("<[A-Za-z].*</[A-Za-z]");

    // Number of taps needed for GA opt out.
    private static final int NUM_TAPS_FOR_GA_OPT_OUT = 7;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment EventDetailFragment.
     */
    public static EventDetailFragment newInstance(Event event) {
        EventDetailFragment fragment = new EventDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_EVENT_INFO, event);
        fragment.setArguments(args);
        return fragment;
    }

    /**********************************
     Members
     **********************************/

    // Event shown through this fragment.
    private Event event;
    // Event card which holds the UI elements.
    private EventCard eventCard;
    // Activity to which this fragment is attached
    private BaseActivity activity;
    // Hidden trick to disable a device from GA reporting is to tap on
    // num_people_interested text multiple times.
    private int gaOptOutCounter = 0;
    // GoogleApiClient to report the page view.
    private GoogleApiClient client;

    public EventDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            event = getArguments().getParcelable(ARG_EVENT_INFO);
        }
        setHasOptionsMenu(event != null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_event_detail, container, false);

        // Populate View.
        eventCard = new EventCard(rootView);
        populateView();
        return rootView;
    }

    public void onAttach(final Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseActivity) activity;
    }

    public void onStart() {
        super.onStart();

        // Setup GoogleApiClient
        client = new GoogleApiClient.Builder(activity).addApi(AppIndex.APP_INDEX_API).build();
        client.registerConnectionCallbacks(new ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Uri webUri = event.getEventDetailsURI();
                AppIndex.AppIndexApi.view(client, activity, Utils.getAppUri(webUri),
                        event.title, webUri, null);
            }

            @Override
            public void onConnectionSuspended(int i) {
                // do nothing.
            }
        });

        if (event != null) {
            client.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (client.isConnected()) {
            Uri webUri = event.getEventDetailsURI();
            AppIndex.AppIndexApi.viewEnd(client, activity, Utils.getAppUri(webUri));
            client.disconnect();
        }
    }


    /**********************************
     Callbacks, action handlers
     **********************************/

    private void openSourceSite() {
        activity.reportActionToAnalytics("openSource");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.sourceUrl));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open url. ignore.
        }
    }

    private void call() {
        activity.reportActionToAnalytics("callOrganizer");
        Intent intent = new Intent(Intent.ACTION_DIAL)
                .setData(Uri.parse("tel:" + event.organizerPhone));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to call. ignore.
        }
    }

    private void openOrganizerWebsite() {
        activity.reportActionToAnalytics("openOrganizerWebsite");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.organizerWebsite));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open url. ignore.
        }
    }

    private void shareEvent() {
        activity.shareEvent(eventCard.shareContentsView, event);
    }

    private void openBookingSite() {
        activity.reportActionToAnalytics("bookTicket");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(event.bookingUrl));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open url. ignore.
        }
    }


    /**********************************
     Helper class to hold all UI elements.
     **********************************/

    private static class EventCard {
        private final View rootView;
        private final View shareContentsView;

        private final ImageView recommendedImageView;
        private final NetworkImageView bgView;
        private final TextView titleView;
        private final TextView fromView;

        private final RelativeLayout venueGroupView;
        private final TextView venueView;
        private final TextView addressView;

        private final LinearLayout timeGroupView;
        private final LinearLayout eventTimeFirstView;
        private final TextView timeView;
        private final TextView timeDetailView;
        private final TextView alsoOnView;
        private final HorizontalScrollView futureTimesViewGroup;
        private final LinearLayout futureTimesView;

        private final FrameLayout bookView;
        private final FrameLayout callView;
        private final FrameLayout shareView;

        private final LinearLayout tagsView;
        private final TextView descriptionView;

        private final TextView organizerHeader;
        private final LinearLayout organizerNameRow;
        private final TextView organizerNameView;
        private final LinearLayout organizerPhoneRow;
        private final TextView organizerPhoneView;
        private final LinearLayout organizerWebsiteRow;
        private final TextView organizerWebsiteView;

        private EventCard(View rootView) {
            this.rootView = rootView;
            shareContentsView = rootView.findViewById(R.id.share_view);

            recommendedImageView = (ImageView) rootView.findViewById(R.id.eh_recommend_banner);
            bgView = (NetworkImageView) rootView.findViewById(R.id.event_bg);

            titleView = (TextView) rootView.findViewById(R.id.event_title);
            fromView = (TextView) rootView.findViewById(R.id.event_from);

            venueGroupView = (RelativeLayout) rootView.findViewById(R.id.event_venue_group);
            venueView = (TextView) rootView.findViewById(R.id.event_venue);
            addressView = (TextView) rootView.findViewById(R.id.event_address);

            timeGroupView = (LinearLayout) rootView.findViewById(R.id.event_time_group);
            eventTimeFirstView = (LinearLayout) rootView.findViewById(R.id.event_time_first);
            timeView = (TextView) rootView.findViewById(R.id.event_time);
            timeDetailView = (TextView) rootView.findViewById(R.id.event_time_details);
            alsoOnView = (TextView) rootView.findViewById(R.id.also_on);
            futureTimesViewGroup = (HorizontalScrollView) rootView.findViewById(R.id.event_future_times_hs);
            futureTimesView = (LinearLayout) rootView.findViewById(R.id.event_future_times);

            bookView = (FrameLayout) rootView.findViewById(R.id.book_ticket);
            callView = (FrameLayout) rootView.findViewById(R.id.call);
            shareView = (FrameLayout) rootView.findViewById(R.id.share);

            tagsView = (LinearLayout) rootView.findViewById(R.id.event_tags);
            descriptionView = (TextView) rootView.findViewById(R.id.event_description);

            organizerHeader = (TextView) rootView.findViewById(R.id.organizer_header);
            organizerNameRow = (LinearLayout) rootView.findViewById(R.id.organizer_name_row);
            organizerNameView = (TextView) rootView.findViewById(R.id.organizer_name);
            organizerPhoneRow = (LinearLayout) rootView.findViewById(R.id.organizer_phone_row);
            organizerPhoneView = (TextView) rootView.findViewById(R.id.organizer_phone);
            organizerWebsiteRow = (LinearLayout) rootView.findViewById(R.id.organizer_website_row);
            organizerWebsiteView = (TextView) rootView.findViewById(R.id.organizer_website);
        }
    }

    private void populateView() {
        if (event == null) {
            eventCard.rootView.setVisibility(View.INVISIBLE);
            return;
        }

        // Set Image
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        ViewGroup.LayoutParams params = eventCard.bgView.getLayoutParams();
        params.height = (int) (0.3 * metrics.heightPixels);
        eventCard.bgView.setLayoutParams(params);
        if (event.imgUrl == null) {
            eventCard.bgView.setVisibility(View.GONE);
        } else {
            eventCard.bgView.setImageUrl(event.imgUrl,
                    VolleyHelper.getImageLoader(activity.getApplicationContext()));
            eventCard.bgView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportActionToAnalytics("imagePreview");
                    final Dialog nagDialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
                    nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    nagDialog.setCancelable(true);
                    nagDialog.setContentView(R.layout.dialog_image_preview);

                    ImageViewTouch preview = (ImageViewTouch) nagDialog.findViewById(R.id.image_preview);
                    VolleyHelper.getImageLoader(getActivity().getApplicationContext()).get(
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
        }

        // Set title
        eventCard.titleView.setText(event.title);
        eventCard.titleView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                gaOptOutCounter++;
                if (gaOptOutCounter == NUM_TAPS_FOR_GA_OPT_OUT) {
                    Toast.makeText(activity, "GA reporting disabled on this device", Toast.LENGTH_SHORT).show();
                    activity.gaHelper.setAppOptOut(true);
                }
            }
        });

        // Set EH recommendation banner
        eventCard.recommendedImageView.setVisibility(event.ehRecommended ? View.VISIBLE : View.GONE);

        // Set Num people Interested
        final ActionBar actionBar = getActivity().getActionBar();
        if (actionBar != null) {
            if (event.numPeopleInterested <= 0) {
                actionBar.setSubtitle("");
            } else {
                String text = getResources().getQuantityString(R.plurals.people_interested,
                        event.numPeopleInterested, event.numPeopleInterested);
                actionBar.setSubtitle(text);
            }
        }

        // Set Venue and address.
        if (event.venue == null && event.address == null) {
            eventCard.venueGroupView.setVisibility(View.GONE);
        } else {
            if (event.venue == null) {
                eventCard.venueView.setVisibility(View.GONE);
            } else {
                eventCard.venueView.setText(Utils.capitalize(event.venue));
            }
            eventCard.addressView.setText(
                    event.address == null ? Utils.capitalize(event.city.toString()) : event.address);
            eventCard.venueGroupView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.showDirections(event);
                }
            });
        }

        // Set action buttons.
        if (event.bookingUrl == null) {
            eventCard.bookView.setVisibility(View.GONE);
        } else {
            eventCard.bookView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    openBookingSite();
                }
            });
        }

        if (event.organizerPhone == null) {
            eventCard.callView.setVisibility(View.GONE);
        } else {
            eventCard.callView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    call();
                }
            });
        }

        eventCard.shareView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shareEvent();
            }
        });

        // Set time.
        EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
        if (eventTime == null) {
            eventCard.timeGroupView.setVisibility(View.GONE);
        } else {
            eventCard.timeView.setText(eventTime.toString());
            eventCard.timeGroupView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.addToCalendar(event, null);
                }
            });

            Date eventDate = DateTimeUtils.getEventDate(event, 0);
            Date today = DateTimeUtils.toMidnight(Calendar.getInstance(), event.city.timeZone).getTime();
            int numDays = (int) TimeUnit.MILLISECONDS.toDays(eventDate.getTime() - today.getTime());
            if (numDays >= 0) {
                eventCard.timeDetailView.setText(
                    MessageFormat.format(getResources().getString(R.string.event_time_details), numDays));
            }

            if (event.eventTimings.length > 1) {
                for (int i = 1; i < event.eventTimings.length; i++) {
                    eventTime = DateTimeUtils.getEventTime(event, i);
                    if (eventTime == null) {
                        break;
                    }

                    final Date eventDateCurr = new Date(event.eventTimings[i]);
                    View timeView = getActivity().getLayoutInflater().inflate(
                            R.layout.event_time, eventCard.futureTimesView, false);
                    ((TextView)timeView.findViewById(R.id.event_day)).setText(
                            eventTime.day + ", " + eventTime.date);
                    ((TextView)timeView.findViewById(R.id.event_time)).setText(eventTime.time);
                    eventCard.futureTimesView.addView(timeView);
                    timeView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            activity.addToCalendar(event, eventDateCurr);
                        }
                    });
                }

                eventCard.alsoOnView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (eventCard.futureTimesViewGroup.getVisibility() == View.GONE) {
                            eventCard.eventTimeFirstView.setVisibility(View.GONE);
                            eventCard.futureTimesViewGroup.setVisibility(View.VISIBLE);
                        } else {
                            eventCard.eventTimeFirstView.setVisibility(View.VISIBLE);
                            eventCard.futureTimesViewGroup.setVisibility(View.GONE);
                        }
                    }
                });
            } else {
                eventCard.alsoOnView.setVisibility(View.GONE);
            }
        }

        // Set description.
        if (htmlCheckPattern.matcher(event.description).find()) {
            eventCard.descriptionView.setText(Html.fromHtml(event.description));
        } else {
            eventCard.descriptionView.setText(event.description);
        }

        // Add attribution.
        if (event.sourceUrl == null) {
            eventCard.fromView.setVisibility(View.INVISIBLE);
        } else {
            final Uri fromUri =  Uri.parse(event.sourceUrl);
            String eventFrom = String.format(
                    getResources().getString(R.string.event_detail_from),
                    fromUri.getHost());
            eventCard.fromView.setText(eventFrom);
            eventCard.fromView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSourceSite();
                }
            });
        }

        // Organizer Info.
        boolean organizerInfoShown = false;
        if (event.organizerName == null) {
            eventCard.organizerNameRow.setVisibility(View.GONE);
        } else {
            organizerInfoShown = true;
            eventCard.organizerNameView.setText(event.organizerName);
        }

        if (event.organizerPhone == null) {
            eventCard.organizerPhoneRow.setVisibility(View.GONE);
        } else {
            organizerInfoShown = true;
            eventCard.organizerPhoneView.setText(event.organizerPhone);
            eventCard.organizerPhoneView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    call();
                }
            });
        }

        if (event.organizerWebsite == null) {
            eventCard.organizerWebsiteRow.setVisibility(View.GONE);
        } else {
            organizerInfoShown = true;
            eventCard.organizerWebsiteView.setText(event.organizerWebsite);
            eventCard.organizerWebsiteView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    openOrganizerWebsite();
                }
            });
        }

        if (!organizerInfoShown) {
            eventCard.organizerHeader.setVisibility(View.GONE);
        }

        // Show tags.
        if (event.getAllTags().length > 0) {
            showTags();
        }
    }

    private void showTags() {
        Utils.waitForViewVisible(eventCard.tagsView, new Runnable() {
            public void run() {
                LayoutParams layoutParams = getLayoutParam();
                int maxWidth = eventCard.tagsView.getWidth()
                        - layoutParams.leftMargin - layoutParams.rightMargin;
                LinearLayout ll = getLL(layoutParams);
                for (String tag : event.getAllTags()) {
                    TextView tagView = addTag(ll, tag);
                    ll.measure(0, 0);
                    if (ll.getMeasuredWidth() < maxWidth) {
                        continue;
                    }

                    tagView.setVisibility(View.GONE);
                    ll = getLL(layoutParams);
                    addTag(ll, tag);
                }
            }
        }, 100);
    }

    private TextView addTag(LinearLayout ll, final String tag) {
        activity.getLayoutInflater().inflate(R.layout.event_tag, ll);
        TextView tagView = (TextView) ll.getChildAt(ll.getChildCount() - 1);
        tagView.setText(tag);
        tagView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.reportActionToAnalytics("tagClick");
                Intent searchIntent = new Intent(activity, LaunchActivity.class);
                searchIntent.setAction(Intent.ACTION_SEARCH);
                searchIntent.putExtra(SearchManager.QUERY, tag);
                startActivity(searchIntent);
            }
        });
        return  tagView;
    }

    private LinearLayout getLL(LayoutParams layoutParams) {
        LinearLayout ll = new LinearLayout(activity);
        ll.setLayoutParams(layoutParams);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        eventCard.tagsView.addView(ll);
        return  ll;
    }

    private LayoutParams getLayoutParam() {
        LayoutParams layoutParams =
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        int margin = Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
        layoutParams.setMargins(0, 0 , margin, 0);
        return layoutParams;
    }
}
