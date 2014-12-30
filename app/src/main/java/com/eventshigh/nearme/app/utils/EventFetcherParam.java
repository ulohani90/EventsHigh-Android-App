package com.eventshigh.nearme.app.utils;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.data.City;
import com.google.android.gms.maps.model.LatLng;

/**
 * Simplified Parameter for fetching events. The current Fetcher supports fetching
 * events for a day and give city.
 */
public class EventFetcherParam implements Parcelable {

    @Nullable public City city;
    @Nullable public LatLng location;
    public int day;
    public String query;

    public EventFetcherParam(@Nullable LatLng location, int day, String query) {
        this.day = day;
        this.query = query;
        changeLocation(location);
    }

    public boolean changeLocation(@Nullable LatLng location) {
        this.location = location;
        City oldCity = city;
        city = City.getCity(location);
        return (oldCity == null && city == null) || (oldCity != null && oldCity.equals(city));
    }

    @Override
    public String toString() {
        return EventFetcherParam.class.getSimpleName() +
                " (City: " + city +
                ", day: " + day +
                ", query: '" + query + "')";
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object another) {
        return another != null && another instanceof EventFetcherParam &&
            toString().equals(another.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(location, flags);
        dest.writeInt(day);
        dest.writeString(query);
    }

    // This is used to regenerate your object. All Parcelables must have
    // a CREATOR that implements these two methods
    public static final Parcelable.Creator<EventFetcherParam> CREATOR =
            new Parcelable.Creator<EventFetcherParam>() {
        public EventFetcherParam createFromParcel(Parcel in) {
            LatLng location = in.readParcelable(LatLng.class.getClassLoader());
            int day = in.readInt();
            String query = in.readString();

            return new EventFetcherParam(location, day, query);
        }

        public EventFetcherParam[] newArray(int size) {
            return new EventFetcherParam[size];
        }
    };
}
