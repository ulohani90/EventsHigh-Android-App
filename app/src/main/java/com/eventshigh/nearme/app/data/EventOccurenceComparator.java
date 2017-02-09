package com.eventshigh.nearme.app.data;

import java.util.Comparator;

/**
 * Created by umesh on 06/02/17.
 */

public class EventOccurenceComparator implements Comparator<Event> {
    @Override
    public int compare(Event lhs, Event rhs) {

        if (lhs.eventTimings.get(0) < rhs.eventTimings.get(0))
            return -1;
        if (rhs.eventTimings.get(0) > rhs.eventTimings.get(0))
            return 1;

        return 0;
    }
}
