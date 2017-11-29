package com.eventshigh.nearme.app.data.stream;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by umesh on 22/11/17.
 */

public class ZoneLocalityMapObject implements Parcelable{
    private String zone;

    private ArrayList<String> localities;

    public ZoneLocalityMapObject(String zone, ArrayList<String> localities) {
        this.zone = zone;
        this.localities = localities;
    }

    protected ZoneLocalityMapObject(Parcel in) {
        zone = in.readString();
        localities = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(zone);
        dest.writeStringList(localities);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ZoneLocalityMapObject> CREATOR = new Creator<ZoneLocalityMapObject>() {
        @Override
        public ZoneLocalityMapObject createFromParcel(Parcel in) {
            return new ZoneLocalityMapObject(in);
        }

        @Override
        public ZoneLocalityMapObject[] newArray(int size) {
            return new ZoneLocalityMapObject[size];
        }
    };

    public ArrayList<String> getLocalities() {
        return localities;
    }

    public String getZone() {
        return zone;
    }

    public void setLocalities(ArrayList<String> localities) {
        this.localities = localities;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
