package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.crashlytics.android.Crashlytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class EventDescriptionSection implements Parcelable {
    public final String name;
    public final String description;

    public EventDescriptionSection(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**********************************
     * Parcel management methods.
     *********************************/
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(description);
    }

    // This is used to regenerate your object. All Parcelables must have
    // a CREATOR that implements these two methods
    public static final Parcelable.Creator<EventDescriptionSection> CREATOR =
            new Parcelable.Creator<EventDescriptionSection>() {
                public EventDescriptionSection createFromParcel(Parcel in) {
                    return new EventDescriptionSection(in.readString(), in.readString());
                }

                public EventDescriptionSection[] newArray(int size) {
                    return new EventDescriptionSection[size];
                }
            };


    /**********************************
     * Helper static methods, used for JSON parsing
     *********************************/
    public static EventDescriptionSection fromJSON(JSONObject jsonObject) throws JSONException, ParseException {
        return new EventDescriptionSection(jsonObject.getString("name"), jsonObject.getString("description"));
    }

    public static List<EventDescriptionSection> fromJSON(JSONArray jsonArray) {
        List<EventDescriptionSection> eventDescriptionSections = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject obj = jsonArray.getJSONObject(i);

                String desc = obj.optString("description");
                if (desc != null && desc.length() > 0) {
                    EventDescriptionSection descriptionSection = fromJSON(jsonArray.getJSONObject(i));
                    eventDescriptionSections.add(descriptionSection);
                }
            } catch (JSONException | ParseException e) {
                Crashlytics.getInstance().core.logException(e);
            }
        }
        return eventDescriptionSections;
    }

}
