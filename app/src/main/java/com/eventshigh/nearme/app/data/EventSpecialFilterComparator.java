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
            if (maxScore == 0) {
                if (scoreType == EventScoreComparator.SCORE_TYPE_OUTDOOR_UBER_SCORE) {
                    if (event.nyeUberScoresMap.containsKey("Camping / Outdoors")) {
                        maxScore = event.nyeUberScoresMap.get("Camping / Outdoors");
                    } else if (event.outdoorUberScore != 0) {
                        maxScore = event.outdoorUberScore;
                    } else {
                        maxScore = event.uberScore;
                    }
                } else {
                    if (event.nyeUberScoresMap.containsKey("Default")) {
                        maxScore = event.nyeUberScoresMap.get("Default");
                    } else if (event.nyeUberScore != 0) {
                        maxScore = event.nyeUberScore;
                    } else {
                        maxScore = event.uberScore;
                    }
                }

            }
        } else if (scoreType == EventScoreComparator.SCORE_TYPE_OUTDOOR_UBER_SCORE && event.outdoorUberScore != 0) {
            maxScore = event.outdoorUberScore;
        } else if (scoreType == EventScoreComparator.SCORE_TYPE_NYE_UBER_SCORE && event.nyeUberScore != 0) {
            maxScore = event.nyeUberScore;
        } else {
            maxScore = event.uberScore;
        }
        return maxScore;

    }
}
