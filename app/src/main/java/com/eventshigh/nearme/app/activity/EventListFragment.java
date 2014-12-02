package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.DownloadImageTask;
import com.eventshigh.nearme.app.utils.Utils;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A list fragment representing a list of Events. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link EventDetailFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class EventListFragment extends ListFragment {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    /**
     * Adapter used to populate the event list.
     */
    private EventListAdapter mEventsListAdapter;

    /**
     * Are we showing two pane layout ?
     */
    private boolean isTwoPane = false;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(Event event);

        /**
         * Share the Event.
         */
        public void shareEvent(View eventView, Event event);
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public EventListFragment() {
    }


    // ***********************
    // Fragment lifecycle  Methods
    // ***********************

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEventsListAdapter = new EventListAdapter();
        setListAdapter(mEventsListAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            mActivatedPosition = savedInstanceState.getInt(STATE_ACTIVATED_POSITION);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    // ***********************
    // Helper methods used by parent activity.
    // ***********************

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        mActivatedPosition = position;

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        mCallbacks.onItemSelected(mEventsListAdapter.getItem(position));
    }

    public void updateNewEvents(List<Event> events) {
        mEventsListAdapter.clear();
        mEventsListAdapter.addAll(events);
    }

    public void updateListingForUserLocation(final LatLng userLocation) {
        // Sort the events based on popularity and distance from user location.
        // If event has e**N users going, we reduce 500*N meters from its distance.
        final Map<String, Double> eventToDistanceMap = new HashMap<String, Double>(mEventsListAdapter.getCount());
        mEventsListAdapter.sort(new Comparator<Event>() {
            @Override
            public int compare(Event lhs, Event rhs) {
                return Double.compare(
                        weightedDistance(lhs, userLocation, eventToDistanceMap),
                        weightedDistance(rhs, userLocation, eventToDistanceMap)
                );
            }
        });

        if (mEventsListAdapter.isEmpty()) {
            return;
        }

        if (mActivatedPosition == ListView.INVALID_POSITION ||
                mActivatedPosition >= mEventsListAdapter.getCount()) {
            mActivatedPosition = 0;
        }

        getListView().smoothScrollToPosition(mActivatedPosition);
        if (isTwoPane) {
            getListView().setItemChecked(mActivatedPosition, true);
            onListItemClick(getListView(), null, mActivatedPosition, 0);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setIsTwoPane(boolean isTwoPane) {
        this.isTwoPane = isTwoPane;
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(isTwoPane
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }


    // ***********************
    // Helper methods.
    // ***********************
    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(Event event) {
        }

        @Override
        public void shareEvent(View eventView, Event event) {
        }
    };

    private class EventListAdapter extends ArrayAdapter<Event> {

        private EventListAdapter() {
            super(getActivity(), R.layout.list_item_event, R.id.event_title);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Build the view, reuse existing if possible.
            final View view = convertView == null ?
                    getActivity().getLayoutInflater().inflate(R.layout.list_item_event, parent, false) :
                    convertView;
            EventCard eventCard = new EventCard(view);
            final Event event = getItem(position);

            // Set the background image.
            eventCard.bgView.setImageResource(R.drawable.eh_default);
            eventCard.bgView.setLayoutParams(new FrameLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT, findOptimalHeight(parent)));
            new DownloadImageTask(eventCard.bgView).execute(event.img_url);

            // Set the title, time etc.
            eventCard.titleView.setText(event.title);
            eventCard.timeView.setText(Utils.getEventTime(event));
            eventCard.numPeopleInterestedView.setText(
                    Integer.toString(event.numPeopleInterested));

            // Set the venue
            if (event.venue == null) {
                eventCard.venueView.setVisibility(View.INVISIBLE);
            } else {
                eventCard.venueView.setVisibility(View.VISIBLE);
                eventCard.venueView.setText(event.venue);
            }

            // Check if its recommended event.
            eventCard.recommendedImageView.setVisibility(event.ehRecommended ? View.VISIBLE :
                    View.INVISIBLE);

            // Set share view.
            eventCard.shareView.setVisibility(isTwoPane ?  View.GONE : View.VISIBLE);
            eventCard.shareView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mCallbacks.shareEvent(view, event);
                }
            });

            return view;
        }
    }

    private static class EventCard {
        private final ImageView bgView;
        private final ImageView shareView;
        private final ImageView recommendedImageView;
        private final TextView titleView;
        private final TextView venueView;
        private final TextView timeView;
        private final TextView numPeopleInterestedView;

        private EventCard(View cardView) {
            bgView = (ImageView) cardView.findViewById(R.id.event_bg);
            shareView = (ImageView) cardView.findViewById(R.id.event_share);
            recommendedImageView = (ImageView) cardView.findViewById(R.id.event_recommended);
            titleView = (TextView) cardView.findViewById(R.id.event_title);
            venueView = (TextView) cardView.findViewById(R.id.event_venue);
            timeView = (TextView) cardView.findViewById(R.id.event_time);
            numPeopleInterestedView = (TextView) cardView.findViewById(R.id.num_people_interested);
        }
    }

    // Find the distance of events from user's position with weight for popular events.
    // If event has e**N users going, we reduce 500*N meters from its distance.
    private static double weightedDistance(Event event, LatLng userLocation, Map<String, Double> eventToDistanceMap) {
        Double result = eventToDistanceMap.get(event.id);
        if (result != null) {
            return result;
        }

        double distance = SphericalUtil.computeDistanceBetween(event.location, userLocation);
        double weight = (event.numPeopleInterested > 0 ? Math.log(event.numPeopleInterested) * 500 : 0)
                + (event.ehRecommended ? 1000 : 0) ;
        double weightedDistance = distance - weight;
        eventToDistanceMap.put(event.id, weightedDistance);
        return weightedDistance;
    }


    // What would be optimal height for event card? Width of event card is same as
    // width of parent. In landscape mode, we want event card height to match the parent
    // height so that one event card is fully visible. In portrait mode, we want event cards
    // to be 9:16 shape.
    private static int findOptimalHeight(ViewGroup parent) {
        int parentWidth = parent.getMeasuredWidth();
        int parentHeight = parent.getMeasuredHeight();

        return Math.min(9 * parentWidth / 16, parentHeight);
    }
}
