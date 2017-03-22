package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.method.DateTimeKeyListener;

import com.eventshigh.nearme.app.utils.DateTimeUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 15/03/17.
 */

public class EventZendeskTicketObject implements Parcelable {

    public String question;
    public long timeStamp;

    public ArrayList<EventZendeskTicketAnswerObject> answers;

    public EventZendeskTicketObject(String question, long timeStamp, ArrayList<EventZendeskTicketAnswerObject> answers) {
        this.question = question;
        this.timeStamp = timeStamp;
        this.answers = answers;
    }


    protected EventZendeskTicketObject(Parcel in) {
        question = in.readString();
        timeStamp = in.readLong();
        answers = new ArrayList<>();
        in.readTypedList(answers, EventZendeskTicketAnswerObject.CREATOR);
    }

    public static final Creator<EventZendeskTicketObject> CREATOR = new Creator<EventZendeskTicketObject>() {
        @Override
        public EventZendeskTicketObject createFromParcel(Parcel in) {
            return new EventZendeskTicketObject(in);
        }

        @Override
        public EventZendeskTicketObject[] newArray(int size) {
            return new EventZendeskTicketObject[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(question);
        dest.writeLong(timeStamp);
        dest.writeTypedList(answers);
    }

    public static EventZendeskTicketObject parseZendeskObj(JSONObject object) {
        JSONArray comments = object.optJSONArray("comments");
        String question = null;
        long timeStamp = 0;
        if (comments.length() > 0) {
            JSONObject questionObj = comments.optJSONObject(0);
            question = questionObj.optString("body");
            timeStamp = DateTimeUtils.parseZendeskTicketDate(questionObj.optString("created_at"));
        }
        ArrayList<EventZendeskTicketAnswerObject> answers = new ArrayList<>();
        if (comments.length() > 1) {
            for (int i = 1; i < comments.length(); i++) {
                JSONObject commentObj = comments.optJSONObject(i);
                answers.add(new EventZendeskTicketAnswerObject(commentObj.optString("body"),
                        DateTimeUtils.parseZendeskTicketDate(commentObj.optString("created_at"))));
            }

        }

        return new EventZendeskTicketObject(question, timeStamp, answers);

    }
}
