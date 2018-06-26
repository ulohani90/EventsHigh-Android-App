package com.eventshigh.nearme.app.data;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class CityBannerObject {
    private String imgUrl;
    private String destinationType;
    private String eId;
    private String interest;
    private int imageWidth;
    private int imageHeight;


    public CityBannerObject() {

    }

    public static CityBannerObject parseFromJson(JSONObject jsonObject) throws JSONException {
        CityBannerObject obj = new CityBannerObject();
        obj.imgUrl = jsonObject.getString("img_url");
        obj.destinationType = jsonObject.getString("destination_type");
        obj.eId = jsonObject.getString("eid");
        obj.interest = jsonObject.getString("interest");
        obj.imageWidth = jsonObject.getInt("image_width");
        obj.imageHeight = jsonObject.getInt("image_height");
        return obj;
    }


    public static List<CityBannerObject> parseFromJsonArray(JSONArray jsonArray) {
        List<CityBannerObject> objs = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                objs.add(parseFromJson(jsonArray.getJSONObject(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return objs;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public String geteId() {
        return eId;
    }

    public String getInterest() {
        return interest;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public int getImageWidth() {
        return imageWidth;
    }
}
