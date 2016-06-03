package com.eventshigh.nearme.app.data.stream;

import android.os.Parcel;
import android.os.Parcelable;

import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventDescriptionSection;
import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.google.android.gms.maps.model.LatLng;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by umesh on 07/04/16.
 */
public class EhPrices implements Parcelable{
    public double min;
    public double max;
    public String name;
    public String note;
    public String currency;
    public ArrayList<Long> occurences;
    public double value;
    public double discountValue;
    public int count = 0;

    public EhPrices(Parcel in) {
        this.min = in.readDouble();
        this.max = in.readDouble();
        this.name = in.readString();
        this.note = in.readString();
        this.currency = in.readString();
        this.value = in.readDouble();
        this.discountValue = in.readDouble();
        this.occurences = new ArrayList<>();
        in.readList(occurences, Long.class.getClassLoader());
        this.count = in.readInt();
    }

    public EhPrices(double min, double max, String name, String note, String currency, double value, double discountValue, ArrayList<Long> occurences,int count) {
        this.min = min;
        this.max = max;
        this.name = name;
        this.note = note;
        this.currency = currency;
        this.value = value;
        this.discountValue = discountValue;
        this.occurences = occurences;
        this.count = count;
    }

    public static EhPrices createObject(double min, double max, String name, String note, String currency, double value, double discountValue, ArrayList<Long> occurences, int count) {
        return new EhPrices(min, max, name, note, currency, value, discountValue, occurences, count);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(min);
        dest.writeDouble(max);
        dest.writeString(name);
        dest.writeString(note);
        dest.writeString(currency);
        dest.writeDouble(value);
        dest.writeDouble(discountValue);
        dest.writeList(occurences);
        dest.writeInt(count);
    }

    public static final Parcelable.Creator<EhPrices> CREATOR =
            new Parcelable.Creator<EhPrices>() {
                public EhPrices createFromParcel(Parcel in) {
                    return new EhPrices(in);
                }
                public EhPrices[] newArray(int size) {
                    return new EhPrices[size];
                }
            };


}
