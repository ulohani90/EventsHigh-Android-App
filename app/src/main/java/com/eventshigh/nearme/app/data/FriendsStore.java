package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.user.UserActionHelper;

public class FriendsStore {
    private final SharedPreferences preferences;
    private final Context context;

    public FriendsStore(Context context) {
        this.context = context;
        preferences = context.getSharedPreferences("my_friends", Context.MODE_PRIVATE);
    }

    public boolean isFollowing(String mobileNo) {


        // By default we assume all friends are being followed, if the information is missing from prefs
        return preferences.getBoolean(mobileNo, false);
    }

    public boolean isKeyExists(String mobileNo) {
        return preferences.contains(mobileNo);
    }

    public void setFollowing(String mobileNo, String contactId, boolean following) {

        new UserActionHelper(context).recordUserFollowAction(following ? UserActionHelper.UserFollowingAction.USER_FOLLOW : UserActionHelper.UserFollowingAction.USER_UNFOLLOW, mobileNo);
        preferences.edit().putBoolean(mobileNo, following).apply();
    }
}
