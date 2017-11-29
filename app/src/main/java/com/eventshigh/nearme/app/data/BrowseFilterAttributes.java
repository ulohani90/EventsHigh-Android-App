package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.eventshigh.nearme.app.R;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 14/11/17.
 */

public class BrowseFilterAttributes implements Parcelable {

    public static final String LOG_TAG = "No Resource Found";

    private String name;
    private String path;
    private String value;
    private String displayIcon;
    private String expressionType;


    public BrowseFilterAttributes() {

    }

    protected BrowseFilterAttributes(Parcel in) {
        name = in.readString();
        path = in.readString();
        value = in.readString();
        displayIcon = in.readString();
        expressionType = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(path);
        dest.writeString(value);
        dest.writeString(displayIcon);
        dest.writeString(expressionType);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<BrowseFilterAttributes> CREATOR = new Creator<BrowseFilterAttributes>() {
        @Override
        public BrowseFilterAttributes createFromParcel(Parcel in) {
            return new BrowseFilterAttributes(in);
        }

        @Override
        public BrowseFilterAttributes[] newArray(int size) {
            return new BrowseFilterAttributes[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayIcon() {
        return displayIcon;
    }

    public void setDisplayIcon(String displayIcon) {
        this.displayIcon = displayIcon;
    }

    public String getExpressionType() {
        return expressionType;
    }

    public void setExpressionType(String expressionType) {
        this.expressionType = expressionType;
    }

    public BrowseFilterAttributes(String name, String path, String value, String expressionType) {
        this.name = name;
        this.path = path;
        this.value = value;
        this.expressionType = expressionType;

    }

    public static BrowseFilterAttributes parseJsonObject(JSONObject jsonObj) {
        BrowseFilterAttributes filter = new BrowseFilterAttributes();
        filter.setName(jsonObj.optString("name"));
        filter.setPath(jsonObj.optString("path"));
        filter.setValue(jsonObj.optString("value"));
        filter.setDisplayIcon(jsonObj.optString("display_icon"));
        if (jsonObj.has("expression_type")) {
            filter.setExpressionType(jsonObj.optString("expression_type"));
        }
        return filter;
    }

    public static List<BrowseFilterAttributes> parseFiltersArray(JSONArray jsonArray) {
        List<BrowseFilterAttributes> filters = new ArrayList<>();
        if (jsonArray != null)
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.optJSONObject(i);
                if (!(jsonObject.has("exclude") && jsonObject.optString("exclude").equalsIgnoreCase("all"))) {
                    filters.add(BrowseFilterAttributes.parseJsonObject(jsonArray.optJSONObject(i)));
                }

            }
        return filters;
    }

    public int getIconResourceId() {
        int resId = R.drawable.ic_general_filter_white;

        try {
            if (name.equalsIgnoreCase("5 Star rating")) {
                resId = R.drawable.ic_fa_star_5_white;
            } else if (name.equalsIgnoreCase("4 Star rating")) {
                resId = R.drawable.ic_fa_star_4_white;
            } else {
                resId = R.drawable.class.getField("ic_" + Utils.getUnderscoreString(displayIcon) + "_white").getInt(null);
            }
        } catch (IllegalAccessException e) {
            // Ignore
        } catch (NoSuchFieldException e) {
            // Ignore
            Log.d(LOG_TAG, "no icon: " + name, e);
        }

        return resId;
    }

    public int getSelectedIconResourceId() {
        int resId = R.drawable.ic_general_filter;

        try {
            if (name.equalsIgnoreCase("5 Star rating")) {
                resId = R.drawable.ic_fa_star_5;
            } else if (name.equalsIgnoreCase("4 Star rating")) {
                resId = R.drawable.ic_fa_star_4;
            } else {
                resId = R.drawable.class.getField("ic_" + Utils.getUnderscoreString(displayIcon)).getInt(null);
            }

        } catch (IllegalAccessException e) {
            // Ignore
        } catch (NoSuchFieldException e) {
            // Ignore
            Log.d(LOG_TAG, "no icon: " + name, e);
        }

        return resId;
    }
}
