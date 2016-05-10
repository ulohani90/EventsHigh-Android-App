package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.eventshigh.nearme.app.utils.DateTimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by umesh on 05/05/16.
 */
public class ShowDates implements Parcelable {
    private long date;
    private ArrayList<String> showTimes;

    public ShowDates(Parcel in){
        this.date = in.readLong();
        showTimes = new ArrayList<>();
        in.readStringList(showTimes);
    }

    public ShowDates(JSONObject obj) {
        try {
            date = DateTimeUtils.parseMovieTime(obj.getString("date"));
            showTimes = new ArrayList<>();
            JSONArray showTimesJson = obj.getJSONArray("showtimes");
            if (showTimesJson != null) {
                for (int i = 0; i < showTimesJson.length(); i++) {
                    showTimes.add((String) showTimesJson.get(i));
                }
            }
        } catch (JSONException e) {

        }
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public ArrayList<String> getShowTimes() {
        return showTimes;
    }

    public void setShowTimes(ArrayList<String> showTimes) {
        this.showTimes = showTimes;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(date);
        dest.writeStringList(showTimes);
    }
    public static final Parcelable.Creator<ShowDates> CREATOR =
            new Parcelable.Creator<ShowDates>() {
                public ShowDates createFromParcel(Parcel in) {
                    return new ShowDates(in);

                }

                public ShowDates[] newArray(int size) {
                    return new ShowDates[size];
                }
            };

}