package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.eventshigh.nearme.app.data.City;

import java.util.concurrent.TimeUnit;

/**
 * This class stores the GCM registration data locally within app.
 */
public class GcmRegistration {
    public interface UserCityListener {
        void onUserCityChanged(City newUserCity);
    }

    // Constants used for SharedPreferences.
    private static final String PREFS_FILE_NAME = "eh_gcm_credentials";

    private static final String PREF_DEVICE_INFO_UPLOADED = "device_info2";
    private static final String PREF_LAST_CITY = "last_city";
    private static final String PREF_LAST_CITY_UPLOADED = "last_city_uploaded";

    // Member variables used to store the user account details in preferences.
    private final Context context;
    private final SharedPreferences gcmRegistrationInfo;
    private long lastSyncTimestamp = 0;

    // City listener.
    private UserCityListener userCityListener = null;

    private GcmRegistration(Context context) {
        this.context = context.getApplicationContext();
        gcmRegistrationInfo = this.context.getSharedPreferences(PREFS_FILE_NAME, 0);
    }

    private static GcmRegistration instance;
    public static synchronized GcmRegistration getInstance(Context context) {
        if (instance == null) {
            instance = new GcmRegistration(context);
        }
        return  instance;
    }

    public void updateGcmRegistrationIdIfNeeded() {
        if (lastSyncTimestamp < System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)) {
            new Thread(new GcmRegistar()).start();
        }
    }

    public void setLastCity(@Nullable City city) {
        if (city == null) {
            return;
        }

        City currentLastCity = null;
        String cityName = gcmRegistrationInfo.getString(PREF_LAST_CITY, null);
        if (cityName != null) {
            try {
                currentLastCity = City.valueOf(cityName);
            } catch (IllegalArgumentException e) {
                // Ignore.
            }
        }

        if (currentLastCity == null || !city.equals(currentLastCity)) {
            gcmRegistrationInfo.edit()
                    .putString(PREF_LAST_CITY, city.toString())
                    .remove(PREF_LAST_CITY_UPLOADED)
                    .apply();

            userCityListener.onUserCityChanged(city);
            lastSyncTimestamp = 0;
        }

        updateGcmRegistrationIdIfNeeded();
    }

    public @Nullable City getLastCity() {
        City city = null;
        String cityName = gcmRegistrationInfo.getString(PREF_LAST_CITY, null);
        if (cityName != null) {
            try {
                city = City.valueOf(cityName);
            } catch (IllegalArgumentException e) {
                // Ignore.
            }
        }
        return city;
    }

    public void setUserCityListener (@Nullable UserCityListener userCityListener) {
        this.userCityListener = userCityListener;
    }

    private class GcmRegistar implements Runnable {
        @Override
        public void run() {
            synchronized (GcmRegistration.this) {
                if (lastSyncTimestamp > System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)) {
                    return;
                }

                lastSyncTimestamp = System.currentTimeMillis();
            }

            City city = getLastCity();
            if (city == null) {
                return;
            }

            // Upload last city.
            if (!gcmRegistrationInfo.getBoolean(PREF_LAST_CITY_UPLOADED, false)) {
                AccountStateReporter.reportLastCity(context, city, new Runnable() {
                    @Override
                    public void run() {
                        gcmRegistrationInfo.edit().putBoolean(PREF_LAST_CITY_UPLOADED, true).apply();
                    }
                });
            }

            // Upload device info.
            if (!gcmRegistrationInfo.getBoolean(PREF_DEVICE_INFO_UPLOADED, false)) {
                AccountStateReporter.reportDeviceInfo(context, new Runnable() {
                    @Override
                    public void run() {
                        gcmRegistrationInfo.edit().putBoolean(PREF_DEVICE_INFO_UPLOADED, true).apply();
                    }
                });
            }
        }
    }
}
