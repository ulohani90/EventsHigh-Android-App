package com.eventshigh.nearme.app.activity;

import android.app.Fragment;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.DownloadImageTask;
import com.eventshigh.nearme.app.utils.Utils;

/**
 * A fragment representing a single Event detail screen.
 * This fragment is either contained in a {@link EventListActivity}
 * in two-pane mode (on tablets) or a {@link EventDetailActivity}
 * on handsets.
 */
public class EventDetailFragment extends Fragment {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "event_info";

    private Event mEvent;
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
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mEvent = getArguments().getParcelable(ARG_ITEM_ID);
        }
        setHasOptionsMenu(mEvent != null);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.fragment_detail, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            mEventCard.descriptionView.setVisibility(View.GONE);
            EventListActivity.shareEvent(getActivity(), mEventCard.rootView, mEvent);
            mEventCard.descriptionView.setVisibility(View.VISIBLE);
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

        // Set Venue.
        mEventCard.venueView.setText(mEvent.address == null ? mEvent.venue : mEvent.address);

        // Set time.
        mEventCard.timeView.setVisibility(mEvent.startTime == null ? View.GONE: View.VISIBLE);
        mEventCard.timeView.setText(Utils.getEventTime(mEvent));

        // Set Num people Interested
        Resources res = getResources();
        String text = res.getQuantityString(R.plurals.people_interested,
                mEvent.numPeopleInterested, mEvent.numPeopleInterested);
        mEventCard.numPeopleInterestedView.setText(text);

        // Set Image
        if (mEvent.img_url == null) {
            mEventCard.bgView.setVisibility(View.GONE);
        } else {
            new DownloadImageTask((ImageView) rootView.findViewById(R.id.event_bg)).execute(mEvent.img_url);
        }

        // Set description.
        if (mEvent.description.contains("<p") || mEvent.description.contains("<P")
                || mEvent.description.contains("<a") ||  mEvent.description.contains("<A")
                || mEvent.description.contains("<span") ||  mEvent.description.contains("<SPAN")
           ) {
            mEventCard.descriptionView.setText(Html.fromHtml(mEvent.description));
        } else {
            mEventCard.descriptionView.setText(mEvent.description);
        }

        return rootView;
    }

    private static class EventCard {
        private final View rootView;
        private final TextView titleView;
        private final ImageView recommendedImageView;
        private final TextView venueView;
        private final TextView timeView;
        private final TextView numPeopleInterestedView;
        private final ImageView bgView;
        private final TextView descriptionView;

        private EventCard(View rootView) {
            this.rootView = rootView;
            titleView = (TextView) rootView.findViewById(R.id.event_title);
            recommendedImageView = (ImageView) rootView.findViewById(R.id.eh_recommend_banner);
            venueView = (TextView) rootView.findViewById(R.id.event_venue);
            timeView = (TextView) rootView.findViewById(R.id.event_time);
            numPeopleInterestedView = (TextView) rootView.findViewById(R.id.num_people_interested);
            bgView = (ImageView) rootView.findViewById(R.id.event_bg);
            descriptionView = (TextView) rootView.findViewById(R.id.event_description);
        }
    }

}
