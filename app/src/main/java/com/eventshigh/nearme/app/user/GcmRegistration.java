package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.data.EventCategory;
import com.eventshigh.nearme.app.utils.ZendeskUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.maps.model.LatLng;
import com.zendesk.sdk.model.network.PushRegistrationResponse;
import com.zendesk.sdk.network.impl.ZendeskConfig;
import com.zendesk.service.ErrorResponse;
import com.zendesk.service.ZendeskCallback;

import java.io.IOException;
import java.util.UUID;
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

    private static final String PREF_REGISTRATION_ID = "registration_id";
    private static final String PREF_REGISTRATION_ID_UPLOADED = "registration_id_uploaded";
    private static final String PREF_ZENDESK_UPDATED = "zendesk_updated2";
    private static final String PREF_IID_UPLOADED = "iid_updated";
    private static final String PREF_DEVICE_INFO_UPLOADED = "device_info";
    private static final String PREF_FIRST_TOPICS = "first_topics";

    private static final String PREF_LAST_CITY = "last_city";
    private static final String PREF_LAST_CITY_UPLOADED = "last_city_uploaded";

    private static final String SENDER_ID = "708156551009";

    // Member variables used to store the user account details in preferences.
    private final Context context;
    private final SharedPreferences gcmRegistrationInfo;
    private long lastSyncTimestamp = 0;

    // City listener.
    private UserCityListener userCityListener = null;
    private LatLng userLocation = null;

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

    public synchronized void resetGcmRegistrationId() {
        Editor editor = gcmRegistrationInfo.edit();
        editor.remove(PREF_REGISTRATION_ID);
        editor.remove(PREF_ZENDESK_UPDATED);
        editor.remove(PREF_REGISTRATION_ID_UPLOADED);
        editor.remove(PREF_FIRST_TOPICS);
        editor.apply();

        lastSyncTimestamp = 0;
        updateGcmRegistrationIdIfNeeded();
    }

    public void setLastCity(@Nullable City city, @Nullable LatLng location) {
        userLocation = location;

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

            if (currentLastCity != null) {
                subscribeOrUnSubscribe(currentLastCity.toString(), false);
            }
            subscribeToTopic(city.toString());

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

    public static void sendUpstream(Context context, Bundle data) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        data.putLong("time_to_live", TimeUnit.DAYS.toSeconds(1));
        try {
            gcm.send(SENDER_ID + "@gcm.googleapis.com", UUID.randomUUID().toString(), data);
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
        }
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

            boolean isPlayServicesPresent =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;

            InstanceID instanceID = null;
            String registrationId = null;
            if (isPlayServicesPresent) {
                instanceID = InstanceID.getInstance(context);
                registrationId = gcmRegistrationInfo.getString(PREF_REGISTRATION_ID, null);
                if (registrationId == null) {
                    try {
                        registrationId = instanceID.getToken(SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
                    } catch (IOException e) {
                        Crashlytics.getInstance().core.logException(e);
                        registrationId = null;
                    }

                    if (registrationId != null) {
                        Editor editor = gcmRegistrationInfo.edit();
                        editor.putString(PREF_REGISTRATION_ID, registrationId);
                        editor.remove(PREF_REGISTRATION_ID_UPLOADED);
                        editor.remove(PREF_ZENDESK_UPDATED);
                        editor.apply();
                    }
                }
            }

            City city = getLastCity();
            if (city == null) {
                return;
            }

            // Upload last city.
            if (!gcmRegistrationInfo.getBoolean(PREF_LAST_CITY_UPLOADED, false)) {
                AccountStateReporter.reportLastCity(context, city, userLocation, new Runnable() {
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

            if (instanceID == null) {
                return;
            }

            // Upload IID.
            if (!gcmRegistrationInfo.getBoolean(PREF_IID_UPLOADED, false)) {
                String iid = instanceID.getId();
                AccountStateReporter.reportInstanceId(context, iid, new Runnable() {
                    @Override
                    public void run() {
                        gcmRegistrationInfo.edit().putBoolean(PREF_IID_UPLOADED, true).apply();
                    }
                });
            }

            // Fetch the GCM registration id.
            if (registrationId == null) {
                return;
            }

            // Upload GCM registration id.
            if (!gcmRegistrationInfo.getBoolean(PREF_REGISTRATION_ID_UPLOADED, false)) {
                AccountStateReporter.reportGcmRegistrationId(context, registrationId, new Runnable() {
                    @Override
                    public void run() {
                        gcmRegistrationInfo.edit().putBoolean(PREF_REGISTRATION_ID_UPLOADED, true).apply();
                    }
                });
            }

            // Report the GCM registration id with zendesk.
            if (!gcmRegistrationInfo.getBoolean(PREF_ZENDESK_UPDATED, false)) {
                ZendeskUtils.initZendesk(context);
                try {
                    ZendeskConfig.INSTANCE.enablePush(registrationId, zendeskCallback);
                } catch (Exception e) {
                    // Wait for initialization to finish and retry later.
                }
            }

            // Subscribe to topics.
            if (!gcmRegistrationInfo.getBoolean(PREF_FIRST_TOPICS, false)) {
                try {
                    GcmPubSub gcmPubSub = GcmPubSub.getInstance(context);

                    // Subscribe to city.
                    subscribeOrUnSubscribe(gcmPubSub, registrationId, city.toString(), true);

                    // Subscribe to interests.
                    Account account = new Account(context);
                    for (String interest : account.getFollowingInterests()) {
                        subscribeOrUnSubscribe(gcmPubSub, registrationId, interest, true);
                    }

                    gcmRegistrationInfo.edit().putBoolean(PREF_FIRST_TOPICS, true).apply();
                } catch (Exception e) {
                    Crashlytics.getInstance().core.logException(e);
                }
            }
        }
    }

    public void subscribeToTopic(final String interest) {
        subscribeOrUnSubscribe(interest, true);
    }

    public void unSubscribeToTopic(String interest) {
        subscribeOrUnSubscribe(interest, false);
    }

    private void subscribeOrUnSubscribe(final String interest, final boolean subscribe) {
        if (!gcmRegistrationInfo.getBoolean(PREF_FIRST_TOPICS, false)) {
            return;
        }
        final String registrationId = gcmRegistrationInfo.getString(PREF_REGISTRATION_ID, null);
        if (registrationId == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    subscribeOrUnSubscribe(GcmPubSub.getInstance(context), registrationId, interest,
                            subscribe);
                } catch (IOException e) {
                    Crashlytics.getInstance().core.logException(e);
                }
            }
        }).start();
    }

    private void subscribeOrUnSubscribe(GcmPubSub gcmPubSub, String registrationId, String interest,
            boolean subscribe) throws IOException {
        String topicName = "/topics/" + EventCategory.toCategoryParsableString(interest);
        try {
            if (subscribe) {
                gcmPubSub.subscribe(registrationId, topicName, null);
            } else {
                gcmPubSub.unsubscribe(registrationId, topicName);
            }
        } catch (IllegalArgumentException e) {
            Crashlytics.getInstance().core.logException(e);
        }
    }

    private ZendeskCallback<PushRegistrationResponse> zendeskCallback =
            new ZendeskCallback<PushRegistrationResponse>() {
                @Override
                public void onSuccess(PushRegistrationResponse pushRegistrationResponse) {
                    gcmRegistrationInfo.edit().putBoolean(PREF_ZENDESK_UPDATED, true).apply();
                }

                @Override
                public void onError(ErrorResponse errorResponse) {
                    // do nothing. upload will be retried.
                }
            };
}
