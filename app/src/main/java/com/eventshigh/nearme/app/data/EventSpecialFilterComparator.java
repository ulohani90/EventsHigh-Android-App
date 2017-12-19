package com.eventshigh.nearme.app.data;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by umesh on 27/11/17.
 */

public class EventSpecialFilterComparator implements Comparator<Event> {

    ArrayList<String> filterNames;
    int scoreType;

    public EventSpecialFilterComparator(ArrayList<String> filterNames, int scoreType) {
        this.filterNames = filterNames;
        this.scoreType = scoreType;
    }

    @Override
    public int compare(Event lhs, Event rhs) {
        if ((lhs.id.equalsIgnoreCase("abaf7c81b606893c45d841b9d5db9256") && rhs.id.equalsIgnoreCase("2ee5a40e210863775fa5fd64ab57f3fd"))
                ||
                (rhs.id.equalsIgnoreCase("abaf7c81b606893c45d841b9d5db9256") && lhs.id.equalsIgnoreCase("2ee5a40e210863775fa5fd64ab57f3fd"))) {
            System.out.println("Stop Here");
        }
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
