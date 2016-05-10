package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;

import com.eventshigh.nearme.app.utils.Utils;

import org.json.JSONObject;

import java.net.URL;

/**
 * Created by umesh on 09/05/16.
 */

public class MovieReviewObject implements Parcelable {
    private String reviewTitle;
    private String reviewBlob;
    private String imageUrl;

    private String reviewerName;
    private String sourceUrl;
    private int id;

    public MovieReviewObject(Parcel in) {
        this.id = in.readInt();
        this.reviewTitle = in.readString();
        this.reviewBlob = in.readString();
        this.imageUrl = in.readString();
        this.reviewerName = in.readString();
        this.sourceUrl = in.readString();
    }


    public MovieReviewObject(JSONObject obj) {
        try {
            id = obj.getInt("id");
            reviewTitle = Utils.checkIfUnknown(obj.getString("review_title"));
            reviewBlob = Utils.checkIfUnknown(obj.getString("review_blob"));
            imageUrl = Utils.checkIfUnknown(obj.getString("image_url"));
            sourceUrl = obj.getString("review_url");
            URL url = new URL(sourceUrl);
            reviewerName = url.getHost();

        } catch (Exception e) {

        }

    }

    public String getReviewTitle() {
        return reviewTitle;
    }

    public void setReviewTitle(String reviewTitle) {
        this.reviewTitle = reviewTitle;
    }

    public String getReviewBlob() {
        return reviewBlob;
    }

    public void setReviewBlob(String reviewBlob) {
        this.reviewBlob = reviewBlob;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(reviewTitle);
        dest.writeString(reviewBlob);
        dest.writeString(imageUrl);
        dest.writeString(reviewerName);
        dest.writeString(sourceUrl);

    }

    public static final Parcelable.Creator<MovieReviewObject> CREATOR =
            new Parcelable.Creator<MovieReviewObject>() {
                public MovieReviewObject createFromParcel(Parcel in) {
                    return new MovieReviewObject(in);

                }

                public MovieReviewObject[] newArray(int size) {
                    return new MovieReviewObject[size];
                }
            };
}

