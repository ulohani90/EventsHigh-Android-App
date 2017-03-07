package com.eventshigh.nearme.app.data;

import java.util.Comparator;

/**
 * Created by umesh on 07/03/17.
 */

public class EventTimeComparator implements Comparator<Event> {
    @Override
    public int compare(Event lhs, Event rhs) {

        if (lhs.isEvergreen) {
            return -1;
        }
        if (rhs.isEvergreen) {
            return 1;
        }
        int indexForCheckLhs = 0, indexForCheckRhs = 0;
        if (lhs.eventTimings != null) {
            for (int i = 0; i < lhs.eventTimings.size(); i++) {
                if (lhs.eventTimings.get(i) >= System.currentTimeMillis()) {
                    indexForCheckLhs = i;
                    break;
                }
            }
        }
        if (rhs.eventTimings != null) {
            for (int i = 0; i < rhs.eventTimings.size(); i++) {
                if (rhs.eventTimings.get(i) >= System.currentTimeMillis()) {
                    indexForCheckRhs = i;
                    break;
                }
            }
        }
        if (lhs.eventTimings.get(indexForCheckLhs) != null && rhs.eventTimings.get(indexForCheckRhs) != null && lhs.eventTimings.get(indexForCheckLhs) < rhs.eventTimings.get(indexForCheckRhs))
            return -1;
        if (lhs.eventTimings.get(indexForCheckLhs) != null && rhs.eventTimings.get(indexForCheckRhs) != null && lhs.eventTimings.get(indexForCheckLhs) > rhs.eventTimings.get(indexForCheckRhs))
            return 1;

        return 0;
    }
}
