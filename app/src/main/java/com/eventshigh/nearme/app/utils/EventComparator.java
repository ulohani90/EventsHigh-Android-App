package com.eventshigh.nearme.app.utils;

import com.eventshigh.nearme.app.data.Event;
import com.google.android.gms.maps.model.LatLng;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link java.util.Comparator} for {@link com.eventshigh.nearme.app.data.Event} class which sorts the events based
 * on user location and popularity of event. If event has e**N users going, we reduce
 * 500*N meters from its distance.
 */
public class EventComparator implements Comparator<Event> {
    private final LatLng userLocation;
    private final Map<String, Double> eventToDistanceMap = new HashMap<>();

    public EventComparator(LatLng userLocation) {
        this.userLocation = userLocation;
    }

    @Override
    public int compare(Event lhs, Event rhs) {
        return Double.compare(
                weightedDistance(lhs, userLocation, eventToDistanceMap),
                weightedDistance(rhs, userLocation, eventToDistanceMap)
        );
    }

    // Find the distance of events from user's position with weight for popular events.
    // If event has e**N users going, we reduce 500*N meters from its distance.
    private static double weightedDistance(Event event, LatLng userLocation, Map<String, Double> eventToDistanceMap) {
        Double result = eventToDistanceMap.get(event.id);
        if (result != null) {
            return result;
        }

        float distance = LocationUtils.distanceInMeters(event.location, userLocation);
        double weight = (event.numPeopleInterested > 0 ? Math.log(event.numPeopleInterested) * 500 : 0)
                + (event.ehRecommended ? 1000 : 0) ;
        double weightedDistance = distance - weight;
        eventToDistanceMap.put(event.id, weightedDistance);
        return weightedDistance;
    }
}
