package com.eventshigh.nearme.app.data.stream;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by umesh on 16/04/16.
 */
public class VoucherObject implements Parcelable{

        public final String voucherName;
        public final int pointsReq;

        public VoucherObject(String voucherName,int pointsReq){
            this.voucherName = voucherName;
            this.pointsReq = pointsReq;
        }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(voucherName);
        dest.writeInt(pointsReq);
    }

    public static final Parcelable.Creator<VoucherObject> CREATOR =
            new Parcelable.Creator<VoucherObject>() {
                public VoucherObject createFromParcel(Parcel in) {
                    return new VoucherObject(
                            in.readString(),
                            in.readInt()
                    );
                }

                public VoucherObject[] newArray(int size) {
                    return new VoucherObject[size];
                }
            };
}
