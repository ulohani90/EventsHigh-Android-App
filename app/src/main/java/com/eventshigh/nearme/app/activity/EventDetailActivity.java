package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.DaySelector;
import com.eventshigh.nearme.app.utils.DownloadImageTask;

import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

/**
 * An activity representing a single Event detail screen.
 */
public class EventDetailActivity extends BaseActivity {

    /**********************************
     CONSTANTS
     **********************************/

    // The argument representing the event that this activity represents.
    public static final String ARG_EVENT_INFO = "event_info";

    // Regex to check if description is plane text or html.
    private static final Pattern htmlCheckPattern = Pattern.compile("<[A-Za-z].*</[A-Za-z]");


    /**********************************
     Members
     **********************************/

    // Event shown through this fragment.
    private Event mEvent;
    // Event card which holds the UI elements.
    private EventCard mEventCard;


    /**********************************
     Activity lifecycle management utilities
     **********************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Show the Up button in the action bar.
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Get the event from Intent.
        mEvent = getIntent().getParcelableExtra(ARG_EVENT_INFO);
        if (actionBar != null && mEvent != null) {
            actionBar.setTitle(mEvent.title);
        }

        // Populate View.
        mEventCard = new EventCard(getWindow().getDecorView());
        populateView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (mEvent == null) {
            return false;
        }

        getMenuInflater().inflate(R.menu.activity_detail, menu);
        MenuItem menuBookItem = menu.findItem(R.id.action_book);
        if (mEvent.booking_url == null) {
            menuBookItem.setVisible(false);
        } else {
            menuBookItem.getActionView().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                   gotoBookingSite();
                }
            });
        }

        MenuItem menuSaveItem = menu.findItem(R.id.action_save);
        if (mEvent.eventTimings.length == 0) {
            menuSaveItem.setVisible(false);
        } else {
            menuSaveItem.getActionView().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    addToCalendar(mEvent, null);
                }
            });
        }

        menu.findItem(R.id.action_share).getActionView().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shareEvent();
            }
        });
        return true;
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
            NavUtils.navigateUpTo(this, new Intent(this, EventGridActivity.class));
            return true;
        }

        if (id == R.id.action_book) {
            gotoBookingSite();
            return true;
        }

        if (id == R.id.action_save) {
            addToCalendar(mEvent, null);
            return true;
        }

        if (id == R.id.action_share) {
            shareEvent();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**********************************
     Callbacks, action handlers
     **********************************/

    public void openSourceSite(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mEvent.source_url));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open url. ignore.
        }
    }

    private void shareEvent() {
        shareEvent(mEventCard.rootView, mEvent);
    }

    private void gotoBookingSite() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mEvent.booking_url));
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
        private final ImageView recommendedImageView;
        private final ImageView bgView;
        private final TextView titleView;
        private final LinearLayout timeView;
        private final TextView numPeopleInterestedView;
        private final TextView venueView;
        private final TextView directionView;
        private final GridView tagsGrid;
        private final TextView descriptionView;
        private final TextView fromView;

        private EventCard(View rootView) {
            this.rootView = rootView;
            recommendedImageView = (ImageView) rootView.findViewById(R.id.eh_recommend_banner);
            bgView = (ImageView) rootView.findViewById(R.id.event_bg);
            titleView = (TextView) rootView.findViewById(R.id.event_title);
            timeView = (LinearLayout) rootView.findViewById(R.id.event_time);
            numPeopleInterestedView = (TextView) rootView.findViewById(R.id.num_people_interested);
            venueView = (TextView) rootView.findViewById(R.id.event_venue);
            directionView = (TextView) rootView.findViewById(R.id.buttonDirection);
            tagsGrid = (GridView) rootView.findViewById(R.id.event_tags);
            descriptionView = (TextView) rootView.findViewById(R.id.event_description);
            fromView = (TextView) rootView.findViewById(R.id.event_from);
        }
    }


    private void populateView() {
        if (mEvent == null) {
            mEventCard.rootView.setVisibility(View.INVISIBLE);
            return;
        }

        // Set Image
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mEventCard.bgView.setMaxHeight((int) (0.4 * metrics.heightPixels));
        if (mEvent.img_url == null) {
            mEventCard.bgView.setVisibility(View.GONE);
        } else {
            DownloadImageTask.setImage(mEventCard.bgView, mEvent.img_url, -1);
        }

        // Set title
        mEventCard.titleView.setText(mEvent.title);

        // Set EH recommendation banner
        mEventCard.recommendedImageView.setVisibility(mEvent.ehRecommended ? View.VISIBLE : View.GONE);

        // Set Venue.
        mEventCard.venueView.setText(mEvent.address == null ? mEvent.venue : mEvent.address);
        mEventCard.venueView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDirections(mEvent);
            }
        });
        mEventCard.directionView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDirections(mEvent);
            }
        });

        // Set time.
        if (mEvent.eventTimings.length == 0) {
            mEventCard.timeView.setVisibility(View.GONE);
        } else {
            for (long time : mEvent.eventTimings) {
                final Date date = new Date(time);
                LinearLayout daySelectorItem = DaySelector.getDaySelectorItem(
                        this, mEventCard.timeView, date, TimeZone.getTimeZone(mEvent.city.timeZone));
                mEventCard.timeView.addView(daySelectorItem);
                daySelectorItem.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addToCalendar(mEvent, date);
                    }
                });
            }
        }

        // Set Num people Interested
        if (mEvent.numPeopleInterested <= 0) {
            mEventCard.numPeopleInterestedView.setVisibility(View.GONE);
        } else {
            Resources res = getResources();
            String text = res.getQuantityString(R.plurals.people_interested,
                    mEvent.numPeopleInterested, mEvent.numPeopleInterested);
            mEventCard.numPeopleInterestedView.setText(text);
        }

        // Show tags.
        mEventCard.tagsGrid.setAdapter(
                new ArrayAdapter<>(this, R.layout.event_tag, mEvent.getAllTags()));

        // Set description.
        if (htmlCheckPattern.matcher(mEvent.description).find()) {
            mEventCard.descriptionView.setText(Html.fromHtml(mEvent.description));
        } else {
            mEventCard.descriptionView.setText(mEvent.description);
        }

        // Add attribution.
        if (mEvent.source_url == null) {
            mEventCard.fromView.setVisibility(View.INVISIBLE);
        } else {
            final Uri fromUri =  Uri.parse(mEvent.source_url);
            String eventFrom = String.format(
                    getResources().getString(R.string.event_detail_from),
                    fromUri.getHost());
            mEventCard.fromView.setText(eventFrom);
        }
    }
}
