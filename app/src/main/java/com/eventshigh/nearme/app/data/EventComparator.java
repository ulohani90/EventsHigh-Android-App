package com.eventshigh.nearme.app.data;

import com.eventshigh.nearme.app.utils.DateTimeUtils;
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
        if (lhs.eventTimings.length > 0 && rhs.eventTimings.length > 0) {
            int result = DateTimeUtils.getEventDate(lhs, 0).compareTo(
                    DateTimeUtils.getEventDate(rhs, 0));
            if (result != 0) {
                return result;
            }
        }

        if (lhs.eventTimings.length == 0) {
            return 1;
        }

        if (rhs.eventTimings.length == 0) {
            return -1;
        }

        return Double.compare(
                weightedDistance(lhs, eventsMarkerManager.isFavourite(lhs.id), userLocation, eventToDistanceMap),
                weightedDistance(rhs, eventsMarkerManager.isFavourite(rhs.id), userLocation, eventToDistanceMap)
        );
    }

    // Find the distance of events from user's position with weight for popular events.
    // If event has e**N users going, we reduce 500*N meters from its distance.
    private static double weightedDistance(Event event, boolean isFavourite, LatLng userLocation,
                                           Map<String, Double> eventToDistanceMap) {
        Double result = eventToDistanceMap.get(event.id);
        if (result != null) {
            return result;
        }

        float distance = LocationUtils.distanceInMeters(event.location, userLocation);
        double weight = (event.numPeopleInterested > 0 ? Math.log(event.numPeopleInterested) * 500 : 0)
                + (event.ehRecommended || isFavourite ? 1000 : 0) ;
        double weightedDistance = distance - weight;
        eventToDistanceMap.put(event.id, weightedDistance);
        return weightedDistance;
    }
}
