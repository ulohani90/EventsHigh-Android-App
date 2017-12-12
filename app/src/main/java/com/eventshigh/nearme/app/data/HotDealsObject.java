package com.eventshigh.nearme.app.data;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by umesh on 12/12/17.
 */

public class HotDealsObject {

    private String eventName;
    private String eventId;
    private int rank;
    private String offerText;
    private String offerRemovalDate;
    private String link;


    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getOfferText() {
        return offerText;
    }

    public void setOfferText(String offerText) {
        this.offerText = offerText;
    }

    public String getOfferRemovalDate() {
        return offerRemovalDate;
    }

    public void setOfferRemovalDate(String offerRemovalDate) {
        this.offerRemovalDate = offerRemovalDate;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public static HotDealsObject parseJson(JSONObject jsonObject) {
        HotDealsObject obj = new HotDealsObject();
        obj.eventName = jsonObject.optString("event_name");
        obj.eventId = jsonObject.optString("eid");
        obj.rank = jsonObject.optInt("rank");
        obj.offerText = jsonObject.optString("offer_text");
        obj.offerRemovalDate = jsonObject.optString("offer_removal_date");
        obj.link = jsonObject.optString("link");
        return obj;
    }
}
