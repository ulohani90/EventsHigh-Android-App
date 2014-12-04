package com.eventshigh.nearme.app.activity;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.DownloadImageTask;
import com.eventshigh.nearme.app.utils.Utils;

import java.util.Calendar;
import java.util.regex.Pattern;

/**
 * A fragment representing a single Event detail screen.
 * This fragment is either contained in a {@link EventListActivity}
 * in two-pane mode (on tablets) or a {@link EventDetailActivity}
 * on handsets.
 */
public class EventDetailFragment extends Fragment {

    /**********************************
     CONSTANTS
     **********************************/

    // The fragment argument representing the event that this fragment represents
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

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public EventDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments().containsKey(ARG_EVENT_INFO)) {
            mEvent = getArguments().getParcelable(ARG_EVENT_INFO);
        }
        setHasOptionsMenu(mEvent != null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.fragment_detail, menu);
        if (mEvent.startTime == null) {
            menu.getItem(R.id.action_cal).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int selectedItemId = item.getItemId();
        if (selectedItemId == R.id.action_share) {
            shareEvent();
            return true;
        }

        if (selectedItemId == R.id.action_direction) {
            showDirections();
            return true;
        }

        if (selectedItemId == R.id.action_cal) {
            addToCalendar();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_event_detail, container, false);
        this.mEventCard = new EventCard(rootView);

        if (mEvent == null) {
            rootView.setVisibility(View.INVISIBLE);
            return rootView;
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

        // Set Image
        if (mEvent.img_url == null) {
            mEventCard.bgView.setVisibility(View.GONE);
        } else {
            new DownloadImageTask(mEventCard.bgView).execute(mEvent.img_url);
        }

        // Set description.
        if (htmlCheckPattern.matcher(mEvent.description).find()) {
            mEventCard.descriptionView.setText(Html.fromHtml(mEvent.description));
        } else {
            mEventCard.descriptionView.setText(mEvent.description);
        }

        return rootView;
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
        BaseActivity baseActivity = (BaseActivity)getActivity();
        baseActivity.shareEvent(mEventCard.rootView, mEvent);
        mEventCard.descriptionView.setVisibility(View.VISIBLE);
    }

    private void showDirections() {
        BaseActivity baseActivity = (BaseActivity)getActivity();
        baseActivity.reportActionToAnalytics("showDirections");

        Uri locationUri = Uri.parse("geo:0,0?q=" +
                mEvent.location.latitude + "," + mEvent.location.longitude +
                " (" + mEvent.title + ")");
        Intent intent = new Intent(Intent.ACTION_VIEW, locationUri);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // No activity to open maps.
            Toast.makeText(baseActivity, R.string.no_map_app, Toast.LENGTH_SHORT).show();
        }
    }

    private void addToCalendar() {
        BaseActivity baseActivity = (BaseActivity)getActivity();
        baseActivity.reportActionToAnalytics("addToCalendar");

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
            Toast.makeText(getActivity(), R.string.no_cal_app, Toast.LENGTH_SHORT).show();
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
            bgView = (ImageView) rootView.findViewById(R.id.event_bg);
            descriptionView = (TextView) rootView.findViewById(R.id.event_description);
        }
    }

}
