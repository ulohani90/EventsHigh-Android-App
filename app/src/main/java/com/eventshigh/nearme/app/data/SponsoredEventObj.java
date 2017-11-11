package com.eventshigh.nearme.app.data;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 10/11/17.
 */

public class SponsoredEventObj {
    public String destinationUrl;
    public String bannerUrl;
    public int rank;

    public SponsoredEventObj() {

    }

    public static SponsoredEventObj parseJson(JSONObject jsonObj) {
        SponsoredEventObj sponsoredEventObj = new SponsoredEventObj();
        sponsoredEventObj.bannerUrl = jsonObj.optString("bannerUrl");
        sponsoredEventObj.destinationUrl = jsonObj.optString("destinationUrl");
        sponsoredEventObj.rank = jsonObj.optInt("rank");
        return sponsoredEventObj;
    }

    public static List<SponsoredEventObj> parseJsonArray(JSONArray jsonArray) {
        List<SponsoredEventObj> objs = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            objs.add(parseJson(jsonArray.optJSONObject(i)));
        }
        return objs;
    }
}
