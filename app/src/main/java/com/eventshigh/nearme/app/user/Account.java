package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.broadcast.UpdateAccountInfoService;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.data.Locality;
import com.eventshigh.nearme.app.user.UserActionHelper.FollowingAction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Manages the user account on this device. The account information is stored using
 * SharedPreferences in {@code PREFS_FILE_NAME}.
 */
public class Account {
    public interface UserCityListener {
        void onUserCityChanged(City newUserCity);
    }

    public static class UserInfo {
        @Nullable public final String name;
        @Nullable public final String phoneNo;
        public final Boolean isVerified;

        public UserInfo(@Nullable String name, @Nullable String phoneNo, Boolean isVerified) {
            this.name = name;
            this.phoneNo = phoneNo;
            this.isVerified = isVerified;
        }
    }

    // Constants used for SharedPreferences.
    private static final String PREFS_FILE_NAME = "eh_user_credentials";

    // Mobile no of the user.
    private static final String PREF_NAME = "name";
    private static final String PREF_MOBILE_NO = "mobile_no";
    private static final String PREF_MOBILE_NO_VERIFIED = "mobile_no_verified";

    // Last city selection by user.
    private static final String PREF_LAST_CITY = "last_city";

    private static final String PREF_SAVED_LOCALITY="saved_locality";

    // The referrer for this user. this user installed the app via this referrer.
    private static final String PREF_REFERRER = "referrer";

    // The referrer link for this user.
    private static final String PREF_REFERRER_LINK = "app_share_link";

    // The prefix to the shared prefs key used to save follow tags for this user
    private static final String PREF_FOLLOW_KEY_PREFIX = "follow_";

    private static boolean disableSnackBar = false;

    // shared and static accountInfo which usages shared preference to store records.
    private static SharedPreferences accountInfo;
    private static synchronized void setAccountInfo(Context context) {
        if (accountInfo == null) {
            accountInfo = context.getSharedPreferences(PREFS_FILE_NAME, 0);
        }
    }

    // Member variables used to store the user account details in preferences.
    private final Context context;
    private UserCityListener userCityListener = null;

    public Account(Context context) {
        this.context = context.getApplicationContext();

        setAccountInfo(this.context);
        accountInfo.registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(PREF_LAST_CITY) && userCityListener != null) {
                    City lastCity = City.getCity(accountInfo.getString(PREF_LAST_CITY, ""));
                    if (lastCity != null) {
                        userCityListener.onUserCityChanged(lastCity);
                    }
                }
            }
        });
    }

    public UserInfo getUserInfo() {
        return new UserInfo(
                accountInfo.getString(PREF_NAME, null),
                accountInfo.getString(PREF_MOBILE_NO, null),
                accountInfo.getBoolean(PREF_MOBILE_NO_VERIFIED, false));
    }

    public void recordPhoneNumber(String name, String phoneNumber) {
        SharedPreferences.Editor editor = accountInfo.edit();
        editor.putString(PREF_NAME, name);
        editor.putString(PREF_MOBILE_NO, phoneNumber);
        editor.remove(PREF_MOBILE_NO_VERIFIED);
        editor.apply();

        if (name != null) {
            Crashlytics.setUserName(name);
        }
        if (phoneNumber != null) {
            Crashlytics.setUserIdentifier(phoneNumber);
        }
    }

    public void recordVerifiedPhoneNumber() {
        SharedPreferences.Editor editor = accountInfo.edit();
        editor.putBoolean(PREF_MOBILE_NO_VERIFIED, true);
        editor.apply();
    }

    public void removeUserInfo() {
        SharedPreferences.Editor editor = accountInfo.edit();
        editor.remove(PREF_NAME);
        editor.remove(PREF_MOBILE_NO);
        editor.remove(PREF_MOBILE_NO_VERIFIED);
        editor.apply();
    }

    public static void disablePhoneVerifySnackbar() {
        disableSnackBar = true;
    }

    public static boolean isPhoneVerifyPending(Context context) {
        if (disableSnackBar) {
            return false;
        }
        Account account = new Account(context);
        UserInfo userInfo = account.getUserInfo();
        return userInfo.phoneNo != null && !userInfo.isVerified;
    }

    public boolean recordReferrer(String referrer) {
        if (!accountInfo.contains(PREF_REFERRER)) {
            accountInfo.edit().putString(PREF_REFERRER, referrer).apply();
            UpdateAccountInfoService.run(context, true);
            return true;
        }

        return false;
    }

    public @Nullable String getReferrer() {
        return accountInfo.getString(PREF_REFERRER, null);
    }


    public void recordReferrerLink(String referrerLink) {
        accountInfo.edit().putString(PREF_REFERRER_LINK, referrerLink).apply();
    }

    public @Nullable String getReferrerLink() {
        return accountInfo.getString(PREF_REFERRER_LINK, null);
    }

    public boolean isFollowing(String tag) {
        return accountInfo.getString(getKeyForTag(tag), null) != null;
    }

    public void setIsFollowing(String tag, boolean isFollowing) {
        if (isFollowing) {
            accountInfo.edit().putString(getKeyForTag(tag), tag).apply();
            new UserActionHelper(context).recordAction(FollowingAction.FOLLOW, tag);
        } else {
            accountInfo.edit().remove(getKeyForTag(tag)).apply();
            new UserActionHelper(context).recordAction(FollowingAction.UN_FOLLOW, tag);
        }
    }

    public List<String> getFollowingInterests() {
        List<String> interests = new ArrayList<>();
        for (Entry<String, ?> entry : accountInfo.getAll().entrySet()) {
            if (entry.getKey().startsWith("follow_")) {
                interests.add(entry.getValue().toString());
            }
        }
        return interests;
    }

    public void setLastCity(@Nullable City city) {
        if (city == null) {
            return;
        }

        City currentLastCity = getLastCity();
        if (currentLastCity == null || !city.equals(currentLastCity)) {
            accountInfo.edit().putString(PREF_LAST_CITY, city.toString()).apply();
            accountInfo.edit().putString(PREF_SAVED_LOCALITY, "").apply();
            UpdateAccountInfoService.refreshCity(context);
        }
    }

    public void setSavedLocalities(List<Locality> localities) {
        if (localities == null) {
            return;
        }

        StringBuilder localityName=new StringBuilder();
        for(int i=0;i<localities.size();i++){
            localityName.append(localities.get(i).name);
            if(i!=localities.size()-1){
                localityName.append(",");
            }
        }
            accountInfo.edit().putString(PREF_SAVED_LOCALITY, localityName.toString()).apply();

    }

    public List<Locality> getSavedLocalities(){
        List<Locality> locality = new ArrayList<Locality>();
        if(accountInfo.getString(PREF_SAVED_LOCALITY, "").length()>0) {
            String[] localityName = accountInfo.getString(PREF_SAVED_LOCALITY, "").split(",");
            for (int i = 0; i < localityName.length; i++) {
                locality.add(Locality.getLocality(localityName[i]));
            }
        }
        return locality;
    }

    public @Nullable City getLastCity() {
        return City.getCity(accountInfo.getString(PREF_LAST_CITY, ""));
    }

    public void setUserCityListener (@Nullable UserCityListener userCityListener) {
        this.userCityListener = userCityListener;
    }

    private static String getKeyForTag(String tag) {
        return PREF_FOLLOW_KEY_PREFIX + EventCategory.toCategoryParsableString(tag);
    }
}
