package com.eventshigh.nearme.app.data;

import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.utils.LocationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.Comparator;

/**
 * Created by umesh on 13/07/16.
 */
public class EventDistanceComparator implements Comparator<Event> {

    @Nullable
    private final LatLng userLocation;

    public EventDistanceComparator(@Nullable LatLng userLocation) {
        this.userLocation = userLocation;
    }

    @Override
    public int compare(Event lhs, Event rhs) {
        if (lhs.location != null && rhs.location != null) {
            return Double.compare(LocationUtils.distanceInKM(lhs.location, userLocation), LocationUtils.distanceInKM(rhs.location, userLocation));
        } else if (lhs.location != null && rhs.location == null) {
            return -1;
        } else if (lhs.location == null && rhs.location != null) {
            return 1;
        } else {
            return 0;
        }
    }
}
