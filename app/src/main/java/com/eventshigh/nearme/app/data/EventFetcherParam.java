package com.eventshigh.nearme.app.data;

import com.google.android.gms.maps.model.LatLng;

public class EventFetcherParam {

    public final LatLng location;
    public final int day;

    public EventFetcherParam(LatLng location, int day) {
        this.location = location;
        this.day = day;
    }
}
