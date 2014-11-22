package com.eventshigh.nearme.app.data;

/**
 * Simplified Parameter for fetching events. The current Fetcher supports fetching
 * events for a day and give city.
 */
public class EventFetcherParam {

    public final City city;
    public final int day;

    public EventFetcherParam(City city, int day) {
        this.city = city;
        this.day = day;
    }

    @Override
    public String toString() {
        return EventFetcherParam.class.getSimpleName() + " (City: " + city + ", day=" + day + ")";
    }
}
