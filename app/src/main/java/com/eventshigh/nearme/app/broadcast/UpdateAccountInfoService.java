package com.eventshigh.nearme.app.broadcast;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.util.Log;

import com.android.volley.Request.Method;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.crashlytics.android.Crashlytics;
import com.eventshigh.nearme.app.data.City;
import com.eventshigh.nearme.app.network.VolleyHelper;
import com.eventshigh.nearme.app.user.Account;
import com.eventshigh.nearme.app.user.GcmRegistration;
import com.eventshigh.nearme.app.utils.DeviceUtils;
import com.eventshigh.nearme.app.utils.EventsHighEndpoints;
import com.eventshigh.nearme.app.utils.Signer;
import com.eventshigh.nearme.app.utils.Utils;
import com.eventshigh.nearme.app.utils.ZendeskUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.zendesk.sdk.model.push.PushRegistrationResponse;
import com.zendesk.sdk.network.impl.ZendeskConfig;
import com.zendesk.service.ErrorResponse;
import com.zendesk.service.ZendeskCallback;

import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.util.concurrent.TimeUnit;

public class UpdateAccountInfoService extends IntentService {

    private static final String PARAM_REFRESH_GCM_TOKEN = "UpdateAccountInfoService.RefreshGCMToken";
    private static final String PARAM_REFRESH_LAST_CITY = "UpdateAccountInfoService.RefreshLastCity";

    private static final String UPLOAD_STATUS_FILENAME = "eh_upload_status";
    private static final String PREF_REFERRER_UPLOADED = "referrer_uploaded";
    private static final String PREF_GCM_TOKEN_UPLOADED = "gcm_token_uploaded2";
    private static final String PREF_ZENDESK_UPDATED = "zendesk_updated";
    private static final String PREF_IID_UPLOADED = "iid_uploaded2";
    private static final String PREF_DEVICE_INFO_UPLOADED = "device_info_uploaded";
    private static final String PREF_LAST_CITY_UPLOADED = "last_city_uploaded";

    private static final long INTERVAL_SYNC = TimeUnit.HOURS.toMillis(1);
    private static long last_sync_ts = 0;

    public static void run(Context context, boolean skipTimeCheck) {
        run(context, skipTimeCheck, new Intent(context, UpdateAccountInfoService.class));
    }

    public static void run(Context context, boolean skipTimeCheck,boolean receivedReferrer2) {
        Intent intent = new Intent(context, UpdateAccountInfoService.class);
        intent.putExtra("received_referrer2",receivedReferrer2);
        run(context, skipTimeCheck, intent);
    }

    public static void refreshGCMToken(Context context) {
        Intent intent = new Intent(context, UpdateAccountInfoService.class);
        intent.putExtra(PARAM_REFRESH_GCM_TOKEN, true);
        run(context, true, intent);
    }

    public static void refreshCity(Context context) {
        Intent intent = new Intent(context, UpdateAccountInfoService.class);
        intent.putExtra(PARAM_REFRESH_LAST_CITY, true);
        run(context, true, intent);
    }

    public UpdateAccountInfoService() {
        super(UpdateAccountInfoService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {


        final SharedPreferences uploadStatus = getSharedPreferences(UPLOAD_STATUS_FILENAME, 0);

        if ( intent!= null && intent.getBooleanExtra(PARAM_REFRESH_GCM_TOKEN, false)) {
            Editor editor = uploadStatus.edit();
            editor.remove(PREF_IID_UPLOADED);
            editor.remove(PREF_GCM_TOKEN_UPLOADED);
            editor.remove(PREF_ZENDESK_UPDATED);
            editor.apply();
        }
        if ( intent != null && intent.getBooleanExtra(PARAM_REFRESH_LAST_CITY, false)) {
            Editor editor = uploadStatus.edit();
            editor.remove(PREF_LAST_CITY_UPLOADED);
            editor.apply();
        }

        // Record referrer.

        boolean isReceivedReferrer2=false;
        if(intent!=null && intent.hasExtra("received_referrer2")){
            isReceivedReferrer2 = intent.getBooleanExtra("received_referrer2",false);
        }
        Account account = new Account(this);
        String referrer = account.getReferrer();
        if (referrer != null && (!uploadStatus.getBoolean(PREF_REFERRER_UPLOADED, false)|| isReceivedReferrer2)) {
           reportReferrer(referrer, uploadStatus,isReceivedReferrer2);
        }

        // Upload last city.
        City city = account.getLastCity();
        if (city == null) {
            return;
        }
        if (!uploadStatus.getBoolean(PREF_LAST_CITY_UPLOADED, false)) {
            reportLastCity(city, uploadStatus);
        }

        // Upload device info.
        if (!uploadStatus.getBoolean(PREF_DEVICE_INFO_UPLOADED, false)) {
            reportDeviceInfo(uploadStatus);
        }

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        if (apiAvailability.isGooglePlayServicesAvailable(this) != ConnectionResult.SUCCESS) {
            return;
        }

        // Referral Link.
        if (account.getReferrerId() == null) {
            account.recordReferrerId(getReferrerId());
        }

        // Upload IID.
        InstanceID instanceID = InstanceID.getInstance(this);
        if (instanceID == null) {
            return;
        }
        if (!uploadStatus.getBoolean(PREF_IID_UPLOADED, false)) {
            reportInstanceId(instanceID.getId(), uploadStatus);
        }

        if (uploadStatus.getBoolean(PREF_GCM_TOKEN_UPLOADED, false) &&
            uploadStatus.getBoolean(PREF_ZENDESK_UPDATED, false)) {
            return;
        }

        String registrationId;
        try {
            registrationId = instanceID.getToken(GcmRegistration.SENDER_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
        } catch (IOException e) {
            Crashlytics.getInstance().core.logException(e);
            return;
        }
        if (registrationId == null) {
            return;
        }

        // Upload GCM registration id.
        reportGcmRegistrationId(registrationId, uploadStatus);

        // Report the GCM registration id with zendesk.
        ZendeskUtils.initZendesk(this);
        try {
            ZendeskConfig.INSTANCE.enablePushWithIdentifier(registrationId, new ZendeskCallback<PushRegistrationResponse>() {
                @Override
                public void onSuccess(PushRegistrationResponse pushRegistrationResponse) {
                    uploadStatus.edit().putBoolean(PREF_ZENDESK_UPDATED, true).apply();
                    synchronized (UpdateAccountInfoService.this) {
                        UpdateAccountInfoService.this.notifyAll();
                    }
                }

                @Override
                public void onError(ErrorResponse errorResponse) {
                    // do nothing. upload will be retried.
                    synchronized (UpdateAccountInfoService.this) {
                        UpdateAccountInfoService.this.notifyAll();
                    }
                }
            });
            UpdateAccountInfoService.this.wait();
        } catch (Exception e) {
            // Wait for initialization to finish and retry later.
        }
    }

    private static synchronized void run(Context context, boolean skipTimeCheck, Intent intent) {
        if (skipTimeCheck || last_sync_ts + INTERVAL_SYNC < System.currentTimeMillis()) {
            context.startService(intent);
            last_sync_ts = System.currentTimeMillis();
        }
    }

    private void reportReferrer(String referrer, SharedPreferences uploadStatus,boolean isReceivedReferrer2) {
        Uri uri = getBaseUri(this, "reportReferrer")
                .appendQueryParameter("referrer", referrer)
                .appendQueryParameter("received_referrer2",isReceivedReferrer2?"true":"false")
                .build();
        report(uri, uploadStatus, PREF_REFERRER_UPLOADED);
    }

    private void reportGcmRegistrationId(String gcmRegistationId, SharedPreferences uploadStatus) {
        Uri uri = getBaseUri(this, "reportGcmRegistationId")
                .appendQueryParameter("gcm_registration_id", gcmRegistationId)
                .build();
        report(uri, uploadStatus, PREF_GCM_TOKEN_UPLOADED);
    }

    private void reportInstanceId(String iid, SharedPreferences uploadStatus) {
        Uri uri = getBaseUri(this, "reportInstanceId")
                .appendQueryParameter("instance_id", iid)
                .build();
        report(uri, uploadStatus, PREF_IID_UPLOADED);
    }

    private void reportLastCity(City city, SharedPreferences uploadStatus) {
        Uri uri = getBaseUri(this, "reportLastCity")
                .appendQueryParameter("last_city", city.toString())
                .appendQueryParameter("lat", "0")
                .appendQueryParameter("lon", "0")
                .build();
        report(uri, uploadStatus, PREF_LAST_CITY_UPLOADED);
    }

    private void reportDeviceInfo(SharedPreferences uploadStatus) {
        Uri uri = getBaseUri(this, "reportDeviceInfo2")
                .appendQueryParameter("device_name", DeviceUtils.getDeviceName())
                .appendQueryParameter("is_rooted", Boolean.toString(DeviceUtils.isRooted()))
                .build();
        report(uri, uploadStatus, PREF_DEVICE_INFO_UPLOADED);
    }

    private String getReferrerLink() {
        try {
            Uri uri = getBaseUri(this, "getReferrerId").build();
            String resp = sendSignedRequest(uri).get();
            JSONObject res = new JSONObject(resp);
            return res.getString("link");
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
            Log.w(UpdateAccountInfoService.class.getName(), "request failed: getReferrerLink", e);
        }

        return null;
    }
    private String getReferrerId() {
        try {
            Uri uri = getBaseUri(this, "getReferrerObject").build();
            String resp = sendSignedRequest(uri).get();
            JSONObject res = new JSONObject(resp);
            return res.getString("referrer_id");
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
            Log.w(UpdateAccountInfoService.class.getName(), "request failed: getReferrerId", e);
        }

        return null;
    }



    private void report(Uri uri, SharedPreferences uploadStatus, String key) {
        try {
            sendSignedRequest(uri).get();
            uploadStatus.edit().putBoolean(key, true).apply();
        } catch (Exception e) {
            Crashlytics.getInstance().core.logException(e);
            Log.w(UpdateAccountInfoService.class.getName(), "request failed: " + uri.toString(), e);
        }
    }

    private RequestFuture<String> sendSignedRequest(Uri uri)
            throws GeneralSecurityException, UnsupportedEncodingException {
        RequestFuture<String> future = RequestFuture.newFuture();
        VolleyHelper.addToRequestQueue(this,
            new StringRequest(Method.GET, Signer.sign(uri).toString(), future, future));
        return future;
    }

    public static Builder getBaseUri(Context context, String path) {
        return getBaseUriWithoutAndroidId(path)
                .appendQueryParameter("android_id", Utils.getAndroidId(context));
    }

    public static Builder getBaseUriWithoutAndroidId(String path) {
        return Uri.parse(EventsHighEndpoints.API_URI_BASE)
                .buildUpon()
                .appendPath("mobileapp")
                .appendPath(path);
    }
}
