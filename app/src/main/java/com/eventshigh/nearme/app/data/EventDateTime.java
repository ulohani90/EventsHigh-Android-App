package com.eventshigh.nearme.app.data;

/**
 * Created by umesh on 05/12/17.
 */

public class EventDateTime {

    private long startDateTime;
    private long endDateTime;

    public EventDateTime(long startDateTime, long endDateTime) {
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        System.out.println("StarDateTime :: " + startDateTime + " AND endDateTime::" + endDateTime);
    }

    public long getStartDateTime() {
        return startDateTime;
    }

    public long getEndDateTime() {
        return endDateTime;
    }

    @Override
    public boolean equals(Object obj) {
        EventDateTime dateTimeObj = (EventDateTime) obj;
        return (dateTimeObj.startDateTime == this.startDateTime && dateTimeObj.endDateTime == this.endDateTime);
    }
}
