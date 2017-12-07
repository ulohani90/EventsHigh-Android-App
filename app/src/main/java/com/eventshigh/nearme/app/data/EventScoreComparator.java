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
            return Float.compare(getUberScore(rhs), getUberScore(lhs));
        } else {
            return Float.compare(getUberScore(rhs), getUberScore(lhs));
        }
    }


    public Float getUberScore(Event event) {
        if (event.nyeUberScoresMap != null) {
            if (scoreType == SCORE_TYPE_OUTDOOR_UBER_SCORE) {
                if (event.nyeUberScoresMap.containsKey("Camping / Outdoors")) {
                    return event.nyeUberScoresMap.get("Camping / Outdoors");
                } else if (event.outdoorUberScore != 0) {
                    return event.outdoorUberScore;
                } else {
                    return (event.uberScore);
                }
            } else if (scoreType == SCORE_TYPE_NYE_UBER_SCORE) {
                if (event.nyeUberScoresMap.containsKey("Default")) {
                    return event.nyeUberScoresMap.get("Default");
                } else if (event.nyeUberScore != 0) {
                    return (event.nyeUberScore);
                } else {
                    return event.uberScore;
                }
            } else {
                return (event.uberScore);
            }

        } else {
            return (event.uberScore);
        }
    }
}
