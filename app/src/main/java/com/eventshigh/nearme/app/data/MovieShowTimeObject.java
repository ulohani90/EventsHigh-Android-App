package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by umesh on 09/05/16.
 */
public class MovieShowTimeObject implements Parcelable {
    private String venueName;
    private String venueAddress;
    private String venueLink;
    private String city;
    private ArrayList<ShowDates> showDates;

    public MovieShowTimeObject(Parcel in) {
        this.venueName = in.readString();
        this.venueAddress = in.readString();
        this.venueLink = in.readString();
        this.city = in.readString();
        showDates = new ArrayList<>();
        in.readTypedList(showDates, ShowDates.CREATOR);
    }

    public MovieShowTimeObject(JSONObject obj) {
        try {
            this.venueName = obj.getString("venue_name");
            this.venueAddress = obj.getString("venue_address");
            this.venueLink = obj.getString("venue_link");
            this.city = obj.getString("city");
            this.showDates = new ArrayList<>();
            JSONArray dates = obj.getJSONArray("dates");
            if (dates != null) {
                for (int i = 0; i < dates.length(); i++) {
                    showDates.add(new ShowDates(dates.getJSONObject(i)));
                }
            }
        } catch (JSONException e) {

        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(venueName);
        dest.writeString(venueAddress);
        dest.writeString(venueLink);
        dest.writeString(city);
        dest.writeTypedList(showDates);
    }


    public String getVenueName() {
        return venueName;
    }

    public void setVenueName(String venueName) {
        this.venueName = venueName;
    }

    public String getVenueAddress() {
        return venueAddress;
    }

    public void setVenueAddress(String venueAddress) {
        this.venueAddress = venueAddress;
    }

    public String getVenueLink() {
        return venueLink;
    }

    public void setVenueLink(String venueLink) {
        this.venueLink = venueLink;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public ArrayList<ShowDates> getShowDates() {
        return showDates;
    }

    public void setShowDates(ArrayList<ShowDates> showDates) {
        this.showDates = showDates;
    }

    @Override
    public int describeContents() {
        return 0;
    }


    public static final Parcelable.Creator<MovieShowTimeObject> CREATOR =
            new Parcelable.Creator<MovieShowTimeObject>() {
                public MovieShowTimeObject createFromParcel(Parcel in) {
                    return new MovieShowTimeObject(in);

                }

                public MovieShowTimeObject[] newArray(int size) {
                    return new MovieShowTimeObject[size];
                }
            };
}
