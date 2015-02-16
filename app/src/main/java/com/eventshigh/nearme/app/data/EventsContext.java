package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;
import java.util.Date;

/**
 * Simplified parameters for fetching events. The current Fetcher supports fetching
 * events for a city and optionally filtered by query. Query can also be a date.
 */
public class EventsContext implements Parcelable {
    private static final String DATE_FILTER_FORMAT = "%4d-%02d-%02d";

    @Nullable public City city;
    @Nullable public LatLng location;
    public String query;
    public String tabName;
    public String dateFilter;

    public EventsContext(@Nullable LatLng location, String query) {
        this.query = query;
        changeLocation(location);
        tabName = "";
        dateFilter = "";
    }

    public boolean changeLocation(@Nullable LatLng location) {
        this.location = location;
        City oldCity = city;
        city = City.getCity(location);
        return (oldCity == null && city == null) || (oldCity != null && oldCity.equals(city));
    }

    public @Nullable Date getDateFilter() {
        return DateTimeUtils.parseDateSafe(dateFilter);
    }

    public void setDateFilter(Calendar calendar) {
        setDateFilter(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    public void setDateFilter(int year, int monthOfYear, int dayOfMonth) {
        dateFilter = String.format(DATE_FILTER_FORMAT, year, monthOfYear + 1, dayOfMonth);
    }

    @Override
    public String toString() {
        return (tabName + " " + query + " events" +
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
        dest.writeString(tabName);
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
            context.tabName = in.readString();
            context.dateFilter = in.readString();
            return context;
        }

        public EventsContext[] newArray(int size) {
            return new EventsContext[size];
        }
    };
}
