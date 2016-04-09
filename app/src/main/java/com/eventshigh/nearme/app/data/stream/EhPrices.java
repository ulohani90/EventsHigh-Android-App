package com.eventshigh.nearme.app.data.stream;

import android.os.Parcel;
import android.os.Parcelable;

import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.EventDescriptionSection;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by umesh on 07/04/16.
 */
public class EhPrices implements Parcelable{
    public  double min;
    public  double max;
    public String name;
    public  String note;
    public String currency;

    public EhPrices(double min,double max,String name,String note,String currency){
        this.min = min;
        this.max = max;
        this.name = name;
        this.note=note;
        this.currency = currency;
    }

    public static EhPrices createObject(double min,double max,String name,String note,String currency){
        return  new EhPrices(min,max,name,note,currency);
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
    }

    public static final Parcelable.Creator<EhPrices> CREATOR =
            new Parcelable.Creator<EhPrices>() {
                public EhPrices createFromParcel(Parcel in) {
                    return new EhPrices(in.readDouble(),
                            in.readDouble(),
                            in.readString(),
                            in.readString(),
                            in.readString()
                    );
                }

                public EhPrices[] newArray(int size) {
                    return new EhPrices[size];
                }
            };


}
