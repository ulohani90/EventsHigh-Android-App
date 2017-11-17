package com.eventshigh.nearme.app.data;

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

public class BrowseFilterAttributes {

    public static final String LOG_TAG = "No Resource Found";

    private String name;
    private String key;
    private String keyPath;
    private String keyForValue;
    private String valueType;
    private String requiredValue;

    public BrowseFilterAttributes() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public void setKeyPath(String keyPath) {
        this.keyPath = keyPath;
    }

    public String getKeyForValue() {
        return keyForValue;
    }

    public void setKeyForValue(String keyForValue) {
        this.keyForValue = keyForValue;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public String getRequiredValue() {
        return requiredValue;
    }

    public void setRequiredValue(String requiredValue) {
        this.requiredValue = requiredValue;
    }

    public BrowseFilterAttributes(String name, String key, String keyPath, String keyForValue, String valueType, String requiredValue) {
        this.name = name;
        this.key = key;
        this.keyPath = keyPath;
        this.keyForValue = keyForValue;
        this.valueType = valueType;
        this.requiredValue = requiredValue;
    }

    public static BrowseFilterAttributes parseJsonObject(JSONObject jsonObj) {
        BrowseFilterAttributes filter = new BrowseFilterAttributes();
        filter.setName(jsonObj.optString("name"));
        filter.setKey(jsonObj.optString("key"));
        filter.setKeyPath(jsonObj.optString("keyPath"));
        filter.setKeyForValue(jsonObj.optString("keyForValue"));
        filter.setValueType(jsonObj.optString("valueType"));
        filter.setRequiredValue(jsonObj.optString("requiredValue"));
        return filter;
    }

    public static List<BrowseFilterAttributes> parseFiltersArray(JSONArray jsonArray) {
        List<BrowseFilterAttributes> filters = new ArrayList<>();
        if (jsonArray != null)
            for (int i = 0; i < jsonArray.length(); i++) {
                filters.add(BrowseFilterAttributes.parseJsonObject(jsonArray.optJSONObject(i)));
            }
        return filters;
    }

    public int getIconResourceId() {
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

    public int getSelectedIconResourceId() {
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
}
