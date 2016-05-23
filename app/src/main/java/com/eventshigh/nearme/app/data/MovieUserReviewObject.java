package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONObject;

import java.net.URL;

/**
 * @author shubham
 * @since 16/5/16.
 */


public class MovieUserReviewObject implements Parcelable {

    private String reviewId;
    private String reviewerId;
    private String reviewFor;
    private String reviewBy;


    private String reviewEntity;
    private String reviewedEntityId;
    private int reviewRating;
    private String reviewText;

    private String reviewPlatform;
    private String reviewDeviceId;

    public MovieUserReviewObject() {
    }

    public MovieUserReviewObject(Parcel in) {
        this.reviewId = in.readString();
        this.reviewerId = in.readString();
        this.reviewFor = in.readString();
        this.reviewBy = in.readString();
        this.reviewEntity = in.readString();
        this.reviewRating = in.readInt();
        this.reviewText = in.readString();
        this.reviewPlatform = in.readString();
        this.reviewDeviceId = in.readString();
        this.reviewedEntityId = in.readString();
    }


    public MovieUserReviewObject(JSONObject obj) {
        try {
            reviewId = Utils.checkIfUnknown(obj.getString("review_id"));
            reviewerId = Utils.checkIfUnknown(obj.getString("reviewer_id"));
            reviewFor = Utils.checkIfUnknown(obj.getString("review_for"));
            reviewEntity = Utils.checkIfUnknown(obj.getString("reviewed_entity"));
            reviewBy = Utils.checkIfUnknown(obj.getString("review_by"));
            reviewRating = obj.getInt("ratings");
            reviewText = Utils.checkIfUnknown(obj.getString("review_text"));
            reviewPlatform = Utils.checkIfUnknown(obj.getString("review_platform"));
            reviewDeviceId = Utils.checkIfUnknown(obj.getString("review_device_id"));
            reviewedEntityId = Utils.checkIfUnknown(obj.getString("reviewed_entity_id"));
        } catch (Exception e) {

        }

    }

    //setters and getters
    public String getReviewText() {
        return reviewText;
    }

    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }


    public String getReviewedEntityId() {
        return reviewedEntityId;
    }

    public void setReviewedEntityId(String reviewedEntityId) {
        this.reviewedEntityId = reviewedEntityId;
    }

    public int getReviewRating() {
        return reviewRating;
    }

    public void setReviewRating(int reviewRating) {
        this.reviewRating = reviewRating;
    }

    public String getReviewEntity() {
        return reviewEntity;
    }

    public void setReviewEntity(String reviewEntity) {
        this.reviewEntity = reviewEntity;
    }

    public String getReviewBy() {
        return reviewBy;
    }

    public void setReviewBy(String reviewBy) {
        this.reviewBy = reviewBy;
    }

    public String getReviewFor() {
        return reviewFor;
    }

    public void setReviewFor(String reviewFor) {
        this.reviewFor = reviewFor;
    }

    public String getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(String reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getReviewPlatform() {
        return reviewPlatform;
    }

    public void setReviewPlatform(String reviewPlatform) {
        this.reviewPlatform = reviewPlatform;
    }

    public String getReviewDeviceId() {
        return reviewDeviceId;
    }

    public void setReviewDeviceId(String reviewDeviceId) {
        this.reviewDeviceId = reviewDeviceId;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(reviewId);
        dest.writeString(reviewerId);
        dest.writeString(reviewFor);
        dest.writeString(reviewBy);
        dest.writeString(reviewEntity);
        dest.writeInt(reviewRating);
        dest.writeString(reviewText);
        dest.writeString(reviewPlatform);
        dest.writeString(reviewDeviceId);

    }

    public static final Parcelable.Creator<MovieUserReviewObject> CREATOR =
            new Parcelable.Creator<MovieUserReviewObject>() {
                public MovieUserReviewObject createFromParcel(Parcel in) {
                    return new MovieUserReviewObject(in);
                }

                public MovieUserReviewObject[] newArray(int size) {
                    return new MovieUserReviewObject[size];
                }
            };
}

