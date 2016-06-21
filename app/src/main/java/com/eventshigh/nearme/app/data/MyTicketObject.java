package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.utils.Utils;
import com.squareup.okhttp.internal.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author shubham
 * @since 15/6/16.
 */
public class MyTicketObject implements Parcelable {

    String bookingId;
    String userName;

    String eventId;
    String userEmail;
    String userMobileNo;
    String eventTime;
    String ticketType;
    String noTicket;
    String amount;
    String eventName;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserMobileNo() {
        return userMobileNo;
    }

    public void setUserMobileNo(String userMobileNo) {
        this.userMobileNo = userMobileNo;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public String getNoTicket() {
        return noTicket;
    }

    public void setNoTicket(String noTicket) {
        this.noTicket = noTicket;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }


    public MyTicketObject(Parcel in){
        this.eventId = in.readString();
        this.eventName = in.readString();
        this.bookingId = in.readString();
        this.userName = in.readString();
        this.userEmail = in.toString();
        this.userMobileNo = in.toString();
        this.eventTime = in.toString();
        this.ticketType = in.toString();
        this.noTicket = in.toString();
        this.amount = in.toString();
    }

    public MyTicketObject(JSONObject jsonObject){
        try{
            eventId = Utils.checkIfUnknown(jsonObject.getString("eventId"));
            eventName = Utils.checkIfUnknown(jsonObject.getString("eventName"));
            bookingId = Utils.checkIfUnknown(jsonObject.getString("bookingId"));
            userName = Utils.checkIfUnknown(jsonObject.getString("userName"));
            userEmail = Utils.checkIfUnknown(jsonObject.getString("userEmail"));
            userMobileNo = Utils.checkIfUnknown(jsonObject.getString("userMobileNo"));
            eventTime = Utils.checkIfUnknown(jsonObject.getString("eventTime"));
            ticketType = Utils.checkIfUnknown(jsonObject.getString("ticketType"));
            noTicket = Utils.checkIfUnknown(jsonObject.getString("numTickets"));
            amount = Utils.checkIfUnknown(jsonObject.getString("amount"));
        }catch (JSONException jse){
            Log.e("MyTicketObject JSON",jse.toString());
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(eventId);
        dest.writeString(eventName);
        dest.writeString(bookingId);
        dest.writeString(userName);
        dest.writeString(userEmail);
        dest.writeString(userMobileNo);
        dest.writeString(eventTime);
        dest.writeString(ticketType);
        dest.writeString(noTicket);
        dest.writeString(amount);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<MyTicketObject> CREATOR =
            new Parcelable.Creator<MyTicketObject>() {
                public MyTicketObject createFromParcel(Parcel in) {
                    return new MyTicketObject(in);
                }

                public MyTicketObject[] newArray(int size) {
                    return new MyTicketObject[size];
                }
            };


    public static List<MyTicketObject> fromJSON(JSONArray jsonArray) {
        List<MyTicketObject> myTicketObjects = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                MyTicketObject myTicketObject = new MyTicketObject(jsonArray.getJSONObject(i));
                myTicketObjects.add(myTicketObject);
            } catch (JSONException e) {
                Crashlytics.getInstance().core.logException(e);
            }
        }
        return myTicketObjects;
    }

}
