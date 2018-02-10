package com.eventshigh.nearme.app.data;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by umesh on 27/11/17.
 */

public class EventSpecialFilterComparator implements Comparator<Event> {

    ArrayList<String> filterNames;

    public EventSpecialFilterComparator(ArrayList<String> filterNames) {
        this.filterNames = filterNames;

    }

    @Override
    public int compare(Event lhs, Event rhs) {

        return Float.compare(getMaxUberScoreValue(rhs), getMaxUberScoreValue(lhs));
    }

    public Float getMaxUberScoreValue(Event event) {
        float maxScore = 0;
        if (event.nyeUberScoresMap != null) {
            for (String filterName : filterNames) {
                if (event.nyeUberScoresMap.containsKey(filterName)) {
                    if (event.nyeUberScoresMap.get(filterName) > maxScore) {
                        maxScore = event.nyeUberScoresMap.get(filterName);
                    }
                }
            }

        }
        if (maxScore == 0) {
            maxScore = event.uberScore;
        }
        return maxScore;

    }
}
