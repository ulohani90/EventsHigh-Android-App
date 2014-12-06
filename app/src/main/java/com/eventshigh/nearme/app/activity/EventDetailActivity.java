package com.eventshigh.nearme.app.activity;

import android.app.ActionBar;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.support.v4.app.NavUtils;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.DownloadImageTask;
import com.eventshigh.nearme.app.utils.EventListAdapter;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.Calendar;
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
        if (mEvent.startTime == null) {
            menu.removeItem(R.id.action_cal);
        }
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

        if (id == R.id.action_share) {
            shareEvent();
            return true;
        }

        if (id == R.id.action_direction) {
            showDirections();
            return true;
        }

        if (id == R.id.action_cal) {
            addToCalendar();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**********************************
     Callbacks, action handlers
     **********************************/

    private OnClickListener mOpenSource = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mEvent.source_url));
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                // No activity to open url. ignore.
            }
        }
    };

    private void shareEvent() {
        mEventCard.descriptionView.setVisibility(View.GONE);
        shareEvent(mEventCard.rootView, mEvent);
        mEventCard.descriptionView.setVisibility(View.VISIBLE);
    }

    private void showDirections() {
       reportActionToAnalytics("showDirections");

        Uri locationUri = Uri.parse("geo:0,0?q=" +
                mEvent.location.latitude + "," + mEvent.location.longitude +
                " (" + mEvent.title + ")");
        Intent intent = new Intent(Intent.ACTION_VIEW, locationUri);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open maps.
            Toast.makeText(this, R.string.no_map_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void addToCalendar() {
        reportActionToAnalytics("addToCalendar");

        Calendar cal = Calendar.getInstance();
        cal.setTime(mEvent.startTime);
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(Events.CONTENT_URI)
                .putExtra(Events.TITLE, mEvent.title)
                .putExtra(Events.EVENT_LOCATION, mEventCard.venueView.getText())
                .putExtra(Events.DESCRIPTION,
                        mEvent.getEventDetailsURI().toString() + "\n\n" + mEvent.description)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, cal);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open cal.
            Toast.makeText(this, R.string.no_cal_app, Toast.LENGTH_SHORT).show();
        }
    }

    /**********************************
     Helper class to hold all UI elements.
     **********************************/

    private static class EventCard {
        private final View rootView;
        private final TextView titleView;
        private final TextView fromView;
        private final ImageView recommendedImageView;
        private final TextView venueView;
        private final TextView timeView;
        private final TextView numPeopleInterestedView;
        private final TextView tag0View;
        private final TextView tag1View;
        private final TextView tag2View;
        private final ImageView bgView;
        private final TextView descriptionView;

        private EventCard(View rootView) {
            this.rootView = rootView;
            titleView = (TextView) rootView.findViewById(R.id.event_title);
            fromView = (TextView) rootView.findViewById(R.id.event_from);
            recommendedImageView = (ImageView) rootView.findViewById(R.id.eh_recommend_banner);
            venueView = (TextView) rootView.findViewById(R.id.event_venue);
            timeView = (TextView) rootView.findViewById(R.id.event_time);
            numPeopleInterestedView = (TextView) rootView.findViewById(R.id.num_people_interested);
            tag0View = (TextView) rootView.findViewById(R.id.event_tag0);
            tag1View = (TextView) rootView.findViewById(R.id.event_tag1);
            tag2View = (TextView) rootView.findViewById(R.id.event_tag2);
            bgView = (ImageView) rootView.findViewById(R.id.event_bg);
            descriptionView = (TextView) rootView.findViewById(R.id.event_description);
        }
    }


    private void populateView() {
        if (mEvent == null) {
            mEventCard.rootView.setVisibility(View.INVISIBLE);
            return;
        }

        // Set title
        mEventCard.titleView.setText(mEvent.title);

        // Set EH recommendation banner
        mEventCard.recommendedImageView.setVisibility(mEvent.ehRecommended ? View.VISIBLE : View.GONE);

        // Add attribution.
        if (mEvent.source_url == null) {
            mEventCard.fromView.setVisibility(View.GONE);
        } else {
            final Uri fromUri =  Uri.parse(mEvent.source_url);
            String eventFrom = String.format(
                    getResources().getString(R.string.event_detail_from),
                    fromUri.getHost());
            mEventCard.fromView.setText(eventFrom);
            mEventCard.fromView.setOnClickListener(mOpenSource);
        }

        // Set Venue.
        mEventCard.venueView.setText(mEvent.address == null ? mEvent.venue : mEvent.address);
        mEventCard.venueView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showDirections();
            }
        });

        // Set time.
        if (mEvent.startTime == null) {
            mEventCard.timeView.setVisibility(View.GONE);
        } else {
            mEventCard.timeView.setText(Utils.getEventTime(mEvent));
            mEventCard.timeView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    addToCalendar();
                }
            });
        }

        // Set Num people Interested
        Resources res = getResources();
        String text = res.getQuantityString(R.plurals.people_interested,
                mEvent.numPeopleInterested, mEvent.numPeopleInterested);
        mEventCard.numPeopleInterestedView.setText(text);

        // Show tags.
        EventListAdapter.showTag(mEventCard.tag0View, mEvent, 0);
        EventListAdapter.showTag(mEventCard.tag1View, mEvent, 1);
        EventListAdapter.showTag(mEventCard.tag2View, mEvent, 2);

        // Set Image
        if (mEvent.img_url == null) {
            mEventCard.bgView.setVisibility(View.GONE);
        } else {
            DownloadImageTask.setImage(mEventCard.bgView, mEvent.img_url, -1);
        }

        // Set description.
        if (htmlCheckPattern.matcher(mEvent.description).find()) {
            mEventCard.descriptionView.setText(Html.fromHtml(mEvent.description));
        } else {
            mEventCard.descriptionView.setText(mEvent.description);
        }
    }
}
