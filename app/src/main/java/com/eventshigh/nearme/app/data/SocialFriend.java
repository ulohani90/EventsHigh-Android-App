package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.utils.ContactUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SocialFriend {
    private final String name;
    public final String mobileNo;
    @Nullable
    public final UserContact contact;

    public SocialFriend(String name, String mobileNo, @Nullable UserContact contact) {
        this.name = name;
        this.mobileNo = mobileNo;
        this.contact = contact;
    }

    public SocialFriend(@NonNull UserContact contact) {
        this.name = contact.name;
        this.mobileNo = contact.mobileNo;
        this.contact = contact;
    }

    public String getName() {
        return contact == null ? name : contact.name;
    }

    public Drawable getDrawable(Context context, int size) {
        return (contact != null) ? contact.getDrawable(context, size) :
                UserContact.getDrawableForName(name, size);
    }

    @Override
    public int hashCode() {
        return mobileNo.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SocialFriend
                && ((SocialFriend) other).mobileNo.equals(mobileNo);
    }

    public static SocialFriend fromJSON(JSONObject data, Context context) throws JSONException {
        String mobileNo = data.getString("mobile_no");
        return new SocialFriend(data.getString("name"), mobileNo,
                ContactUtils.getContactForServerPhone(context, mobileNo));
    }

    public static List<SocialFriend> fromJSON(JSONArray data, Context context) throws JSONException {
        List<SocialFriend> friends = new ArrayList<>(data.length());
        for (int i = 0; i < data.length(); i++) {
            friends.add(fromJSON(data.getJSONObject(i), context));
        }
        return friends;
    }
}
