package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Created by umesh on 25/11/17.
 */

public class DisplayZoneObject implements Parcelable {

    private String displayName;

    private ArrayList<String> synonyms;

    public DisplayZoneObject() {

    }

    public ArrayList<String> getSynonyms() {
        return synonyms;
    }

    public String getDisplayName() {
        return displayName;
    }

    protected DisplayZoneObject(Parcel in) {
        displayName = in.readString();
        synonyms = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(displayName);
        dest.writeStringList(synonyms);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DisplayZoneObject> CREATOR = new Creator<DisplayZoneObject>() {
        @Override
        public DisplayZoneObject createFromParcel(Parcel in) {
            return new DisplayZoneObject(in);
        }

        @Override
        public DisplayZoneObject[] newArray(int size) {
            return new DisplayZoneObject[size];
        }
    };

    public static ArrayList<DisplayZoneObject> parseFromArray(JSONArray jsonArray) {
        ArrayList<DisplayZoneObject> objs = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            objs.add(parseFromJson(jsonArray.optJSONObject(i)));
        }
        return objs;
    }

    public static DisplayZoneObject parseFromJson(JSONObject jsonObject) {
        DisplayZoneObject obj = new DisplayZoneObject();
        obj.displayName = jsonObject.optString("display_name");
        obj.synonyms = new ArrayList<>();
        if (jsonObject.has("synonyms")) {
            JSONArray synsArray = jsonObject.optJSONArray("synonyms");
            for (int i = 0; i < synsArray.length(); i++) {
                obj.synonyms.add(synsArray.optString(i));
            }
        }
        return obj;
    }


    @Override
    public boolean equals(Object obj) {
        DisplayZoneObject displayZoneObject = (DisplayZoneObject) obj;
        if (this.displayName.equalsIgnoreCase(displayZoneObject.getDisplayName())) {
            return true;
        }
        return false;

    }
}
