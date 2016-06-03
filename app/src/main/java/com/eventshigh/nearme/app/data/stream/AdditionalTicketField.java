package com.eventshigh.nearme.app.data.stream;

import android.os.Parcel;
import android.os.Parcelable;

import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author shubham
 * @since 2/6/16.
 */
public class AdditionalTicketField implements Parcelable{

    String name;
    String type;
    List<String> options;

    public AdditionalTicketField(String name, String type, List<String> options){
        this.name = name;
        this.type = type;
        this.options = options;
    }

    public AdditionalTicketField(Parcel in){
        this.name = Utils.checkIfUnknown(in.readString());
        this.type = Utils.checkIfUnknown(in.readString());
        options = new ArrayList<>();
        in.readStringList(options);
    }

    public static final Parcelable.Creator<AdditionalTicketField> CREATOR =
            new Parcelable.Creator<AdditionalTicketField>() {
                public AdditionalTicketField createFromParcel(Parcel in) {
                    return new AdditionalTicketField(in);
                }
                public AdditionalTicketField[] newArray(int size) {
                    return new AdditionalTicketField[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(type);
        dest.writeStringList(options);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public static AdditionalTicketField fromJsonObject(JSONObject additionalFieldJson) throws ParseException,JSONException{
        String name = additionalFieldJson.getString("name");
        String type = additionalFieldJson.getString("type");
        String optionstr = additionalFieldJson.getString("options");
        List<String> options = new ArrayList<>();
        if(!Utils.checkIfStringEmpty(optionstr))
        options = Arrays.asList(optionstr.split(","));
        return new AdditionalTicketField(name,type,options);
    }

}
