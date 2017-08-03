package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.eventshigh.nearme.app.utils.DateTimeUtils;
import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONObject;

/**
 * Created by umesh on 16/09/16.
 */
public class EventSession implements Parcelable {


    private int pkId;
    private String sessionId;
    private String parentEId;
    private String title;
    private String description;
    private String performers;
    private String imageUrl;

    private String venue;
    private long date;
    private String startTime;
    private long endDate;
    private String endTime;
    private boolean isStandAlone;
    private boolean isSessionTicketing;
    private String relatedEId;


    public EventSession(JSONObject obj) {
        pkId = obj.optInt("pk_id");
        sessionId = Utils.checkIfUnknown(obj.optString("session_id"));
        parentEId = Utils.checkIfUnknown(obj.optString("parent_eid"));
        title = Utils.checkIfUnknown(obj.optString("title"));
        description = Utils.checkIfUnknown(obj.optString("description"));
        performers = Utils.checkIfUnknown(obj.optString("performers"));
        imageUrl = Utils.checkIfUnknown(obj.optString("image_url"));
        venue = Utils.checkIfUnknown(obj.optString("venue"));
        date = DateTimeUtils.parseMovieTime(obj.optString("date"));
        endDate = DateTimeUtils.parseMovieTime(obj.optString("end_date"));
        startTime = obj.optString("start_time");
        endTime = obj.optString("end_time");
        isStandAlone = obj.optBoolean("is_stand_alone");
        isSessionTicketing = obj.optBoolean("is_session_ticketing");
        relatedEId = Utils.checkIfUnknown(obj.optString("related_eid"));


    }

    public int getPkId() {
        return pkId;
    }

    public void setPkId(int pkId) {
        this.pkId = pkId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getParentEId() {
        return parentEId;
    }

    public void setParentEId(String parentEId) {
        this.parentEId = parentEId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPerformers() {
        return performers;
    }

    public void setPerformers(String performers) {
        this.performers = performers;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public long getEndDate() {
        return endDate;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public boolean isStandAlone() {
        return isStandAlone;
    }

    public void setIsStandAlone(boolean isStandAlone) {
        this.isStandAlone = isStandAlone;
    }

    public boolean isSessionTicketing() {
        return isSessionTicketing;
    }

    public void setIsSessionTicketing(boolean isSessionTicketing) {
        this.isSessionTicketing = isSessionTicketing;
    }

    public String getRelatedEId() {
        return relatedEId;
    }

    protected EventSession(Parcel in) {
        pkId = in.readInt();
        sessionId = in.readString();
        parentEId = in.readString();
        title = in.readString();
        description = in.readString();
        performers = in.readString();
        imageUrl = in.readString();
        venue = in.readString();
        date = in.readLong();
        endDate = in.readLong();
        startTime = in.readString();
        endTime = in.readString();
        isStandAlone = in.readInt() == 1;
        isSessionTicketing = in.readInt() == 1;
        relatedEId = in.readString();

    }

    public static final Creator<EventSession> CREATOR = new Creator<EventSession>() {
        @Override
        public EventSession createFromParcel(Parcel in) {
            return new EventSession(in);
        }

        @Override
        public EventSession[] newArray(int size) {
            return new EventSession[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(pkId);
        dest.writeString(sessionId);
        dest.writeString(parentEId);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(performers);
        dest.writeString(imageUrl);
        dest.writeString(venue);
        dest.writeLong(date);
        dest.writeLong(endDate);
        dest.writeString(startTime);
        dest.writeString(endTime);
        dest.writeInt(isStandAlone ? 1 : 0);
        dest.writeInt(isSessionTicketing ? 1 : 0);
        dest.writeString(relatedEId);

    }
}
