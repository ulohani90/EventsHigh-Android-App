package com.eventshigh.nearme.app.data;

import java.util.Comparator;

/**
 * Created by umesh on 13/07/16.
 */
public class EventScoreComparator implements Comparator<Event> {

    public static final int SCORE_TYPE_UBER_SCORE = 1;
    public static final int SCORE_TYPE_NYE_UBER_SCORE = 2;
    public static final int SCORE_TYPE_OUTDOOR_UBER_SCORE = 3;

    int scoreType;

    public EventScoreComparator(int scoreType) {
        this.scoreType = scoreType;
    }


    @Override
    public int compare(Event lhs, Event rhs) {
        if (scoreType == SCORE_TYPE_UBER_SCORE) {
            return Float.compare(rhs.uberScore, lhs.uberScore);
        } else if (scoreType == SCORE_TYPE_NYE_UBER_SCORE) {
            return Double.compare(rhs.nyeUberScore, lhs.nyeUberScore);
        } else {
            return Double.compare(rhs.outdoorsUberScore, lhs.outdoorsUberScore);
        }
    }
}
