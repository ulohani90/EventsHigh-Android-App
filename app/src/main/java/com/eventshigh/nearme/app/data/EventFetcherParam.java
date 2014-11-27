package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.model.LatLng;

/**
 * Simplified Parameter for fetching events. The current Fetcher supports fetching
 * events for a day and give city.
 */
public class EventFetcherParam implements Parcelable {

    public final City city;
    public final LatLng location;
    public final int day;

    public EventFetcherParam(City city, LatLng location, int day) {
        this.city = city;
        this.location = location;
        this.day = day;
    }

    @Override
    public String toString() {
        return EventFetcherParam.class.getSimpleName() + " (City: " + city + ", day=" + day + ")";
    }

    @Override
    public boolean equals(Object another) {
        return another != null && another instanceof EventFetcherParam &&
                city.equals(((EventFetcherParam)another).city) &&
                day == ((EventFetcherParam)another).day;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(city.toString());
        dest.writeDouble(location.latitude);
        dest.writeDouble(location.longitude);
        dest.writeInt(day);
    }

    // This is used to regenerate your object. All Parcelables must have
    // a CREATOR that implements these two methods
    public static final Parcelable.Creator<EventFetcherParam> CREATOR =
            new Parcelable.Creator<EventFetcherParam>() {
        public EventFetcherParam createFromParcel(Parcel in) {
            String city = in.readString();
            double lat = in.readDouble();
            double lng = in.readDouble();
            int day = in.readInt();

            return new EventFetcherParam(City.valueOf(city), new LatLng(lat,lng), day);
        }

        public EventFetcherParam[] newArray(int size) {
            return new EventFetcherParam[size];
        }
    };
}
