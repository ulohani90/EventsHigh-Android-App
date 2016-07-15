package com.eventshigh.nearme.app.data;

import java.util.Comparator;

/**
 * Created by umesh on 13/07/16.
 */
public class EventPriceComparator implements Comparator<Event> {


    @Override
    public int compare(Event lhs, Event rhs) {
        return Double.compare(lhs.getMinPrice(), rhs.getMinPrice());
    }
}
