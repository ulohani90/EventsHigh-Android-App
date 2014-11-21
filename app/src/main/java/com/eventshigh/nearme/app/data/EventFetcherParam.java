package com.eventshigh.nearme.app.data;

import com.google.android.gms.maps.model.LatLng;

/**
 * Simplified Parameter for fetching events. The current Fetcher supports fetching
 * events for a day and give city.
 */
public class EventFetcherParam {

    public final LatLng location;
    public final int day;

    public EventFetcherParam(LatLng location, int day) {
        this.location = location;
        this.day = day;
    }
}
