package com.eventshigh.nearme.app.data;

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
        float timeWeight = event.eventTimings.length > 0 ? 2000 : 10000;
        for (long eventTime : event.eventTimings) {
            if (eventTime > System.currentTimeMillis()) {
                timeWeight = 0.000025f * (eventTime - System.currentTimeMillis());
                break;
            }
        }

        double recommendedDiscount = (event.numPeopleInterested > 0 ? Math.log(event.numPeopleInterested) * 500 : 0)
                + (event.ehRecommended || isFavourite ? 2000 : 0);
        double weightedDistance = distance + timeWeight - recommendedDiscount;
        eventToDistanceMap.put(event.id, weightedDistance);
        return weightedDistance;
    }
}
