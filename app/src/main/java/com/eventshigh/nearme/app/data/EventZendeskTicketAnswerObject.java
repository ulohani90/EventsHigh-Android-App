package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by umesh on 15/03/17.
 */

public class EventZendeskTicketAnswerObject implements Parcelable {

    public String answer;
    public long timeStamp;

    public EventZendeskTicketAnswerObject(String answer, long timeStamp) {
        this.answer = answer;
        this.timeStamp = timeStamp;
    }


    protected EventZendeskTicketAnswerObject(Parcel in) {
        answer = in.readString();
        timeStamp = in.readLong();
    }

    public static final Creator<EventZendeskTicketAnswerObject> CREATOR = new Creator<EventZendeskTicketAnswerObject>() {
        @Override
        public EventZendeskTicketAnswerObject createFromParcel(Parcel in) {
            return new EventZendeskTicketAnswerObject(in);
        }

        @Override
        public EventZendeskTicketAnswerObject[] newArray(int size) {
            return new EventZendeskTicketAnswerObject[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(answer);
        dest.writeLong(timeStamp);
    }
}
