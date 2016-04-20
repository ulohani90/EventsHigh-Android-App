package com.eventshigh.nearme.app.data.stream;

import android.os.Parcel;
import android.os.Parcelable;

import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;

/**
 * Created by umesh on 16/04/16.
 */
public class OfferObject implements Parcelable{

        public final int id;
        public final String city;
        public final String name;
        public final String imgUrl;
        public final String desc;
        public final long validTill;
        public final String callToAction;
        public final int rank;
        public final String actionButtonText;
        public final String termsConditions;
        public final int minPoints;
        public final ArrayList<VoucherObject> vouchers;

    public OfferObject(int id , String city,String name,String imgUrl,String desc,long validTill,String callToAction,int rank,String actionButtonText,String termsConditions,
                       int minPoints,ArrayList<VoucherObject> vouchers){
        this.id = id;
        this.city = city;
        this.name = name;
        this.imgUrl = imgUrl;
        this.desc = desc;
        this.validTill = validTill;
        this.callToAction = callToAction;
        this.rank = rank;
        this.actionButtonText = actionButtonText;
        this.termsConditions = termsConditions;
        this.minPoints = minPoints;
        this.vouchers = vouchers;

    }

    public static OfferObject parse(JSONObject obj) throws JSONException ,ParseException {

        int id = obj.getInt("id");
        String name = obj.getString("name");
        String city = obj.getString("city");
        String imgUrl = obj.getString("img_url");
        String desc = obj.getString("desc");
        long validTill = 0;
        if(Utils.checkIfUnknown(obj.getString("valid_till")) !=null) {
           validTill = DateTimeUtils.parseOfferTime(obj.getString("valid_till"));
        }
        String callToAction = obj.getString("call_to_action");
        int rank = obj.getInt("rank");
        String actionButtonText = obj.getString("action_button_text");
        String termsConditions = obj.getString("terms_and_conditions");
        int minPoints = obj.getInt("min_points");
        ArrayList<VoucherObject> vouchers = new ArrayList<>();
        String voucherArray = Utils.checkIfUnknown(obj.getString("vouchers_arr"));
        if(voucherArray!=null && voucherArray.length()>0){
            String[] vouchersData = voucherArray.split(",");
            if(vouchers!=null ){
                for(int i=0;i<vouchersData.length;i++){
                    String[] data = vouchersData[i].split("\\|");
                    vouchers.add(new VoucherObject(data[0],data.length>1?Integer.parseInt(data[1]):0));
                }
            }
        }

        return new OfferObject(id,city,name,imgUrl,desc,validTill,callToAction,rank,actionButtonText,termsConditions,minPoints,vouchers);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    public OfferObject (Parcel in){
        this.id = in.readInt();
        this.city = in.readString();
        this.name = in.readString();
        this.imgUrl  = in.readString();
        this.desc = in.readString();
        this.validTill = in.readLong();
        this.callToAction = in.readString();
        this.rank = in.readInt();
        this.actionButtonText =in.readString();
        this.termsConditions = in.readString();
        this.minPoints = in.readInt();
        this.vouchers = new ArrayList<>();
        in.readTypedList(vouchers,VoucherObject.CREATOR);

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(city);
        dest.writeString(name);
        dest.writeString(imgUrl);
        dest.writeString(desc);
        dest.writeLong(validTill);
        dest.writeString(callToAction);
        dest.writeInt(rank);
        dest.writeString(actionButtonText);
        dest.writeString(termsConditions);
        dest.writeInt(minPoints);
        dest.writeTypedList(vouchers);
    }

    public static final Parcelable.Creator<OfferObject> CREATOR =
            new Parcelable.Creator<OfferObject>() {
                public OfferObject createFromParcel(Parcel in) {
                    return new OfferObject(in);
                }

                public OfferObject[] newArray(int size) {
                    return new OfferObject[size];
                }
            };

}
