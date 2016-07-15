package com.eventshigh.nearme.app.data;

import java.util.Comparator;

/**
 * Created by umesh on 13/07/16.
 */
public class EventScoreComparator implements Comparator<Event> {


    @Override
    public int compare(Event lhs, Event rhs) {
        return Float.compare(lhs.uberScore, rhs.uberScore);
    }
}
