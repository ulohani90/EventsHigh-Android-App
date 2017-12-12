package com.eventshigh.nearme.app.data;

import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.DealsComaprator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * Created by umesh on 12/12/17.
 */

public class DealsObject {

    ArrayList<HotDealsObject> hotDeals;

    ArrayList<HotDealsObject> helloBarDeals;

    public ArrayList<HotDealsObject> getHotDeals() {
        return hotDeals;
    }

    public void setHotDeals(ArrayList<HotDealsObject> hotDeals) {
        this.hotDeals = hotDeals;
    }

    public ArrayList<HotDealsObject> getHelloBarDeals() {
        return helloBarDeals;
    }

    public void setHelloBarDeals(ArrayList<HotDealsObject> helloBarDeals) {
        this.helloBarDeals = helloBarDeals;
    }

    public static DealsObject parseJson(JSONObject jsonObject) {
        DealsObject obj = new DealsObject();
        if (jsonObject.has("hot_deals")) {
            obj.hotDeals = new ArrayList<>();
            JSONArray hotDealsArray = jsonObject.optJSONArray("hot_deals");
            for (int i = 0; i < hotDealsArray.length(); i++) {
                HotDealsObject deal = HotDealsObject.parseJson(hotDealsArray.optJSONObject(i));
                if (isValidDeal(deal))
                    obj.hotDeals.add(deal);
            }
            Collections.sort(obj.hotDeals, new DealsComaprator());
        }


        if (jsonObject.has("hellobar_deals")) {
            obj.helloBarDeals = new ArrayList<>();
            JSONArray helloBarDealsArray = jsonObject.optJSONArray("hellobar_deals");
            for (int i = 0; i < helloBarDealsArray.length(); i++) {
                HotDealsObject deal = HotDealsObject.parseJson(helloBarDealsArray.optJSONObject(i));
                if (isValidDeal(deal))
                    obj.helloBarDeals.add(deal);
            }
            Collections.sort(obj.hotDeals, new DealsComaprator());
        }


        return obj;
    }

    public static boolean isValidDeal(HotDealsObject deal) {
        Date date = new Date();
        SimpleDateFormat FULL_DATE_TIME_MILLIS_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            date = FULL_DATE_TIME_MILLIS_FORMAT.parse(deal.getOfferRemovalDate());
            if (date.getTime() >= System.currentTimeMillis()) {
                return true;
            } else {
                return false;
            }

        } catch (ParseException e) {
            e.printStackTrace();
            return true;
        }
    }
}
