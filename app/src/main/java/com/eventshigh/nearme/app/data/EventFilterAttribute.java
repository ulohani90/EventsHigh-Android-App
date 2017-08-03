package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by umesh on 22/12/16.
 */

public class EventFilterAttribute implements Parcelable {
    public String name;

    public boolean value;

    public static final String LOG_TAG = "No Resource Found";

    protected EventFilterAttribute(Parcel in) {
        name = in.readString();
        value = in.readInt() == 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeInt(value ? 1 : 0);
        
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<EventFilterAttribute> CREATOR = new Creator<EventFilterAttribute>() {
        @Override
        public EventFilterAttribute createFromParcel(Parcel in) {
            return new EventFilterAttribute(in);
        }

        @Override
        public EventFilterAttribute[] newArray(int size) {
            return new EventFilterAttribute[size];
        }
    };

    public EventFilterAttribute(JSONObject jsonObject) {
        this.name = jsonObject.optString("name");
        this.value = jsonObject.optBoolean("is_included");
    }

    public EventFilterAttribute(String name, boolean value) {
        this.name = name;
        this.value = value;
    }

    public static ArrayList<EventFilterAttribute> getAttributes(JSONArray jsonArray) {

        ArrayList<EventFilterAttribute> attributes = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                attributes.add(new EventFilterAttribute(jsonObject));
            }
        } catch (JSONException e) {

        }
        return attributes;

    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public int getIconResourceId() {
        int resId = R.drawable.ic_general_filter;

        try {
            resId = R.drawable.class.getField("ic_" + Utils.getUnderscoreString(name)).getInt(null);
        } catch (IllegalAccessException e) {
            // Ignore
        } catch (NoSuchFieldException e) {
            // Ignore
            Log.d(LOG_TAG, "no icon: " + name, e);
        }

        return resId;
    }

    public int getSelectedIconResourceId() {
        int resId = R.drawable.ic_general_filter_white;

        try {
            resId = R.drawable.class.getField("ic_" + Utils.getUnderscoreString(name) + "_white").getInt(null);
        } catch (IllegalAccessException e) {
            // Ignore
        } catch (NoSuchFieldException e) {
            // Ignore
            Log.d(LOG_TAG, "no icon: " + name, e);
        }

        return resId;
    }
}
