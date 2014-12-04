package com.eventshigh.nearme.app.activity;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.eventshigh.nearme.app.data.Event;
import com.eventshigh.nearme.app.utils.EventListAdapter;
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

        mEventsListAdapter = new EventListAdapter((BaseActivity)getActivity(), !isTwoPane);
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
        mEventsListAdapter.setShowShareIcon(!isTwoPane);

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
    };

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
}
