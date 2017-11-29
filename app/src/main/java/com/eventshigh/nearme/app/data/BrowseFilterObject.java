package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by umesh on 24/11/17.
 */

public class BrowseFilterObject implements Parcelable {

    private ArrayList<BrowseFilterAttributes> filterAttributes;

    private ArrayList<DisplayZoneObject> displayZones;

    private ArrayList<BrowseFilterAttributes> dateFilters;

    public BrowseFilterObject() {

    }

    protected BrowseFilterObject(Parcel in) {
        filterAttributes = new ArrayList<>();
        in.readTypedList(filterAttributes, BrowseFilterAttributes.CREATOR);
        displayZones = new ArrayList<>();
        in.readTypedList(displayZones, DisplayZoneObject.CREATOR);
        dateFilters = new ArrayList<>();
        in.readTypedList(dateFilters, BrowseFilterAttributes.CREATOR);
    }

    public static BrowseFilterObject parseFromJson(JSONObject jsonObject) {
        if (jsonObject.has("filters")) {
            JSONObject filterObj = jsonObject.optJSONObject("filters");
            BrowseFilterObject obj = new BrowseFilterObject();
            String city = null;
            if (filterObj.has("cities")) {
                JSONArray citiesArray = filterObj.optJSONArray("cities");
                if (citiesArray.length() > 0) {
                    city = citiesArray.optString(0);
                }
            }
            if (city != null) {
                if (filterObj.has("attribute_filters")) {
                    obj.filterAttributes = (ArrayList) BrowseFilterAttributes.
                            parseFiltersArray(filterObj.optJSONArray("attribute_filters"));
                }

                if (filterObj.has("display_zone_filters")) {
                    obj.displayZones = DisplayZoneObject.parseFromArray(filterObj.optJSONObject("display_zone_filters").optJSONArray(city));
                }
                if (filterObj.has("date_filters")) {
                    obj.dateFilters = (ArrayList) BrowseFilterAttributes.
                            parseFiltersArray(filterObj.optJSONArray("date_filters"));
                }
                return obj;
            }
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public ArrayList<DisplayZoneObject> getDisplayZones() {
        return displayZones;
    }

    public ArrayList<BrowseFilterAttributes> getFilterAttributes() {
        return filterAttributes;
    }

    public ArrayList<BrowseFilterAttributes> getDateFilters() {
        return dateFilters;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeTypedList(filterAttributes);
        parcel.writeTypedList(displayZones);
        parcel.writeTypedList(dateFilters);
    }

    public static final Creator<BrowseFilterObject> CREATOR = new Creator<BrowseFilterObject>() {
        @Override
        public BrowseFilterObject createFromParcel(Parcel in) {
            return new BrowseFilterObject(in);
        }

        @Override
        public BrowseFilterObject[] newArray(int size) {
            return new BrowseFilterObject[size];
        }
    };
}
