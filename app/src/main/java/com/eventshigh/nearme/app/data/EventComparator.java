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
    private final Map<String, Double> eventToDistanceMap = new HashMap<>();

    public EventComparator(LatLng userLocation, EventsMarkerManager eventsMarkerManager) {
        this.userLocation = userLocation;
        this.eventsMarkerManager = eventsMarkerManager;
    }

    @Override
    public int compare(Event lhs, Event rhs) {
        return Double.compare(
                weightedScore(rhs, eventsMarkerManager.isFavourite(rhs.id), userLocation, eventToDistanceMap),
                weightedScore(lhs, eventsMarkerManager.isFavourite(lhs.id), userLocation, eventToDistanceMap)
        );
    }

    // Find the distance of events from user's position with weight for popular events.
    // If event has e**N users going, we reduce 500*N meters from its distance.
    private static double weightedScore(Event event, boolean isFavourite, LatLng userLocation,
                                        Map<String, Double> eventToDistanceMap) {
        Double result = eventToDistanceMap.get(event.id);
        if (result != null) {
            return result;
        }

        EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
        float distance = LocationUtils.distanceInMeters(event.location, userLocation);
        boolean isPastEvent = eventTime == null || eventTime.time == null || event.eventTimings[0] < System.currentTimeMillis();

        return event.uberScore + (isFavourite ? 20 : 0) - (isPastEvent ? 20 : 0)
                - Math.min(30, Math.pow(1.4, distance / 1000));
    }
}
