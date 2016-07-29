package com.eventshigh.nearme.app.data;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by umesh on 21/07/16.
 */
public class NewSocialFriend implements Comparable<SocialFriend>, Parcelable {

    private final String name;
    private final String email;
    private final String photo;


    public NewSocialFriend(String name, String email, String photo) {
        this.name = name;
        this.email = email;
        this.photo = photo;
    }

    public NewSocialFriend(Parcel in) {
        this.name = in.readString();
        this.email = in.readString();
        this.photo = in.readString();
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoto() {
        return photo;
    }

    @Override
    public int compareTo(SocialFriend another) {
        return 0;
    }

    public static List<NewSocialFriend> parseFriendsArray(JSONArray friends) {
        List<NewSocialFriend> friendsList = new ArrayList<>();
        for (int i = 0; i < friends.length(); i++) {
            try {
                JSONObject friendObj = friends.getJSONObject(i);
                friendsList.add(parseJsonObject(friendObj));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return friendsList;
    }

    public static NewSocialFriend parseJsonObject(JSONObject friendObj) throws JSONException {
        return new NewSocialFriend(friendObj.getString("name"), friendObj.getString("email"), friendObj.getString("profile_pic"));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(email);
        dest.writeString(photo);
    }

    public static final Parcelable.Creator<NewSocialFriend> CREATOR =
            new Parcelable.Creator<NewSocialFriend>() {
                public NewSocialFriend createFromParcel(Parcel in) {
                    return new NewSocialFriend(in);
                }

                public NewSocialFriend[] newArray(int size) {
                    return new NewSocialFriend[size];
                }
            };
}
