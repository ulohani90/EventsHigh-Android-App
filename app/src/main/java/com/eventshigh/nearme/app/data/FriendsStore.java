package com.eventshigh.nearme.app.data;

import android.content.Context;
import android.content.SharedPreferences;

public class FriendsStore {
    private final SharedPreferences preferences;

    public FriendsStore(Context context) {
        preferences = context.getSharedPreferences("friends", Context.MODE_PRIVATE);
    }

    public boolean isFollowing(String contactId) {
        // By default we assume all friends are being followed, if the information is missing from prefs
        return preferences.getBoolean(contactId, true);
    }

    public void setFollowing(String contactId, boolean following) {
        
        preferences.edit().putBoolean(contactId, following).apply();
    }
}
