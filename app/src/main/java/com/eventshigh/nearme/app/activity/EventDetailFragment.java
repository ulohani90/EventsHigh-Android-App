package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.task.DownloadImageTask;
import com.eventshigh.nearme.app.ui.DaySelector;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;

import java.util.Date;
import java.util.TimeZone;
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
    private Event mEvent;
    // Event card which holds the UI elements.
    private EventCard mEventCard;
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
            mEvent = getArguments().getParcelable(ARG_EVENT_INFO);
        }
        setHasOptionsMenu(mEvent != null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_event_detail, container, false);

        // Populate View.
        mEventCard = new EventCard(rootView);
        populateView();
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.fragment_detail, menu);

        menu.findItem(R.id.action_share).getActionView().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shareEvent();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            shareEvent();
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                Uri webUri = mEvent.getEventDetailsURI();
                AppIndex.AppIndexApi.view(client, activity, Utils.getAppUri(webUri),
                        mEvent.title, webUri, null);
            }

            @Override
            public void onConnectionSuspended(int i) {
                // do nothing.
            }
        });

        if (mEvent != null) {
            client.connect();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (client.isConnected()) {
            Uri webUri = mEvent.getEventDetailsURI();
            AppIndex.AppIndexApi.viewEnd(client, activity, Utils.getAppUri(webUri));
            client.disconnect();
        }
    }


    /**********************************
     Callbacks, action handlers
     **********************************/

    private void openSourceSite() {
        activity.reportActionToAnalytics("openSource");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mEvent.sourceUrl));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open url. ignore.
        }
    }

    private void call() {
        activity.reportActionToAnalytics("callOrganizer");
        Intent intent = new Intent(Intent.ACTION_DIAL)
                .setData(Uri.parse("tel:" + mEvent.organizerPhone));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to call. ignore.
        }
    }

    private void openOrganizerWebsite() {
        activity.reportActionToAnalytics("openOrganizerWebsite");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mEvent.organizerWebsite));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open url. ignore.
        }
    }

    private void shareEvent() {
        activity.shareEvent(mEventCard.shareView, mEvent);
    }

    private void openBookingSite() {
        activity.reportActionToAnalytics("bookTicket");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mEvent.bookingUrl));
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
        private final View shareView;
        private final ImageView recommendedImageView;
        private final ImageView bgView;
        private final TextView titleView;
        private final LinearLayout timeView;
        private final TextView numPeopleInterestedView;
        private final TextView venueView;
        private final TextView addressView;
        private final FrameLayout bookView;
        private final FrameLayout callView;
        private final FrameLayout saveView;
        private final TextView directionView;
        private final TextView tagsHeaderView;
        private final LinearLayout tagsView;
        private final View tagsSeparatorView;
        private final TextView descriptionView;
        private final TextView fromView;
        private final TextView organizerHeader;
        private final LinearLayout organizerNameRow;
        private final TextView organizerNameView;
        private final LinearLayout organizerPhoneRow;
        private final TextView organizerPhoneView;
        private final LinearLayout organizerWebsiteRow;
        private final TextView organizerWebsiteView;

        private EventCard(View rootView) {
            this.rootView = rootView;
            shareView = rootView.findViewById(R.id.share_view);
            recommendedImageView = (ImageView) rootView.findViewById(R.id.eh_recommend_banner);
            bgView = (ImageView) rootView.findViewById(R.id.event_bg);
            titleView = (TextView) rootView.findViewById(R.id.event_title);
            timeView = (LinearLayout) rootView.findViewById(R.id.event_time);
            numPeopleInterestedView = (TextView) rootView.findViewById(R.id.num_people_interested);
            venueView = (TextView) rootView.findViewById(R.id.event_venue);
            addressView = (TextView) rootView.findViewById(R.id.event_address);
            bookView = (FrameLayout) rootView.findViewById(R.id.book_ticket);
            callView = (FrameLayout) rootView.findViewById(R.id.call);
            saveView = (FrameLayout) rootView.findViewById(R.id.save);
            directionView = (TextView) rootView.findViewById(R.id.show_directions);
            tagsHeaderView = (TextView) rootView.findViewById(R.id.event_tags_header);
            tagsView = (LinearLayout) rootView.findViewById(R.id.event_tags);
            tagsSeparatorView = rootView.findViewById(R.id.event_tags_separator);
            descriptionView = (TextView) rootView.findViewById(R.id.event_description);
            fromView = (TextView) rootView.findViewById(R.id.event_from);
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
        if (mEvent == null) {
            mEventCard.rootView.setVisibility(View.INVISIBLE);
            return;
        }

        // Set Image
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int maxHeight = (int) (0.4 * metrics.heightPixels);
        int infographResId = mEvent.category.getInfographResourceId();
        mEventCard.bgView.setMaxHeight(maxHeight);
        if (mEvent.imgUrl == null && infographResId == R.drawable.eh_default) {
            mEventCard.bgView.setVisibility(View.GONE);
        } else {
            DownloadImageTask.setImage(mEventCard.bgView, getResources(),
                    mEvent.imgUrl, infographResId);
        }

        if (mEvent.imgUrl != null) {
            mEventCard.bgView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.reportActionToAnalytics("imagePreview");
                    final Dialog nagDialog = new Dialog(activity, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
                    nagDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    nagDialog.setCancelable(true);
                    nagDialog.setContentView(R.layout.dialog_image_preview);

                    ImageViewTouch preview = (ImageViewTouch) nagDialog.findViewById(R.id.image_preview);
                    DownloadImageTask.setImageNoCache(preview, mEvent.imgUrl, getResources());

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
        mEventCard.titleView.setText(mEvent.title);

        // Set EH recommendation banner
        mEventCard.recommendedImageView.setVisibility(mEvent.ehRecommended ? View.VISIBLE : View.GONE);

        // Set Num people Interested
        if (mEvent.numPeopleInterested <= 0) {
            mEventCard.numPeopleInterestedView.setVisibility(View.GONE);
        } else {
            Resources res = getResources();
            String text = res.getQuantityString(R.plurals.people_interested,
                    mEvent.numPeopleInterested, mEvent.numPeopleInterested);
            mEventCard.numPeopleInterestedView.setText(text);
            mEventCard.numPeopleInterestedView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    gaOptOutCounter ++;
                    if (gaOptOutCounter == NUM_TAPS_FOR_GA_OPT_OUT) {
                        Toast.makeText(activity, "GA reporting disabled on this device", Toast.LENGTH_SHORT).show();
                        activity.gaHelper.setAppOptOut(true);
                    }
                }
            });
        }

        // Set Venue and address.
        if (mEvent.venue == null) {
            mEventCard.venueView.setVisibility(View.GONE);
        } else {
            mEventCard.venueView.setText(Utils.capitalize(mEvent.venue));
        }
        mEventCard.addressView.setText(
                mEvent.address == null ? Utils.capitalize(mEvent.city.toString()) : mEvent.address);
        mEventCard.directionView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showDirections(mEvent);
            }
        });

        // Set action buttons.
        if (mEvent.bookingUrl == null) {
            mEventCard.bookView.setVisibility(View.GONE);
        } else {
            mEventCard.bookView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    openBookingSite();
                }
            });
        }

        if (mEvent.organizerPhone == null) {
            mEventCard.callView.setVisibility(View.GONE);
        } else {
            mEventCard.callView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    call();
                }
            });
        }

        // Set time.
        if (mEvent.eventTimings.length == 0) {
            mEventCard.timeView.setVisibility(View.GONE);
            mEventCard.saveView.setVisibility(View.GONE);
        } else {
            mEventCard.saveView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    activity.addToCalendar(mEvent, null);
                }
            });
            for (long time : mEvent.eventTimings) {
                final Date date = new Date(time);
                LinearLayout daySelectorItem = DaySelector.getDaySelectorItem(
                        activity, mEventCard.timeView, date, TimeZone.getTimeZone(mEvent.city.timeZone));
                mEventCard.timeView.addView(daySelectorItem, getLayoutParam());
                daySelectorItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        activity.addToCalendar(mEvent, date);
                    }
                });
            }
        }

        // Set description.
        if (htmlCheckPattern.matcher(mEvent.description).find()) {
            mEventCard.descriptionView.setText(Html.fromHtml(mEvent.description));
        } else {
            mEventCard.descriptionView.setText(mEvent.description);
        }

        // Add attribution.
        if (mEvent.sourceUrl == null) {
            mEventCard.fromView.setVisibility(View.INVISIBLE);
        } else {
            final Uri fromUri =  Uri.parse(mEvent.sourceUrl);
            String eventFrom = String.format(
                    getResources().getString(R.string.event_detail_from),
                    fromUri.getHost());
            mEventCard.fromView.setText(eventFrom);
            mEventCard.fromView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    openSourceSite();
                }
            });
        }

        // Organizer Info.
        boolean organizerInfoShown = false;
        if (mEvent.organizerName == null) {
            mEventCard.organizerNameRow.setVisibility(View.GONE);
        } else {
            organizerInfoShown = true;
            mEventCard.organizerNameView.setText(mEvent.organizerName);
        }

        if (mEvent.organizerPhone == null) {
            mEventCard.organizerPhoneRow.setVisibility(View.GONE);
        } else {
            organizerInfoShown = true;
            mEventCard.organizerPhoneView.setText(mEvent.organizerPhone);
            mEventCard.organizerPhoneView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    call();
                }
            });
        }

        if (mEvent.organizerWebsite == null) {
            mEventCard.organizerWebsiteRow.setVisibility(View.GONE);
        } else {
            organizerInfoShown = true;
            mEventCard.organizerWebsiteView.setText(mEvent.organizerWebsite);
            mEventCard.organizerWebsiteView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    openOrganizerWebsite();
                }
            });
        }

        if (!organizerInfoShown) {
            mEventCard.organizerHeader.setVisibility(View.GONE);
        }

        // Show tags.
        if (mEvent.getAllTags().length > 0) {
            showTags();
        } else {
            mEventCard.tagsSeparatorView.setVisibility(View.GONE);
            mEventCard.tagsHeaderView.setVisibility(View.GONE);
        }
    }

    private void showTags() {
        Utils.waitForViewVisible(mEventCard.tagsView, new Runnable() {
            public void run() {
                LayoutParams layoutParams = getLayoutParam();
                int maxWidth = mEventCard.tagsView.getWidth()
                        - layoutParams.leftMargin - layoutParams.rightMargin;
                LinearLayout ll = getLL(layoutParams);
                for (String tag : mEvent.getAllTags()) {
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
                activity.showSearchView(tag);
            }
        });
        return  tagView;
    }

    private LinearLayout getLL(LayoutParams layoutParams) {
        LinearLayout ll = new LinearLayout(activity);
        ll.setLayoutParams(layoutParams);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        mEventCard.tagsView.addView(ll);
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
