package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;

/**
 * Simplified parameters for fetching events. The current Fetcher supports fetching
 * events for a city and optionally filtered by query. Query can also be a date.
 */
public class EventsContext implements Parcelable {
    private static final String DATE_FILTER_FORMAT = "%4d-%02d-%02d";

    @Nullable public City city;
    @Nullable public LatLng location;
    public String query;
    public String dateFilter;

    public EventsContext(@Nullable LatLng location, String query) {
        this.query = query;
        changeLocation(location);
        dateFilter = "";
    }

    public EventsContext(EventsContext other) {
        this(other.location, other.query);
        this.dateFilter = other.dateFilter;
    }

    public boolean changeLocation(@Nullable LatLng location) {
        this.location = location;
        City oldCity = city;
        city = City.getCity(location);
        return (oldCity == null && city == null) || (oldCity != null && oldCity.equals(city));
    }

    public void setDateFilter(Calendar calendar) {
        dateFilter = formatDateFilter(calendar);
    }

    public static String formatDateFilter(Calendar calendar) {
        return String.format(DATE_FILTER_FORMAT, calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public String toString() {
        return (query + " events" +
                (city == null ? " near you" : " in " + city.toString().toLowerCase()) +
                (dateFilter.isEmpty() ? "" : " on " + dateFilter)).trim();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object another) {
        return another != null && another instanceof EventsContext &&
            toString().equals(another.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(location, flags);
        dest.writeString(query);
        dest.writeString(dateFilter);
    }

    // This is used to regenerate your object. All Parcelables must have
    // a CREATOR that implements these two methods
    public static final Parcelable.Creator<EventsContext> CREATOR =
            new Parcelable.Creator<EventsContext>() {
        public EventsContext createFromParcel(Parcel in) {
            LatLng location = in.readParcelable(LatLng.class.getClassLoader());
            String query = in.readString();
            EventsContext context =  new EventsContext(location, query);
            context.dateFilter = in.readString();
            return context;
        }

        public EventsContext[] newArray(int size) {
            return new EventsContext[size];
        }
    };
}
