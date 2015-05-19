package com.eventshigh.nearme.app.data;

import android.support.annotation.Nullable;

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
    @Nullable  private final LatLng userLocation;
    private final Map<String, Double> eventScoreMap = new HashMap<>();

    public EventComparator(@Nullable LatLng userLocation) {
        this.userLocation = userLocation;
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
            double distancePenalty = userLocation == null ? 0 :
                (event.location == null ? 30 :
                    Math.min(30, Math.pow(1.4,
                            LocationUtils.distanceInKM(event.location, userLocation))));

            EventTime eventTime = DateTimeUtils.getEventTime(event, 0);
            double timePenalty = eventTime == null || eventTime.time == null ? 30 :
                    Math.min(30, 1e-7 * Math.abs(System.currentTimeMillis() - event.eventTimings[0]));

            result = event.uberScore - timePenalty - distancePenalty;
            eventScoreMap.put(event.id, result);
        }
        return result;
    }
}
