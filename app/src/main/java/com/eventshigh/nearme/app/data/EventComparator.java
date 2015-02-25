package com.eventshigh.nearme.app.data;

import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DateTimeUtils.EventTime;
import com.eventshigh.nearme.app.utils.LocationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link java.util.Comparator} for {@link com.eventshigh.nearme.app.data.Event} class which
 * sorts the events based on user location and popularity of event. If event has e**N users going,
 * we reduce 500*N meters from its distance.
 */
public class EventComparator implements Comparator<Event> {
    private final EventsMarkerManager eventsMarkerManager;
    private final LatLng userLocation;
    private final Map<String, Double> eventScoreMap = new HashMap<>();

    public EventComparator(LatLng userLocation, EventsMarkerManager eventsMarkerManager) {
        this.userLocation = userLocation;
        this.eventsMarkerManager = eventsMarkerManager;
    }

    @Override
    public int compare(Event lhs, Event rhs) {
        return Double.compare(weightedScore(rhs), weightedScore(lhs));
    }

    // Find the weighted score of event -- we take uber score and then put some penalty
    // for distance and if its a past event.
    private double weightedScore(Event event) {
        Double result = eventScoreMap.get(event.id);
        if (result == null) {
            double distancePenalty = event.location == null ? 30 :
                    Math.min(30, Math.pow(1.4,
                            LocationUtils.distanceInMeters(event.location, userLocation) / 1000));
            EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
            double timePenalty = eventTime == null || eventTime.time == null ? 20 :
                    (event.eventTimings[0] < System.currentTimeMillis() ?
                            2.31e-7 * (System.currentTimeMillis() - event.eventTimings[0]) : 0);

            result = event.uberScore + (eventsMarkerManager.isFavourite(event.id) ? 20 : 0)
                    - timePenalty - distancePenalty;
            eventScoreMap.put(event.id, result);
        }
        return result;
    }
}
