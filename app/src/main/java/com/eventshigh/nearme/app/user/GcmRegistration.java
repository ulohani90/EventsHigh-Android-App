package com.eventshigh.nearme.app.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.utils.ZendeskUtils;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.android.gms.maps.model.LatLng;
import com.zendesk.sdk.model.network.ErrorResponse;
import com.zendesk.sdk.model.network.PushRegistrationResponse;
import com.zendesk.sdk.network.impl.ZendeskCallback;
import com.zendesk.sdk.network.impl.ZendeskConfig;

import java.io.IOException;

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

    private static final String PREF_LAST_CITY = "last_city";
    private static final String PREF_LAST_CITY_UPLOADED = "last_city_uploaded";

    private static final String SENDER_ID = "708156551009";

    // Member variables used to store the user account details in preferences.
    private final Context context;
    private final SharedPreferences gcmRegistrationInfo;

    // City listener.
    UserCityListener userCityListener = null;

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
        new GcmRegistar().execute();
    }

    public void resetGcmRegistrationId() {
        Editor editor = gcmRegistrationInfo.edit();
        editor.remove(PREF_REGISTRATION_ID);
        editor.remove(PREF_REGISTRATION_ID_UPLOADED);
        editor.apply();

        updateGcmRegistrationIdIfNeeded();
    }

    public void setLastCity(@Nullable City city, @Nullable LatLng location) {
        if (city != null) {
            new CityRegistar(city, location).execute();
            if (userCityListener != null) {
                userCityListener.onUserCityChanged(city);
            }
        }
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

    public static void sendUpstream(Context context, String messageId, Bundle data) {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        try {
            gcm.send(SENDER_ID + "@gcm.googleapis.com", messageId, data);
        } catch (IOException e) {
            Crashlytics.logException(e);
        }
    }

    private class CityRegistar extends AsyncTask<Void, Void, Void> {
        private final City city;
        @Nullable private final LatLng location;

        private CityRegistar(City city, @Nullable LatLng location) {
            this.city = city;
            this.location = location;
        }

        @Override
        protected Void doInBackground(Void... params) {
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
            }

            uploadLastCity(location);
            return null;
        }
    }

    private class GcmRegistar extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            String registrationId = gcmRegistrationInfo.getString(PREF_REGISTRATION_ID, null);
            if (registrationId == null) {
                try {
                    InstanceID instanceID = InstanceID.getInstance(context);
                    registrationId = instanceID.getToken(SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

                    Editor editor = gcmRegistrationInfo.edit();
                    editor.putString(PREF_REGISTRATION_ID, registrationId);
                    editor.remove(PREF_REGISTRATION_ID_UPLOADED);
                    editor.apply();
                } catch (IOException e) {
                    // Ignore. try it next time.
                    Crashlytics.logException(e);
                    registrationId = null;

                    Editor editor = gcmRegistrationInfo.edit();
                    editor.remove(PREF_REGISTRATION_ID);
                    editor.remove(PREF_ZENDESK_UPDATED);
                    editor.remove(PREF_REGISTRATION_ID_UPLOADED);
                    editor.apply();
                }
            }

            if (!gcmRegistrationInfo.getBoolean(PREF_REGISTRATION_ID_UPLOADED, false)) {
                AccountStateReporter.reportGcmRegistrationId(context, registrationId, new Runnable() {
                    @Override
                    public void run() {
                        gcmRegistrationInfo.edit().putBoolean(PREF_REGISTRATION_ID_UPLOADED, true).apply();
                    }
                });
            }

            // Upload last city.
            uploadLastCity(null);

            // Report the GCM registration id with zendesk.
            if (registrationId != null &&
                    !gcmRegistrationInfo.getBoolean(PREF_ZENDESK_UPDATED, false)) {
                ZendeskUtils.initZendesk(context);
                try {
                    ZendeskConfig.INSTANCE.enablePush(registrationId, zendeskCallback);
                } catch (Exception e) {
                    // Wait for initialization to finish and retry later.
                }
            }

            // Upload IID.
            if (!gcmRegistrationInfo.getBoolean(PREF_IID_UPLOADED, false)) {
                String iid = InstanceID.getInstance(context).getId();
                AccountStateReporter.reportInstanceId(context, iid, new Runnable() {
                    @Override
                    public void run() {
                        gcmRegistrationInfo.edit().putBoolean(PREF_IID_UPLOADED, true).apply();
                    }
                });
            }

            return null;
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

    private void uploadLastCity(LatLng location) {
        City city = getLastCity();
        if (city != null &&
                !gcmRegistrationInfo.getBoolean(PREF_LAST_CITY_UPLOADED, false)) {
            AccountStateReporter.reportLastCity(context, city, location, new Runnable() {
                @Override
                public void run() {
                    gcmRegistrationInfo.edit().putBoolean(PREF_LAST_CITY_UPLOADED, true).apply();
                }
            });
        }
    }
}
