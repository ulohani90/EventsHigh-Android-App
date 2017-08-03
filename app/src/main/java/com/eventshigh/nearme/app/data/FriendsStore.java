package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.eventshigh.nearme.app.user.UserActionHelper;

public class FriendsStore {
    private final SharedPreferences preferences;
    private final Context context;

    public FriendsStore(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences("my_friends_email", Context.MODE_PRIVATE);
    }

    public boolean isFollowing(String email) {

        // By default we assume all friends are being followed, if the information is missing from prefs
        return preferences.getBoolean(email, false);
    }

    public boolean isKeyExists(String email) {
        return preferences.contains(email);
    }

    public void setFollowing(String email, String contactId, boolean following) {

        new UserActionHelper(context).recordUserFollowAction(following ? UserActionHelper.UserFollowingAction.USER_FOLLOW : UserActionHelper.UserFollowingAction.USER_UNFOLLOW, email);
        preferences.edit().putBoolean(email, following).apply();
    }
}
