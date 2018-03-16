package com.eventshigh.nearme.app.data;

import java.util.Comparator;

/**
 * Created by umesh on 13/07/16.
 */
public class EventScoreComparator implements Comparator<Event> {

    public static final int SCORE_TYPE_UBER_SCORE = 1;
    public static final int SCORE_TYPE_NYE_UBER_SCORE = 2;
    public static final int SCORE_TYPE_OUTDOOR_UBER_SCORE = 3;

    String filterName;

    public EventScoreComparator(String filterName) {
        this.filterName = filterName;
    }


    @Override
    public int compare(Event lhs, Event rhs) {
        return Float.compare(getUberScore(rhs), getUberScore(lhs));
    }


    public Float getUberScore(Event event) {
        if (event.nyeUberScoresMap != null && event.nyeUberScoresMap.containsKey(filterName)) {
           // System.out.println("Filter name " + filterName + " Filter Score " + event.nyeUberScoresMap.get(filterName));
            return event.nyeUberScoresMap.get(filterName);
        }
        //System.out.println("Filter name " + filterName + " Uber Score " + event.uberScore);
        return event.uberScore;

    }
}
