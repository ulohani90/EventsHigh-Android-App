package com.eventshigh.nearme.app.data;

import android.net.Uri;

import com.eventshigh.nearme.app.utils.DateTimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Offer from EventsHigh. Offer is some incentives for taking an actions -- e.g BookMyShow pass
 * for getting referrer install.
 */
public class Offer {
    public final String id;
    public final String message;
    public final Uri imgUrl;
    public final Date offerEndDate;
    public final String actionType;
    public final String actionName;
    public final String actionLink;
    public final int threshold;

    public Offer(String id, String message, String imgUrl, Date offerEndDate,
                 String actionType, String actionName, String actionLink, int threshold)
            throws IllegalArgumentException {
        this.id = id;
        this.message = message;
        this.imgUrl = Uri.parse(imgUrl);
        this.offerEndDate = offerEndDate;
        this.actionType = actionType;
        this.actionName = actionName;
        this.actionLink = actionLink;
        this.threshold = threshold;
    }

    public boolean isExpired() {
        return offerEndDate.getTime() < System.currentTimeMillis();
    }

    /**********************************
     JSON Parsing.
     *********************************/
    public static Offer parse(JSONObject offerJSON)
            throws JSONException, IllegalArgumentException, ParseException {
        return new Offer(
                offerJSON.getString("offer_id"),
                offerJSON.getString("offer_message"),
                offerJSON.getString("img_url"),
                DateTimeUtils.parseOfferDate(offerJSON.getString("offer_end_date")),
                offerJSON.getString("offer_action_type"),
                offerJSON.getString("offer_action_name"),
                offerJSON.getString("offer_action_link"),
                offerJSON.optInt("offer_threshold", 0)
        );
    }

    public static List<Offer> parse(JSONArray offersJSONArray)
            throws JSONException, IllegalArgumentException, ParseException {
        List<Offer> offers = new ArrayList<>();
        for (int i = 0; i < offersJSONArray.length(); i++) {
            offers.add(parse(offersJSONArray.getJSONObject(i)));
        }
        return offers;
    }
}
